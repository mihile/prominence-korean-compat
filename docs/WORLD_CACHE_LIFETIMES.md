# 차원 이동 후 이전 월드 정리

build2026102602는 클라이언트에서 차원 이동·리스폰·접속 종료 뒤에도 남던 이전 월드 참조를 정리하고, 셰이더 캐시의 해시 계산에 쓰는 임시 버퍼를 줄입니다. 별도 설정 변경 없이 통합 JAR 하나로 적용합니다.

## 보존하는 동작

- 현재 월드의 작업대/인벤토리/아이템 미리보기/키 입력/낙하 이벤트를 보존합니다. 서버 월드·서버 플레이어와 다른 스레드의 이벤트는 정리 대상이 아닙니다.
- 인벤토리 내용이나 장비 NBT를 수정하지 않습니다. 오래된 클라이언트 인벤토리를 붙잡는 캐시 연결만 제거합니다. 서버 판정이나 저장 형식을 바꾸지 않습니다.
- Voxy 월드/모델 캐시 용량, 셰이더 출력·컴파일 프로그램, EMI 레시피와 개인 설정을 유지합니다. 강제 GC나 주기적인 전체 캐시 비우기를 넣지 않습니다.

<details>
<summary>정리 범위와 가드</summary>

`WorldCacheCleanupClient`가 기존 약한 월드/플레이어 감지기를 사용합니다. 실제 문맥이 바뀐 뒤 END_CLIENT_TICK에서만 정리하며, 매 프레임/매 틱 전체 맵을 순회하지 않습니다. 클래스/Field/Method 메타데이터만 보관하고 플레이어·월드를 강하게 저장하지 않습니다.

- **Iceberg1.1.25:** 이전 ClientWorld의 `EntityCollector.wrappedLevelsMap`와 미리보기 엔티티 캐시 항목을 제거합니다. CustomItemRenderer의 armorStand/horse/entity/blockEntity가 이전 월드 소유일 때 해당 객체와 대응 item key를 함께 초기화해 다음 미리보기를 재생성합니다. 현재/서버 문맥·모델 bounds·렌더 자원·일반 판정 캐시는 보존합니다.
- **Fabric Transfer3.3.6+8dd72ea377 / Zenith2.0.2:** 강한 인벤토리 키와 약한 값이 작업대의 container 역참조를 통해 이전 월드를 유지하는 순환을 끊습니다. 이전 클라이언트 월드의 확인된 Zenith wrapper와 직접 식별 가능한 Minecraft 플레이어/블록 엔티티 인벤토리 키만 제거합니다. 알 수 없는 wrapper·현재·서버 항목은 유지합니다.
- **Modern Industrialization1.8.6:** 멀티플레이 클라이언트에서 HOLDING_UP의 이전 ClientPlayerEntity 키만 제거합니다. 현재 로컬 플레이어와 키 패킷을 유지합니다. 원본 HashMap을 서버 스레드와 공유하는 통합 서버 실행 중에는 이 한 가지 정리를 생략해 새 교차 스레드 쓰기를 추가하지 않습니다. 다른 정리는 계속 적용됩니다.
- **Balm7.3.41:** 렌더/클라이언트 스레드에서 완료된 이전 월드의 LivingFallEvent만 ThreadLocal.remove로 해제합니다. 낙하 판정/취소/피해 override 메서드를 다시 쓰지 않으며 현재 이벤트와 다른 스레드 값을 보존합니다. 기존 실기동 힙에서도 병합된 `class_1309.currentFallEvent` 필드를 확인했습니다.

Minecraft1.20.1과5개 모드의10개 native 클래스 지문을 확인합니다. 모듈마다 독립적으로 활성화하며 버전/지문이 다르면 해당 기능을 끕니다. 예상과 다른 런타임 필드 형태가 나오면 그 연동만 중지하고 원래 기능을 유지합니다. opt-out은 `prominence.worldCacheCleanup=false` 또는 `.iceberg/.inventories/.mi/.balm=false` 시스템 속성입니다. 사용자 실행 옵션에 자동 추가하지 않습니다.

Xaero biome context의 추가 이전 월드 참조는 아직 올바른 갱신 시점을 확정하지 않아 이 수정에 포함하지 않았습니다. 모든 이전 월드 참조와 모든 RAM 문제를 해결했다고 주장하지 않습니다.

</details>

## 셰이더 해시 계산

문자열/define마다 만들던8KiB 버퍼를 해시 계산 한 번의 로컬 scratch buffer로 바꿨습니다. 전역/ThreadLocal 버퍼나 문자열 보관 캐시는 추가하지 않습니다. namespace·UTF-16 코드 유닛과 길이 prefix·define 순서·중복·손상 surrogate의 처리 및 기존 디스크 key/파일 형식은 같습니다. 기존 캐시 파일도 그대로 사용합니다.

## 검증 범위

- WorldCacheTests73검사: 실제 Guava weakValues의 키→작업대→wrapper 순환을 재현하고 해제 후 이전 world fixture가 회수됨을 확인했습니다. 현재/서버·도중 교체된 값·캐시 쌍·이벤트·문맥 감지·원본 field와 각 지문 정상/누락/변조를 검사합니다. Minecraft 월드는 만들지 않습니다.
- ShaderHashScratchTests43개 해시/UTF-16/경계/동시 호출 검사. 이전 공개 production class와 비교하는 선택적 fixture까지 포함하면54검사입니다. 500defines로5번 계산할 때 thread allocation은41,527,864→45,496bytes였습니다. 이 수치는 특정 CPU-only fixture이며 전체 게임 RAM 절감량이 아닙니다.
- 기존 전체 회귀 검사 및 native Iris/Euphoria18메서드 실제 변환, 설치 셰이더 pack include/native JCPP151검사를 통과했습니다. 실제 GPU 화면이나 게임의 RAM/GC 개선 검수와 구분합니다.
- `tools/verify_world_cache_fixture.py --build-dir BUILD --classpath CLASSPATH_JSON --java-home JDK_HOME`는 production 클라이언트 callback과 실제 반사 어댑터를 데이터 전용 stub 문맥에서 실행합니다.16검사로 정상/리스폰/접속 종료, 현재/서버 인벤토리·미리보기, 통합 서버 MI 정리 생략과 다른 스레드의 이벤트 보존을 확인합니다. 실제 Minecraft 클래스의 field/원본 지문 검사는 별도73검사와 이전 실힙 필드 증거를 사용합니다.

기동 로그: `Client world cache lifetimes: iceberg=enabled, inventories=enabled, mi=enabled, balm=enabled`. 실제 정리 때 모듈별 해제 항목 수를 기록합니다. 로그의 항목 수를 MB 절감량으로 해석하지 않습니다.
