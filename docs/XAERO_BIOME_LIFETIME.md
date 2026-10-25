# 지도 색상 계산기의 월드 참조

클라이언트 build2026102606에서 Xaero Minimap26.6.0의 색상 계산기가 작업 후 이전 월드를 보유하는 경로를 보완합니다. 이전 실제 MAT에서 현재 Xaero 세션 → MinimapWriterFabric → BiomeBlendCalculator.original → 이전 ClientWorld가 확인됐습니다.

원래 `writeChunk`는 전체 갱신의 시작에서만 calculator.prepare(world, blending)를 호출합니다. 뒤의 점진 작업도 같은 calculator를 사용하므로 단순히 prepare 직후나 임의 클라이언트 틱에 참조를 지우면 안 됩니다. 수정은 각 writeChunk의 월드를 작업 범위에서만 설정하고 finally에서 이전 호출 문맥을 복원합니다. 정상 초기값은 null이며, 중첩 호출에서는 바깥 작업을 보존합니다. prepare가 다시 불리지 않는 점진 작업도 해당 작업의 월드를 계속 읽습니다. 원래 블렌딩 범위와 계산·지도 캐시·청크·데이터는 유지합니다.

Minecraft1.20.1 / Xaero Minimap26.6.0 / MixinExtras0.5.4 및 native writer/calculator 두 클래스 SHA-256이 일치할 때만 적용합니다. `-Dprominence.xaeroBiomeScope=false`로 이 모듈만 끌 수 있습니다. 원래 writer와 calculator는 동시 호출을 지원하지 않으며 새 스레드·ThreadLocal·전체 월드 순회는 추가하지 않습니다. 실제 사용이 끝나기 전에 다른 스레드에서 필드를 지우지 않습니다.

`tools/verify_xaero_biome_native.py`는 원래 두 대상을 Mixin으로 변환하여63메서드와58개 native 호출열/prepare 보존을 확인합니다. production scope와 실제 변환 calculator를 데이터 전용 월드 대역으로 실행하는20검사는 현재 문맥·원래 인수/반환·점진 작업·블렌딩 범위·예외·중첩·기존 활성 문맥·이전 소유자 회수를 대조합니다. 별도9검사는 버전·지문·누락·변조·opt-out을 확인합니다. 실제 Minecraft 월드나 GL은 시작하지 않습니다.

이 작업은 확인된 참조 수명만 보완합니다. 기존 MAT의 여러 월드 retained 크기를 합쳐 보장 절감량으로 쓰지 않습니다. 새 힙 덤프나 실제 게임 전후 비교는 없으며, 메모리 감소량과 실화면 검수는 아직입니다. Voxy/셰이더/지도 캐시 용량과 Java heap/그래픽 설정을 변경하지 않습니다. 서버 수정은 필요하지 않습니다.
