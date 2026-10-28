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
