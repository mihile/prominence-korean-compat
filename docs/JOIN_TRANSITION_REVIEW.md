# 접속·차원 이동 검토 (2026-10-28)

대상 기록: 20261028-202922-8c8e94, 실행 client build2026102803, 순정 ComplementaryUnbound r5.9.3. JFR·PresentMon·로그·사후 MAT를 대조했다. 원시 자료는 공개하지 않는다.

## 측정 결과

- 첫 Iris pipeline 준비 10,048ms. 전처리 누적 921ms와 전체 pipeline 시간은 다르다. JFR에서 GLSL 변환 및 GL link가 보인다. GPU 컴파일 시간을 전처리 시간으로 단정할 수 없다.
- 네더 준비 2,287ms + Voxy 초기화 498ms. 엔드 준비 1,415ms + Voxy 초기화 438ms. Present 시작 간격은 해당 이동에서 약 4,497ms / 3,549ms였다. 반환 오버월드 Voxy 초기화 69ms.
- 첫 접속 Present 시작 간격 18,895ms는 표시 경로 불일치 플래그가 있으며 이를 곧바로 단일 프레임 렌더 시간이라고 해석하지 않는다. 이후 실제 접속 구간에 2,107ms / 1,622ms 시작 간격도 있다.
- EMI 전체 19,901ms, 그중 recipe bake 2,906ms. 초기 플러그인·모델·인덱스 작업이 겹친다. 하나의 CPU 샘플 비중으로 시간을 환산하지 않았다.
- 기록 중 GC 정지 합계 1,866ms, 최대 489ms. Full GC 1회. GC 후 피크 6,212,337,936 / max 6,341,787,648 bytes. 사후 MAT는 별도 시점이며 raw heap histogram과 혼합하지 않았다.
- MAT: ShaderSourceDiskCache 클래스 정적 표의 retained heap 151,507,704 bytes. 이 클래스의 전역 DYNAMIC_CACHE가 전체 원문을 붙잡는다. WorldEngine 약 539MB와 ModelFactory 약 359MB도 있으나 큰 정상 LOD 캐시라는 사실만으로 제거하지 않았다.
- 순정 Unbound 엔드 Voxy fragment shader에서 `texture(colortex13, texCoord).r`의 texCoord가 미선언이다. 기존 호환은 `.a`만 다뤄 놓쳤다.

## 판단과 변경

ProgramSet 백그라운드 예열은 Iris의 비동기용 계약이 없는 공유 상태를 변경하며 GPU pipeline을 컴파일하지 않는다. 외부 synchronized(pack)는 원래 getProgramSet 호출과 공유하는 락도 아니다. 제거했다. 첫 접속 이전부터 존재한 순정 shader compiler 비용을 Euphoria만의 문제로 볼 수 없다. 이전 릴리스의 “3.8초 원천 제거” 문구는 실제 게임 검증 없이 작성된 기대값이다.

메모리 표와 쓰기 큐를 제한하고 전역 원문 표를 제거했다. 캐시 출력은 기존과 byte-identical임을 native fixture로 확인했다. MAX_PRIORITY 상승과 지형 제출 제한도 제거했다. 후자는 지형 준비를 의도적으로 늦추므로 현재 사용자가 보고한 체감과 맞지 않는다. Voxy 완료 모델의 FIFO를 유지하면서 프레임별 작업을 분산한다. 네이티브 호출 하나 자체의 지연을 4ms 아래로 보장하지 않는다.

32MiB 결과 캐시·16MiB 대기 쓰기 한도, FIFO/진행·예외 복원·스레드 경계, 실제 Voxy/Iris bytecode Mixin, 순정 Unbound includes, 누적 클래스 보존을 오프라인 검증했다. 첫 GPU shader compile·pipeline 구성의 지연은 남는 병목이며 무리한 공유 GL 컨텍스트 컴파일이나 임의 그래픽 품질 변경은 이번 구현에 포함하지 않았다. 최신 패치의 실사용 전후 시간 개선은 재측정 전 미확정이다.


## 후속 측정: 20261028-212411-d5f66c / build2026102804

이번 로그는 `ComplementaryUnbound_r5.9.3 + EuphoriaPatches_1.10.5`가 활성화된 조건이다. 이전 순정 Unbound 기록과 동일 조건의 A/B가 아니다.

- 접속 직후 연속 Present 시작 간격 1,804 / 2,331 / 2,827ms가 관찰된다. 단순 평균 FPS로 설명할 수 없다. 측정은 파이프라인 사전 설정 2,863ms가 끝난 뒤 시작되어 이 초기 작업을 JFR에서 분석했다고 말하지 않는다.
- 네더 첫 생성 4,635ms / Voxy 초기화 372ms, 엔드 생성 3,875ms / Voxy 초기화 353ms. 해당 시작 간격 최대 5,683 / 4,948ms. 오버월드 복귀도 Iris 1,600ms / Voxy 36ms로, Voxy 초기화 전체가 5초라고 볼 수 없다.
- GC 정지 합계 2,499ms, 최대 499ms, Full GC 1회. GC 후 피크 6,144,125,464 bytes. GC만으로 이동 시 단일 5초를 설명하지 못한다.
- 지난 전역 원문 캐시 151MB retained 항목은 새 MAT 상위에서 없어졌다. EMI는 여전히 21,654ms이고 총 시간 감소는 확인되지 않았다.
- 렌더 샘플에는 Fabric registry remap/StateIdTracker.recalcStateMap, Voxy Mapper.loadFromStorage 중 ModernFix 블록 상태 캐시와 배낭 shape 생성, 클래스 로딩, Flywheel 대기가 함께 있다. 기존 JFR ClassLoad 이벤트는 0건이므로 각 클래스 로딩의 정확한 비용을 정량 확정할 수 없다. 샘플 순위만으로 “전부 Voxy 때문” 또는 “전부 shader 때문”으로 단정하지 않는다.
- 확정한 추가 부하: 캐시 쓰기마다 prune()이 전체 파일을 isRegularFile/readAttributes로 조회하고 정렬했다. 현재 캐시 4,327개/약 1.2GiB이며 JFR writer 스택의 상위 두 항목이 Windows 파일 속성 조회다(각 87 samples, 시간을 뜻하지 않음). 파이프라인 종료마다 대기 쓰기가 약 15~16MiB에 붙어 있고 본격적인 캐시 재사용은 충분히 확보되지 않았다.

### 구현

build2026102805는 디스크 색인을 한 번 읽은 뒤 쓰기/퇴출 시 증분 갱신한다. 128회 쓰기 후 외부 변경을 재확인한다. writer는 정확히 1개로 제한하여 색인 경쟁을 막았다. 상한·원자 쓰기·범위 검사·오래된 항목 우선 정책은 유지한다. 32개 쓰기 fixture에서 directory scan 1회를 검증했다. 실제 3분 기록에서 시간 개선은 아직 미측정이다.

### 첫 접속의 다음 개선 지점

Voxy Mapper 초기화가 현재 렌더 경로에서 NBT/상태/opacity 및 형상 초기화를 유발한다. 순수 디코딩과 상태 적용·GPU 준비를 분리하는 방안이 우선 후보다. 전체 getOrCreate를 무작정 worker로 옮기면 registry remap·ModernFix 상태 초기화·월드 수명과 충돌할 수 있다. 불투명도 캐시는 모드/설정 변경 시 무효화해야 하므로 state ID만 키로 영구 저장하면 안 된다. Fabric remap도 서버 raw ID 의미를 바꾸므로 단순 생략하지 않는다. 현재 자료로 어느 호출이 몇 ms인지 전부 확정했다고 하지 않는다.

### 전환의 시각적 문제

Fastload `instant_load=false`, server_render_chunk_radius=3이다. 현재 호환 패치는 로딩 화면을 닫거나 페이드를 삭제하는 mixin을 포함하지 않는다. 받은 화면 조건이 만족되면 전환하고 이후 Voxy/Flywheel 모델 및 새 shader 비용이 몰리면 사용자가 화면을 먼저 보고 정지하는 체감이 생길 수 있다. 이 설명은 코드·실측에 맞는 후보이며 화면 녹화 없이 페이드 발생 시각을 확정하지 않는다.

개선 설계는 이전 화면 보관 → 필요한 지형/새 shader가 준비된 첫 일관된 프레임 → 짧은 150~250ms 페이드 해제다. 이는 전환 품질 개선이고 실제 계산시간 단축은 별개다. 렌더 스레드가 shader compile에 막힌 동안 같은 스레드에서 부드러운 애니메이션을 보장할 수 없다. 임의의 5초 대기나 준비 조건 없는 HUD 표시가 해결책이 아니다. 이번 .5는 캐시 결함만 수정하며 시각 전환 로직을 임의로 추가하지 않았다.


## 후속 측정: 20261028-224843-71e1d4 / build2026102806

실행 빌드 2806, Fastload 로그 참조 0회, 현재 설정은 ModernFix dynamic_resources 명시 활성화가 없고 기본 비활성이다. Euphoria 1.10.5가 포함된 Unbound다. 앞선 기록과 설정 및 이동 순서가 달라 통제 A/B로 쓰지 않는다.

확인된 동작:

- 누락 trim mask 상세 출력 81,784행은 없어졌다. 남은 atlas 누락 10행은 SimplySwords/Archon/Create 등 별도 경로다. 최신 로그 총 5,594행으로, 모델 마스크 오류를 단순 필터로 감춘 것이 아니다.
- 캐시 writer가 네더·엔드 각각 157개를 쓰고 pendingBytes=0, queueDropped=0으로 마무리했다. 지난 15~16MiB backlog가 사라졌다. 처음 메뉴 준비와 오버월드 복귀에서는 3개씩 queue drop이 있어 완전 무손실 캐시라고 표현하지 않는다. 네더·엔드의 shader text miss는 여전히 각 157개다.
- EMI 전체 시간은 17,430ms. 이전 21,654ms보다 짧게 관찰됐으나 설정·부하 차이를 통제하지 않았으므로 패치 효과로 확정하지 않는다. 주요 구간은 plugin registration 6,920ms, index construction 2,341ms, recipe baking 2,123ms, search baking 3,551ms.
- 네더 Iris 준비 4,578ms / Voxy 초기화 69ms. 엔드 Iris 5,384ms / Voxy 67ms. Voxy 초기화가 현재 이동의 수초 전체를 차지하지 않는다.

메모리와 정지:

- GC 후 peak 6,341,263,840 / max 6,341,787,648 bytes(거의 100%). 기록 중 실제 GCPhasePause 합계 5,441.9ms, 최대 881.1ms, G1Full 5회. 이전 기록은 pause 2,499.3ms / Full 1회. concurrent G1Old 전체 시간을 정지로 더하지 않았다.
- 사후 MAT BakedQuad(class_777) 객체가 이전 약 297,815개에서 10,957,013개로 증가했다. 객체 자체 shallow heap만 525,936,624 bytes이며 관련 int[] 전체를 이 클래스의 소유라고 합산하지 않았다. 일괄 베이킹으로 인한 모델 메모리 확대와 일치한다. 정확한 생성 원인별 소유는 histogram만으로 모두 확정하지 않는다.
- 첫 접속 7,733ms Present 시작 간격은 consistency 경고가 있어 단일 GPU/CPU 프레임 정지라고 확정하지 않는다. 그 구간의 GC만으로 설명할 수 없다. 이동 구간은 Iris 준비 및 GC가 함께 있다.
- Voxy 섹션 보유 약 539MB, 모델 팩토리 약 357MB도 크지만 현재 활성 캐시인지 판별하지 않고 통째 제거하지 않는다. ResourcefulLib HighlightHandler 약 99MB는 별도 후보다.

다음 조치의 우선순위:

1. Fastload는 비활성 조건을 유지하고 ModernFix dynamic_resources만 복원한 비교가 가장 가치 있다. 현재 heap 여유에서 전체 모델을 미리 굽는 전략이 Full GC를 늘리는지 검증한다. 이번 분석 작업에서는 설정을 변경하지 않았다.
2. FerriteCore bakedQuadDeduplication과 modelSides는 이미 활성화돼 있다. 정점 배열 인터닝과 다른 BakedQuad 객체 공유를 혼동하지 않는다. 전체 객체 공유는 동일 필드·모드 추가 필드·수명/가변성을 확인한 뒤 검토해야 하며, histogram 숫자만 보고 무작정 구현하지 않는다.
3. 캐시 저장은 개선됐으나 Euphoria가 포함된 pipeline 준비 비용은 남았다. 새 GPU pipeline과 변환/소스 전처리/GC를 구분한다. 서로 다른 차원 결과를 같은 것으로 재사용하면 안 된다.
4. 평균 FPS나 로그 감소로 첫 접속이 해결됐다고 주장하지 않는다. 이 회차는 분석 문서만 갱신했고 불필요한 새 JAR 빌드를 만들지 않았다.


## 후속 측정: 20261028-232355-2b086a / build2026102806

이번 실행은 로그에서 dynamic_resources=true를 확인했다. Fastload는 실행 로그에 없고 셰이더는 Unbound r5.9.3 + Euphoria 1.10.5다. JAR은 이전 기록과 같은 build2806이다. 이동 순서와 플레이 내용까지 통제한 실험은 아니다.

- GC 후 peak 5,926,298,008 / max 6,341,787,648 bytes, 약 93.4%. Full GC 0회(직전5회), 실제 정지 합계 1,783ms(직전5,442ms), 최장95ms(직전881ms). 동적 리소스 복원 후 메모리/GC 지표가 뚜렷하게 좋아졌다.
- MAT BakedQuad 290,821개 / shallow13,959,408 bytes. 직전10,957,013개 / 525,936,624 bytes에서 감소했다. ItemStack 약192만개와 ItemEmiStack 약255만개는 큰 변화가 없다.
- 첫 접속의 Present 시작 간격 1,780/2,092/3,315ms는 여전히 남았다. GC가 개선됐으므로 최초 접속 지연 전체를 메모리나 ModernFix 모델 락 하나로 설명할 수 없다.
- EMI19,224ms(직전17,430ms)로 초기화 시간 개선은 이번에 확인되지 않았다. 캐시 쓰기 backlog는 네더/엔드 모두0이고 mask 누락대량출력은 재발하지 않았다.
- 엔드 Iris4,153ms / Voxy graphics init73ms, 네더 Iris3,444ms / Voxy graphics init64ms. 오버월드 복귀 Iris1,480ms / Voxy54ms, 이후 Voxy37ms. Voxy init counter는 WorldEngine/Mapper 저장소 여는 시간까지 전부 포함한 값이 아니다.
- 첫 접속 구간 JFR에는 StateIdTracker.recalcStateMap, 초기 클래스 로딩, Flywheel WaitGroup 대기, Voxy Mapper StateEntry 생성 중 ModernFix의 블록 상태 cache/shape 초기화가 계속 관찰된다. 샘플 수를 ms로 환산하지 않았다.

개선 후보 우선순위:

1. 현재 Fastload-off/dynamic_resources-on 구성을 기준으로 유지한다. 이번 회차에서는 설정을 더 바꾸지 않는다.
2. Voxy Mapper에서 오래된 mapping 전부의 opacity를 생성자에서 즉시 계산하는 대신 실제 getBlockStateOpacity 호출에서 계산하도록 분리하는 방안이 유력하다. 현재 소스에서 opacity 필드 읽기는 Mapper.getBlockStateOpacity(int) 하나다. 저장 ID/BlockState 해석은 그대로 하고, leaves15 규칙·native BlockGetter·첫 접근 동기화·다른 호출자의 직접 필드 접근 여부를 검증해야 한다. 단순 sentinel 추가나 전체 세계 초기화 비동기 이동은 검증 전 적용하지 않는다. 비용 절감량은 아직 미측정이다.
3. 차원별 Iris 프로그램 준비는 여전히 수초다. shader source preprocessing, GLSL 변환, GL link의 정확한 span을 나눠야 한다. 이미 서로 다른 프로그램을 같은 캐시 결과로 취급하거나 무조건 destroy를 막는 방식은 안전하지 않다.
4. ResourcefulLib HighlightHandler 약99MB는 별도의 추가 RAM 후보지만 현재 최초3초/이동4초를 설명하는 근거는 아니다. 정상 Voxy 섹션538MB와 모델359MB도 크기만 보고 제거하지 않는다.

이 회차는 분석·개선 후보 정리다. 새 코드/JAR이나 운영 설정은 변경하지 않았다. 원시 JFR/HPROF/로그는 로컬에 둔다.

## 구현: nightly-20261029.1 / 저장된 Voxy 매핑의 차광값 계산 분리

232355 기록의 첫 접속 렌더 스레드에서 `Mapper.loadFromStorage → StateEntry → getLightBlock → ModernFix 상태 캐시/배낭 형상`이 관찰됐다. 과거에 저장된 모든 상태의 차광값은 매핑 ID/NBT를 복원하는 데 필요하지 않다. `loadFromStorage` 범위에서만 계산을 미루고, Mipper가 실제로 해당 상태를 사용할 때 원래 함수를 한 번 호출한다. 새로 수집한 상태의 생성은 원래 동작이다.

원본 BlockGetter와 BlockPos를 보존한다. 나뭇잎15 분기, ID/BlockState 디코딩, 직렬화, 데이터 수정/복구 경로는 유지한다. 값의 근사나 디스크 형식 변경, 전역 상태 캐시, 임의 worker 생성은 없다. 계산 성공 후 원래 필드에 값을 반영하고 임시 객체/뷰를 해제하며, 실패는 호출자에게 전파하고 다음 접근에서 재시도한다. 동시 최초 접근은 상태별로 한 번만 계산한다. 사용되는 상태의 작업은 없어지는 것이 아니라 필요 시점으로 분산되는 것이므로 모든 첫 접속 정지가 제거됐다고 표현하지 않는다.

현재 Voxy port.9의 Mapper/StateEntry/익명 BlockGetter/Mipper 클래스 지문을 묶어 두 믹신을 함께 활성화한다. 설치 모드465개 및 내부 JAR의 필드 참조를 조사해 opacity 직접 소비자가 Mapper 하나임을 확인했다. 다른 Voxy/addon 조합을 지원할 때 `tools/audit_voxy_opacity.py`를 다시 실행한다. 직접 필드 접근을 추가하는 addon은 별도 검토가 필요하다. 옵트아웃은 `-Dprominence.emiHome.voxyDeferredOpacity=false`.

검증: 20,000개 매핑의 사용된100개만 계산하는 fixture, 동시 접근·실패 재시도·중첩/예외/thread-local 복구·클로저 해제, 실제 네이티브 Mixin 변환/ASM 검증. 변환된 StateEntry 생성자를 작은 블록 API 대역으로 실제 실행해 eager/lazy/나뭇잎/실패/최종값을 검증했다. 이는 게임 실행이나 프레임 개선 실측이 아니다. 로그에는 매핑 복원 시간과 미룬 계산 개수를 1회 기록한다.

현재 latest.log도 확인했다(567,686bytes, ERROR31/WARN1131, 마지막 종료23:27:26). 캡처232355와 같은 실행이며 새로운 실행 결과가 아니다. 장식 마스크 대량 오류는 재발하지 않았다. 기존 shader 태그 경고 등을 이번 패치에서 숨기지 않았다. Iris pipeline/registry remap/Flywheel 대기와 EMI19.2초는 별도 잔여 비용이다. ModernFix 설정, 셰이더, 서버, 운영 JAR은 직접 변경하지 않는다.

## 후속 측정: 20261029-005257-b9ea42 / build2026102901

설치 SHA256은 공개2901과 일치하고 `voxyDeferredOpacity=enabled`다. JFR/PresentMon/힙 덤프/MAT가 complete이며, 현재 latest.log는 저장 로그 뒤에 종료 기록이 추가된 같은 실행이다(현재569,255bytes, ERROR31/WARN1139). ModernFix dynamic_resources=true, Fastload 미로딩, Unbound/Euphoria 조건이다.

### 패치의 동작과 실제 체감 지표를 구분

- 첫 Voxy 매핑 읽기321.317ms, 기존 블록상태13,309개의 차광값 계산을 미뤘다. 네더48.552ms/2,402개, 엔드34.502ms/2,603개. 해당 계산의 JFR 샘플은 이제 VoxyServer Remote Ingest에서 관찰되며 렌더 스레드의 일괄 계산 경로에서는 관찰되지 않는다. 따라서 패치 활성화/실행 경로 변화는 확인됐지만 샘플 부재를 시간0으로 해석하지 않는다.
- 첫 접속의 긴 프레임은1,873/1,738/3,499/546/2,140ms다. 직전1,780/2,092/3,315/796ms와 비교해 전체 끊김 개선은 확인되지 않았다. 서로 다른 실행 부하/진행 조건이므로 이번 패치가 악화의 단독 원인이라고도 확정하지 않는다.
- 3,499ms 프레임의 PresentMon GPU busy는44.535ms, GPU wait는3,458.816ms다. GPU 연산 포화보다는 렌더 스레드의 준비/대기 병목에 맞는다. GPUTime 전체를 GPU가 계산한 시간으로 보지 않는다.
- EMI19,224→20,261ms, recipe bake2,408→2,625ms. 이번 변경은 EMI 개선 패치가 아니며 전체 초기화 개선도 없다.
- GC pause1,783→1,770ms, 최장95.17→92.50ms, Full0→0. GC후 peak5,930,483,952 /6,341,787,648bytes(93.5%). MAT BakedQuad290,737, ItemEmiStack2,545,766; WorldEngine538.7MB/ModelFactory361.6MB. 메모리 폭증이나 Full GC 재발은 없지만 여유가 넉넉한 것은 아니다.

### 남은 첫 접속 병목의 구체적 근거

1. FTB Quests Freeze Fix1.0.0의 `LevelReadyMixin.onClose`가 접속 화면 종료 시 `ClientQuestFile.openGui()` 후 즉시 `closeGui(true)`를 호출한다(실제 설치 Fabric1.20.1 bytecode 확인). JFR에는 이 호출에서 ChapterPanel→퀘스트 아이콘→DisplayStacksCache→creative tab 준비로 이어지는 렌더 스레드 대기가 있다. creative tab 모니터 대기2건 합계721.959ms, 최장685.721ms, previousOwner는 ForkJoinPool.commonPool-worker-7. 이는 수동 퀘스트북 조작을 가정한 추정이 아니라 강제 예열 경로가 기록된 것이다.
2. 모델 bake 모니터 대기11건 합계1,034.772ms(직전3건62.565ms). previousOwner는 청크 렌더 작업 스레드들801.010ms와 Voxy Model factory processor233.761ms다. 핫바/미니맵이 같은 모델 베이킹 락을 기다린다. EMI Thread-68이 이 락을 쥐었다고 단정할 근거는 없다.
3. 가장 긴 첫 접속 프레임 구간에는 Flywheel WaitGroup.await 샘플이 많다. sampling 수를 경과시간으로 환산하지 않는다. 접속 초반 Fabric StateIdTracker.recalcStateMap, 클래스 로딩, Iris block-material map 구축도 남는다.
4. EMI 플러그인 중 emi2,170ms, emi_enchanting1,903ms, farmersdelight598ms, allthetrims552ms 등이 관찰된다. EMI Enchanting 생성자42행의 registry 반복이 Thread-68 상위 샘플이다. 전체20초를 하나의 플러그인이나 GPU 작업으로 환산하지 않는다.

다음 우선순위는 FTB 강제 전체 GUI 예열을 접속 임계 경로에서 분리하고, 공유 아이템 목록이 완성된 후 필요한 아이콘 준비를 제한적으로 수행하는 것이다. 이전의 모든 렌더 스레드 creative 조회를 무조건 생략하는 fallback 패치는 변형 아이템 누락 위험 때문에 재활성화하지 않는다. 이어 청크/Voxy 모델 작업과 화면에 필요한 모델 사이의 경합, Flywheel worker 완료 대기를 조사한다. `dynamic_resources=false`는 이전 Full GC/모델 메모리 폭증 결과상 우선 해결책이 아니다.

### 차원 이동

네더 Iris4,301ms / Voxy graphics60ms, 엔드 Iris3,484ms / Voxy35ms. 오버월드 첫 복귀 Iris1,547ms / Voxy42ms, 이후 Voxy36ms다. 이동의 수초 대기는 여전히 Iris pipeline 준비가 큰 부분이다. 최대 Present 시작 간격5,122ms는 캡처 자체의 consistency 경고가 있으므로 정확한 단일 프레임 시간으로 쓰지 않는다. 차원 준비 중 네더/엔드 cache pending0, 오버월드 복귀 pending약10MB/drop6으로 이전 전반적인 writer 정체는 재발하지 않았다.

이번 회차는 분석만 수행했다. 추가 모드/설정/JAR 변경 없이 이 근거를 기록하며 원시 측정 자료는 로컬에 유지한다.


## 2026-10-29 — Freeze Fix 비활성화 이후 검토, nightly-20261029.2

- 현재 `ftbquestsfreezefix-fabric-1.0.0-1.20.1.jar.disabled`를 확인했다. 재활성화하지 않는다. 최신 로그는02:03~02:22 실행이며 기존 캡처005257 이후 새 JFR/PresentMon/MAT 묶음은 없다. 따라서 비활성화 전후의 프레임 개선률은 계산하지 않는다.
- 현재 EMI18,496ms, enchanting1,984.6ms/조건조회5,106,348회. Voxy mapping188.7ms/차광계산13,312개 지연. EMI가 모든 모델 락을 잡았다는 근거는 없으며 기존005257 기록의 모델 대기1,034.8ms(청크 작업801.0/Voxy233.8)와 Flywheel 작업 완료 대기를 별도로 본다. Flywheel의 무한 대기 생략·작업 미완료 반환은 넣지 않았다.
- 렌더 스레드에서 실제 샘플이 잡힌 Fabric `StateIdTracker.onRemap -> recalcStateMap`은 상태 순서가 동일해도 ID 목록 전체를 지운 뒤 해시맵에 다시 넣는다. 설치된 Fabric Registry Sync2.4.1+4c0a70e677와1.20.1 IdList bytecode 및 [Fabric 1.20.1 원문](https://github.com/FabricMC/fabric/blob/1.20.1/fabric-registry-sync-v0/src/main/java/net/fabricmc/fabric/impl/registry/sync/trackers/StateIdTracker.java)을 대조했다.
- 이번 수정은 remap 시 정렬된 전체 상태의 객체 동일성·순서·개수가 기존 ID 목록과 모두 같을 때만 재삽입을 생략한다. 하나라도 다르면 원래 전체 재구성을 실행한다. 신규 등록 경로·서버 동기화 패킷·ID 결정 방식은 유지하며 영구 캐시는 추가하지 않는다. `Registry state remap: unchanged=..., states=..., elapsedMs=...`로 적용 여부/비용을 남긴다. 서버 ID 순서가 실제로 달라지는 첫 접속에는 이 최적화가 적용되지 않을 수 있다. 수초 프리즈 전체 해결로 보지 않는다.
- 최신 로그의 반복 shader sampler/uniform 경고는02:04:31~32(접속02:04:42 전)에 집중된다. Discord context queue 경고80회는10초 간격이다. 둘을 수초 프레임 정지의 직접 원인이라고 단정하거나 로그만 숨기지 않았다.02:11~12 Voxy Model factory에서 EMF bed head/foot64회 생성 제한,02:16 stream buffer 삭제 오류도 확인했다. 현재 캡처에는 해당 실행 타임라인이 없으므로 별도 렌더 수명 문제 후보로 남긴다.
- 검증:10만 상태 동일/순서변경/객체교체/개수변경/동등하지만 다른 객체 fixture, 설치된 StateIdTracker의 실제 Mixin 변환/ASM 검사. 공개는 클라이언트 nightly만, 실제 끊김 감소는 미측정이다.
