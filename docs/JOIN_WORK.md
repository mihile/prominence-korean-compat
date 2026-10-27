# 접속 구간의 반복 작업 보완

client build2026102702. Prominence II 4.1.3 / Minecraft 1.20.1, 클라이언트 전용.

## 확인한 작업과 변경

- 집에 처음 접속한 기록에서 Reactive Music의 PlayerThread.run이 대기 중에도 반복 실행된다. 원본은 재생 요청이 없고 player가 null이면 continue로 즉시 루프를 반복하고, 완료된 player도 반복해서 호출할 수 있다. 수정은 다음 재생 요청까지 LockSupport로 잠들게 한다. play가 queued를 게시한 뒤 깨우며, volatile 조건을 다시 검사하므로 알림이 먼저 와도 요청을 놓치지 않는다. 종료 시 기존 interrupt/kill 경로를 따른다. MP3 디코더·재생 중 AudioDevice·곡 선택·음량 처리는 바꾸지 않는다.
- FancyMenu MCEFUtil.isMCEFLoaded가 매 틱 Class.forName으로 없는 선택 기능을 다시 찾는 스택이 렌더 스레드에 나타난다. MCEF 클래스가 시작 classpath에 없을 때만 false 결과를 재사용한다. MCEF가 설치돼 있으면 모듈 전체를 적용하지 않고, critical failure 플래그도 계속 읽는다. Fabric의 실행 중 고정 classpath를 전제로 한다.
- Iris BlockMaterialMapping.createBlockStateIdMap의 fastutil 맵 확장/rehash가 렌더 스레드에 나타난다. 처음에는 원본 생성자를 사용하고, 이후에는 직전 완료 크기로 새 맵을 미리 할당한다. 16–131072 항목으로 제한하고 정수 하나만 보관한다. 첫 맵 생성은 빨라지지 않으며, 실제 값·순서·태그/속성 판정·최초 매칭 우선순위는 원본 그대로 계산한다. 맵·월드·셰이더·GPU 자원을 보관하는 캐시가 아니다.

세 모듈은 독립적으로 설치 모드 버전·원본 클래스 SHA-256과 MC/MixinExtras를 확인한다. 지원 대상은 Reactive Music 1.3.5, FancyMenu 3.7.0, Iris 1.7.6+mc1.20.1이다. 대상 코드가 바뀌면 해당 모듈을 끈다. 진단 opt-out은 `-Dprominence.join.music=false`, `-Dprominence.join.menu=false`, `-Dprominence.join.material=false`이며 사용자 JVM 설정은 자동 변경하지 않는다.

## 검사 범위

`tools/verify_join_native.py`는 설치된 실제 세 클래스를 Mixin/MixinExtras로 변환하고 38개 메서드의 ASM 검증을 수행한다. 실제 음악 클래스의 원본/수정 대기 루프를 각각 250ms 실행해 스레드 CPU를 기록했다. 테스트는 곡을 요청하지 않아 오디오 장치나 Minecraft·월드·GL을 초기화하지 않는다. 이 조건에서 원본은 CPU250ms, 수정은 OS 측정 해상도상0ms였다. 실제 음악 재생의 장치 검수나 게임 프레임 개선 실측은 별도로 필요하다.

실제 메뉴 메서드203호출의 클래스 조회1회, critical failure 처리, 실제 Iris 빈 입력 메서드의 초기 용량/완료 크기 hook을 확인한다. helper130검사에서는 선행 알림·대기 후 알림·종료·ID/순서/기본값 보존을, guard15검사에서는 설치 지문과 변경/누락/버전 차단을 확인한다. 이전462클래스·업데이트 helper와 공개키는 바이트 동일하다.

## Iris 파이프라인 전체 재사용 검토

현재 Euphoria의 DimensionShaderRefresh는 차원별 ShaderPack으로 바꾼 뒤 PipelineManager.destroyPipeline을 호출한다. ShaderPack 캐시는 이미 존재하지만 GPU 파이프라인 캐시는 아니다. Iris 파이프라인은 render targets, shadow targets, custom textures, uniforms, block/entity/item material settings와 연동되고 Voxy 렌더러도 같은 수명에 묶여 있다. 단순히 destroy를 건너뛰거나 이전 파이프라인을 반환하면 차원별 상태와 GPU 자원 수명이 달라진다.

이번 변경은 전체 GPU 파이프라인 보관을 구현하지 않는다. 기존 전처리 디스크 캐시와 결과 캐시는 유지하고, 분류표 반복 확장만 줄인다. 전체 재생성 비용에는 남는 시간이 있다. 향후에는 동일 입력의 shader transform/프로그램 생성 비용을 더 세분화하고, 상태 복원·리소스 reload·서버 종료·셰이더 설정 변경·실제 메모리 상한까지 검증한 뒤 재사용 범위를 결정해야 한다.

## 효과 판단

저장된 접속 기록의 여러 작업이 동시에 발생했다. 음악의 불필요한 CPU 사용과 메뉴의 반복 탐색은 실제 제거 대상으로 확인했지만, 이것만으로 모든 접속 끊김의 원인이 설명되거나 체감 문제가 해결됐다고 단정하지 않는다. 업데이트 후 같은 집 첫 접속 조건으로 프레임/JFR/GPU를 한 번에 기록해 비교한다. 측정 raw 파일과 개인 경로는 릴리스에 포함하지 않는다.
