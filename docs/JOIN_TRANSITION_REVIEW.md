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
