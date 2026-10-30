# 웜홀·웨이스톤 도착 보호

Fabric Waystones3.3.3+mc1.20.1의 `WaystoneBlockEntity.doTeleport`는 도착지 로딩 완료를 기다리지 않고 서버의 이동 처리를 끝낸다. 따라서 클라이언트가 화면을 준비하는 동안에도 도착지의 적이 피해를 줄 수 있다. 포켓 웜홀의 teleport_to_waystone 패킷도 이 함수를 사용한다.

## 동작

- 같은 이동 함수의 실제 `FabricDimensions.teleport` 호출 직전에 서버 보호를 시작한다. 일반 웨이스톤·포켓 웜홀 등 이 공통 경로에 적용하며 다른 모드나 명령어 이동에는 자동 적용하지 않는다.
- 클라이언트가 요청받은 차원과 도착 청크를 확인하고, terrain 로딩 화면/overlay 없이 월드를 정상 렌더링한 프레임3개가 이어지면 다음 client tick에 완료를 보낸다. 프레임 간격이250ms를 넘거나 조건이 깨지면 다시 확인한다. 향후 모든 비동기 작업의 종료를 예측하는 신호가 아니라 도착 화면이 사용 가능한 시점을 확인하는 신호다.
- 서버는 해당 플레이어의 현재 이동 token이 일치할 때만 로딩 보호를60server ticks(정상20TPS에서3초) 유예로 전환한다. 중복·과거 완료 패킷은 시간을 연장하지 않는다.
- 일반 몹 공격·투사체·마법·화염·낙하 등 플레이어의 native damage 경로를 차단한다. 공허(out_of_world)와 관리자 강제 사망(generic_kill)은 보존한다. abilities.invulnerable, 효과·장비·저장 NBT를 변경하지 않는다.
- 실패한 이동은 새 보호를 취소하고 유효한 이전 이동 보호만 복원한다. 연결 종료·사망/리스폰·서버 종료·시간 만료 때 정리한다. 보관 키는 weak player 참조이며 전역 월드/클라이언트 객체를 보관하지 않는다.
- 완료 신호가 오지 않으면60초 대기 후3초 유예로 끝낸다. 신호를 지원하지 않는 구클라이언트는 도착 시점부터10초 고정 보호를 받는다. 정확한 ‘화면 준비 후3초’ 동작은 클라이언트와 서버 양쪽 새 JAR이 필요하다. 구서버에 새 클라이언트만 설치하면 실제 피해 보호는 제공되지 않는다.

## 범위와 검증

설치된 WaystoneBlockEntity와 Minecraft1.20.1 ServerPlayerEntity의 SHA256, Fabric Waystones 버전, MixinExtras0.5.4를 확인한 경우에만 Mixin을 적용한다. 관련 지문은 `prominence-arrival-pins.json`이다. `-Dprominence.arrivalProtection=false`는 해당 JVM의 삽입을 비활성화한다.

`tools/verify_arrival_native.py`는 CLIENT와 실제 dedicated SERVER classpath에서 각각 native2타깃211메서드를 변환하고 ASM 검증한다. 이동 전/성공/실패 삽입과 원래 피해 처리보다 먼저 취소할 수 있는 위치를 확인했다. 순수 정책 fixture3,027개와 guard6개는 로딩·60tick·timeout·중복token·구클라이언트·tickoverflow·프레임 안정성·패킷 경계를 검사한다. 이는 실제 전투/텔레포트 플레이 검수를 대신하지 않는다.

클라이언트 기존616클래스와 서버 기존182클래스를 byte-identical로 보존했다. 새 공통11클래스는 두 JAR에서 동일하며 서버에는 client/render 코드가 없다. 주기적인 전세계 스캔·청크 강제 로드·새 thread·프로파일링은 추가하지 않는다. 패킷은 실제 이동마다 작고 제한된 요청/완료 각1회다.

클라이언트는 nightly2026103005, 서버는 build2026103001로 각각 서명 공개한다. 실제 설치는 기존 updater 경로를 사용하며 실행 중 JAR 교체·강제 종료·재시작은 하지 않는다. 새 클라이언트/서버는 구형 상대와 접속을 유지한다. 서버용 소스는 별도 저장소의 `src/compat/arrival-protection`과 `tools/integrate_arrival.py`에 있다.
