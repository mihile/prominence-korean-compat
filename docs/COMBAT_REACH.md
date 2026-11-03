# 팔 연장기와 Better Combat 공격 거리

`nightly-20261103.9`는 Reach Entity Attributes의 공격 거리 속성을 Better Combat 무기 공격에 연결합니다. Things의 팔 연장기는 원래 블록 거리와 공격 거리에 각각+2를 주지만, Better Combat1.9.0은 무기 자체의 사거리와 등록된 확장 API만 사용하여 이 공격 속성을 놓쳤습니다.

클라이언트 `TargetFinder.findAttackTargetResult`의 거리 인자를 함수 진입 시 보정합니다. 후보 탐색, 타격 상자와 최종 거리 필터가 보정된 값을 받으며, 기존 AttackRangeExtensions/Pehkui 단계는 기존 순서로 이어집니다. Reach Entity Attributes2.4.0의 속성 기본값은0이므로 바닐라 사거리3을 다시 더하거나 빼지 않습니다. 무기 기본3과 속성+2는5가 됩니다. 장착 상태와 속성은 매번 읽어 탈착 즉시 원복되며, 장비·플레이어·월드 캐시는 만들지 않습니다.

음수 보너스로 최종 거리가0미만이면0으로 제한하고 비정상적인 비유한 결과는 원래 무기 거리로 돌아갑니다. Friends&Foes의 ‘도달 범위’는 블록 거리 속성만 주므로 이 수정이 공격 보너스를 새로 만들지 않습니다. 블록 거리, 무기 기본 데이터, 공격 각도·벽 판정, 스폰·밸런스 설정과 서버 파일은 변경하지 않습니다. 이 버전의 서버 공격 처리에 새 거리 속성을 등록할 필요는 없으며, 이미 동기화되는 속성을 클라이언트의 표적 획득에 사용합니다.

## 적용과 범위

게임의 **패치 업데이트 → 시험판(nightly)**에서 업데이트한 뒤 게임을 정상 종료하고 다시 실행합니다. 기존 설치본을 실행 중 직접 교체하지 않습니다. 이 모듈은 클라이언트 전용이며 서버 JAR에 넣지 않습니다. 시작 로그 `Better Combat attack reach attributes: enabled`가 활성화 여부를 나타냅니다.

Minecraft1.20.1, Better Combat1.9.0+1.20.1 및 Reach Entity Attributes2.4.0의 버전과 네이티브 클래스 SHA-256을 확인합니다. 대상/확장/Pehkui 연계 코드나 의존성이 달라지면 비활성화하며, 의존성 링크 오류도 기존 게임 실행을 막지 않도록 처리합니다. JVM 속성 `prominence.combat.reach=false`로 이 모듈만 해제할 수 있습니다. 업데이트가 사용자 JVM 옵션을 수정하지는 않습니다.

## 검증

실제 설치된 TargetFinder에 Sponge Mixin0.8.7을 적용해10개 메서드를 ASM 검증했습니다. 대상 탐색 함수 외 기존8개 메서드의 바이트코드는 동일하며, 새 속성 보정이 후보 탐색보다 먼저 실행되는지 확인했습니다.

원본/변환된 표적 획득 함수와 실제 Reach Entity Attributes의 getter 바이트코드를 작은 API fixture에서 실행했습니다. 각각141개 검사로 기본 거리, +2·음수·큰 보너스, 범위 밖 대상, 후보 탐색 확대, 탈착/즉시 원복, 속성 없음, 블록 전용 효과, 비유한 값과 기존 ADD/MULTIPLY 순서를 확인했습니다. 기존 확장 API는 실제 Better Combat 코드를 사용하고 월드·충돌 기하 객체는 작은 대역입니다. 실제 월드에서 거리별 공격을 검수한 결과로 해석하지 않습니다.

실제 가드도14개 검사로 정상 활성화, 버전/지문 불일치, 누락 의존성/클래스, 링크 오류와 해제 옵션을 확인했습니다. 누적665개 기존 클래스, 업데이트 도우미, 내장 번역/리소스는 그대로 보존했습니다. 추가된3개 클래스를 포함한 누적 검증과 Gson2.10 ABI 검사를 함께 수행합니다. 실제 플레이 검수는 업데이트 후 남아 있습니다.

기존 전체 클래스 ASM 검사에서 메뉴 훅 수를 세는 부분은 updater의 소유 진입점으로 한정했습니다. 다른 누적 모듈의 독립 화면 훅을 updater 중복 등록으로 오인하던 검사를 바로잡은 것으로, 모든 클래스의 ASM 검증은 계속 수행합니다.

## 소스와 빌드

실제 모드 코드/지문을 기준으로 다음 원본과 대조했습니다: Things1.20의 [`ArmExtenderItem`](https://github.com/glisco03/things/blob/ff48e2d11f1bde6fd5a073bc9b6010719eabf5ad/src/main/java/com/glisco/things/items/trinkets/ArmExtenderItem.java), Better Combat1.9.0+1.20.1의 [`TargetFinder`](https://github.com/ZsoltMolnarrr/BetterCombat/blob/8814f0297bb85f4513646e73d604069487ce7322/common/src/main/java/net/bettercombat/client/collision/TargetFinder.java). 원본 모드의 클래스를 배포 JAR에 복사하지 않습니다.

```text
python tools/integrate_combat_reach.py --base LATEST_PUBLIC_JAR --classpath CLASSPATH_JSON --java-home JDK_HOME --output STAGING --build NEW_BUILD --label NIGHTLY_TAG
python tools/verify_combat_reach.py --build-dir STAGING --classpath CLASSPATH_JSON --java-home JDK_HOME
python tools/verify.py --build-dir STAGING --classpath CLASSPATH_JSON --java-home JDK_HOME --mods CLIENT_MODS --fancymenu FANCYMENU_JAR
```

클래스패스에는 실제 client-intermediary, 로더/라이브러리와 중첩 의존성인 Reach Entity Attributes2.4.0이 필요합니다. 검증은 실제 실행 환경의 Sponge Mixin0.17.3+mixin.0.8.7, Minecraft용 Gson2.10 및 Mojang logging1.1.1을 사용했습니다. 개인 절대 경로와 클래스패스는 공개하지 않습니다. 최신 공개 build2026110308은 직전 nightly7과 모든 클래스가 같아 이를 누적 기준으로 사용했습니다.
