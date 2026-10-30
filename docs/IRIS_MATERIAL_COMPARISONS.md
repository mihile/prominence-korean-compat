# Iris 첫 렌더의 재질 조건 비교

## 20261030-003340-ef1f2b 분석

실행 빌드는 nightly2026103002다. 최신 로그는 저장 로그를 그대로 포함하고 종료 기록이 추가됐으며 저장 시 ERROR31/WARN940, 현재 ERROR33/WARN945다. 정리한 creative 대기/공용 스레드 우회와 외부 Xaero throttle은 재활성화되지 않았다. 확률2,322개 수신도 정상이다.

이번 셰이더는 `Bliss-Shader-Unstable.zip`(SHA25694fe5abd6f8963d2a3b1aff96af26a1c22c2dc184574493b0afcd4d9f78e6c20)다. 이전 Complementary/Euphoria 기록과 셰이더가 달라 패치별 개선률로 비교하지 않는다. 메모리 GC후 최고5,582,618,624 / 최대6,341,787,648B(88.0%), GC 총1,394ms / 최장83.59ms / trace중 Full0회였다. MAT WorldEngine538.7MB, ModelFactory350.6MB는 별도 보유 객체이며 이 크기만으로 누수라고 판단하지 않는다.

| 구간 | Iris preparePipeline | PresentMon FrameTime |
| --- | ---: | ---: |
| 첫 입장 | 438ms | 3,081ms; 관측 시작 간격8,591ms와 불일치 경고 |
| 네더 | 1,717ms | 2,521ms |
| 엔드 | 637ms | 1,221ms |
| 오버월드 복귀 | 418ms | 1,087ms |

첫 간격을 정확한8.59초짜리 단일 프레임으로 단정하지 않는다. 네더 긴 프레임의 GPUBusy46ms/Wait2,475ms는 GPU 연산 포화만으로 설명되지 않음을 보여준다. preparePipeline은 이후 첫 렌더의 비용을 모두 포함하지 않는다. JFR 시간대별 스택에서 `IrisRenderingPipeline.beginLevelRendering → BlockMaterialMapping.createBlockStateIdMap → addBlockStates → checkState`가 첫 입장과 이동 뒤 다시 실행된다. mapping 관련 CPU 표본41개, pipeline 생성54개가 기록됐다. 표본 수를 정확한 ms로 바꾸거나 두 경로를 합산하지 않는다. ModernFix 모델 대기, Flywheel 작업 완료, 초기 registry remap도 남아 있다.

EMI 전체19,404ms: index2,088 / plugin6,776 / recipe bake2,934 / search4,830ms. 이 시간은 단일 렌더 정지 시간이 아니다. 전리품 경로 CPU 표본은4개로 이번 대규모 끊김의 주원인이라는 근거는 없다.

## 코드와 셰이더 확인

설치 Iris1.7.6 Fabric1.20.1의 BlockMaterialMapping SHA256은9ce4dffd7f1e722aa03f1230f6cd4698fa898b0bd3c379821476bdd1c405b78e다. native checkState는 조건별로 `property.name(state.get(property))`를 계산하고 기대 문자열과 비교한다. addBlockStates/addTag는 동일한 조건으로 상태 목록 전체를 순회한다. 설치 Bliss의 block.properties는1,228,538B이고 속성 조건처럼 보이는 raw token14,279개가 있다. 이는 비활성 전처리 분기와 반복도 포함하므로 실행된 조건/상태 수가 아니다.

셰이더의 차원별 소스는 서로 다를 수 있고 첫 GL compile/link는 여전히 필요하다. native TransformPatcher에는 이미400-entry 결과 캐시가 있어 같은 기능의 큰 캐시를 덧붙이지 않았다. 기존에 철회한 JCPP disk 캐시나 pipeline 파괴 억제를 되살리지 않았다. 원문/설정은 수정하지 않았다. 구조 참고: [Iris 차원 소스 규칙](https://shaders.properties/current/reference/miscellaneous/dimension_properties/), [Bliss Unstable 소스](https://github.com/X0nk/Bliss-Shader/tree/Unstable), [Colorwheel](https://github.com/djefrey/Colorwheel). 설치 native bytecode를 최종 기준으로 사용했다.

## 수정 경계

- `checkState` 자체의 비교 루프는 그대로 둔다. 단순 Boolean/Enum 조건은 원래 상태 목록과 비교 경로를 사용한다. 상태마다 캐시를 조회하는 초기 시안은 간단한 조건에서 역효과가 있어 폐기했고 배포하지 않았다.
- 한 재질표 생성 중 전체 상태가 이미 배정된 동일한 immutable 목록은 이후 다시 순회하지 않는다. 원래 `putIfAbsent`가 모두 무효인 경우에만 생략한다. block/tag 순서와 먼저 배정된 ID, 삽입 순서를 보존한다.
- 지원되는 native 속성으로만 구성되고 정수 값의 종류가 4개 이상, 상태가 16개 이상인 조건은 가장 선택적인 정수 값으로 후보를 먼저 거른다. 남은 후보도 원래 `checkState`로 최종 검사한다. parse→name 왕복이 일치하지 않는 별칭이나 알 수 없는 형식은 원래 목록을 사용한다.
- 사용자 정의 속성·enum·map·상태, 준비 중 예외는 원래 경로로 돌아간다. 원래 비교에서 발생하는 예외를 숨기는 처리는 없다. 정확한 native StateManager가 같은 블록의 속성과 상태를 전달하는 호출 지점에 한정한다.
- 처리 목록은 `createBlockStateIdMap`의 thread-local scope 안에서만 보관한다. 중첩·예외 시 복원하고 종료 시 참조를 제거한다. 월드 간 재질표·셰이더·GPU 자원을 재사용하거나 매 프레임 작업하지 않는다.
- 설치 Iris mapping 및 Minecraft property/state/StateManager 8개 클래스의 지문에 한정한다. `-Dprominence.join.materialPredicates=false`로 별도 비활성화할 수 있다. `Iris material filtering` 로그는 재질표 생성마다 한 번만 전체 시간과 후보/생략/fallback 수를 기록한다.

## 검증과 남는 한계

`tools/verify_material_predicates.py`는 설치된 native `checkState`를 추출해 생산 helper 결과와 대조한다. Minecraft 경계는 명시적인 API doubles이며 게임을 시작하지 않는다. 1,068개 검사로 최종 재질 ID·삽입 순서·정수/혼합 조건·별칭·예외·fallback·중첩·스레드 격리를 확인했다. 정수 조건의 반복 입력에서 native 값 이름 변환은 192,000→13,000회였으며 게임 시간 개선 수치가 아니다. 단순 Boolean 입력은 목록 자체를 바꾸지 않고 추가 이름 변환도 하지 않는지 확인한다.

실제 native Mixin 변환에서는 변경된 3타깃 48메서드를 ASM 검증하고, 디버그 정보·stack frame을 제외한 `checkState` 명령이 그대로인지 검사한다. 누적 613클래스 중 기존 609개와 updater helper는 byte-identical이며 기존 JoinGuard와 새 3클래스만 달라졌다. 누적 기능 613클래스/59 Mixin 경계, join helper 130개·가드 20개 검사도 통과했다. 결과는 빌드의 검증 보고서에 남긴다.

기록의 EMF/OBE 침대 모델 반복 생성, Worlds Together registry 조회, connector 파싱, 리소스/초기화 오류는 별도로 남아 있다. 버전 확인/공개 서버 ping의 네트워크 오류도 구분했다. 로그를 숨기지 않았으며 이들이 모두 같은 정지의 원인이라고 단정하지 않는다. 수정 후 게임 프레임 감소량은 아직 미측정이다. 설치본·서버·설정·셰이더 파일은 직접 변경하지 않았다. 이번 배포는 client nightly 전용이다.
