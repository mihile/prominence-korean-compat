# 마법부여의 장비 간 중첩 안내

현재 설치된 모드의 방어구·착용 장비 효과74종을 조사해 한국어 설명 끝에 괄호 안내를 넣었다. 초기20종에54종을 추가한 nightly-20261104.6 누적 수정이다. 초기 범위에는 곡예사가 빠져 있었으며, 이번에는 마법부여 이름이 있는29개 모드 JAR의 목록과 등록 슬롯·호출부를 대조했다. 전체 키·분류·버전·지문은 [검토 목록](../review/enchantment_stacking.json)에 있다.

| 방식 | 표시 예 |
|---|---|
| 장비 최고 레벨만 조회 | `(중복 착용 시 최고 레벨만 적용)` |
| 장비 레벨을 더함 | `(장비별 레벨 합산)` |
| 피해 보호 수치를 더함 | `(방어구별 효과 합산)` |
| 장비마다 독립적으로 콜백 호출 | `(장비마다 개별 발동 판정)` |
| 특정 슬롯을 직접 조회 | `(착용한 신발에만 적용)` 등 |
| 개별 아이템의 규칙/내구도/수리 | `(해당 장비에만 적용)` 등 |

곡예사는 Combat Roll1.3.3의 EntityAttributes_CombatRoll.getAttributeValue에서 RECHARGE에 대해 vanilla getEquipmentLevel을 호출한다. 이 함수는 지정 슬롯의 최고 레벨을 선택한다. 동일III를 방어구4개에 부여해도 레벨III이며XII로 합산하지 않는다. 별도로 붙은 구르기 관련 속성 수정까지 중첩되지 않는다는 뜻은 아니다. 긴 발걸음은feet, 추가 구르기는head 등록 슬롯을 조회한다.

MCDA5.0.2의19종은 모두 분류했다. 광분·난폭·튕겨내기·아군 치유 등은 최고 레벨, 영혼 가방은 전용 helper의 레벨 합산, 연소·재활용·눈덩이는 장비별 피격 콜백이다. 단순히 같은 EnchantmentHelper를 참조한다는 이유만으로 모두 중첩 불가라고 처리하지 않았다. Zenith 반발 효과는 장비별 콜백에서 최고 레벨로 반발력을 계산하므로 `(부위별 발동, 최고 레벨 기준)`으로 구분한다. 실제 콜백과 적용 슬롯을 모두 확인했다.

전투 자극·저항 등 ExtraSpellAttributes6종의 기존 합산 안내는 유지한다. 광란은 주손 등록 슬롯을 읽는다는 안내를 유지하며, 처치 후 성급함 버프 자체가 쌓이는 설명과 구분한다. 난폭의 최대 생명력-60%, 광분의50% 이하 조건 설명 교정도 보존한다.

Spell Power 및 이를 호출하는 원소 숙련/부패 효과는 현재 서버의 spell_power/enchantments.json allow_stacking=true 설정을 확인하고 합산으로 안내한다. 이 부분은 설정 의존적이며 고정된 vanilla 규칙이 아니다. 해당 설정을 바꾸면 정적 언어 안내도 함께 갱신해야 한다. More RPG Classes의 실제 등록 항목은elemental이며, 언어 파일에만 남은air_mastery/earth_mastery/water_mastery를 현재 등록된 마법부여처럼 세지 않았다.

보호·마법 보호처럼 장비별 보호 수치를 합치는 효과는 `(방어구별 효과 합산)`으로 구분한다. 화염 보호와 폭발 보호는 피해 감소 수치는 합산하지만 연소 시간/밀려남 감소는 최고 레벨을 조회하므로, 두 효과를 괄호 안에서도 나눠 표시한다. native ProtectionEnchantment.method_8238/method_8237의 별도 최고 레벨 조회를 확인했다. 점감/보호 상한/확률 같은 원래 효과의 나머지 계산이 사라진다는 뜻은 아니다. 가시·일부 MCDA 효과의 개별 발동은 최종 피해나 확률이 장비 수에 정확히 비례한다는 뜻도 아니다. 수선은 공유하는 경험치를 사용하므로 장비 수만큼 경험치가 복제되는 것으로 안내하지 않는다.

흉갑에만 자연적으로 부여할 수 있는 효과라도, 피격 콜백이 각 아이템을 방문하는 경우에는 등록 슬롯만 보고 흉갑 전용이라고 단정하지 않았다. 반대로 고정 슬롯/장비 조회를 실제로 사용하는 효과만 슬롯 전용으로 안내한다. 무기·도구 전용 항목, 라이브러리 예제/언어 키, 현재 등록되지 않는 항목은 방어구 조사 범위에 억지로 포함하지 않았다. 무기 전용이지만 앞선 요청에 포함된 광란의 기존 안내는 유지한다.

## 범위

| 네임스페이스 | 안내 수 |
|---|---:|
| mcda | 19 |
| extraspellattributes | 6 |
| mcdw | 1 |
| combatroll | 3 |
| spell_power | 8 |
| more_rpg_classes | 1 |
| death_knights | 1 |
| ad_astra_giselle_addon | 4 |
| betterend | 1 |
| betterarcheology | 1 |
| estrogen | 1 |
| zenith | 5 |
| sortilege | 3 |
| the_bumblezone | 1 |
| yigd | 2 |
| create | 1 |
| minecraft | 16 |

이 범위는 설치본·현재 서버 설정을 확인한 방어구/착용 장비 안내이며, 모든 무기·도구 전용 마법부여까지 검수했다는 주장은 아니다. 등록 및 실제 소비 경로를 확인하지 않은 항목에 임의의 중첩 판정을 붙이지 않았다. 기존 이름과 효과 계산, 서버 설정은 유지한다.

## 검증과 배포

실제 Minecraft 언어 Mixin10개 메서드와 팩토리/조회/구형 Paxi 우선순위·영어 분리158개 검사를 통과했다. 최종 JAR에서 바꾸는 항목은 한국어 정정표와 내부 빌드 식별2개뿐이다.683개 기존 클래스, updater helper, 내장 한국어팩10,999개 파일과 기존 퀘스트 규칙은 바이트 단위로 보존한다. 원본 전투/이동/인챈트 로직은 재컴파일하지 않는다. 게임/서버/월드를 기동하지 않으며 실제 게임 화면의 줄바꿈 검수는 남아 있다.

```text
python tools/build_language_update.py --base LATEST_NIGHTLY_JAR --output STAGING --build NEW_BUILD --label nightly-YYYYMMDD.N
python tools/verify_translation_native.py --build-dir STAGING --classpath CLASSPATH_JSON --java-home JDK_HOME
```

클라이언트 시험판 서명 피드로 배포하며 기존 업데이트·종료 후 교체 경로를 따른다. 실행 중 JAR를 직접 교체하거나 stable main·서버 JAR·설정을 바꾸지 않는다.
