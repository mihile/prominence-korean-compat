# 마법부여의 장비 간 중첩 안내

20개 마법부여의 한국어 설명 끝에 실제 장비 집계 방식에 맞는 괄호 안내를 추가했다. 방어구 여러 부위의 레벨 합산과 상태 효과 자체의 중첩은 구분한다. 게임 계산/장비 적용 슬롯을 바꾸는 패치가 아니다.

| 대상 | 설명 뒤 안내 |
|---|---|
| 광분·난폭 등 같은 최고 레벨 조회 방식10종 | `(중복 착용 시 최고 레벨만 적용)` |
| 전투 자극·저항·회피·주문 억제·주문 파괴·수호 | `(장비별 레벨 합산)` |
| 신발 전용3종: fire_trail/lucky_explorer/swiftfooted | `(착용한 신발에만 적용)` |
| 광란 | `(주손 무기의 레벨만 적용)` |

이름 표기는 기존 번역을 유지한다. 전체 키와 분류는 [review/enchantment_stacking.json](../review/enchantment_stacking.json)에 기록했다. 합산은 장비에 부여된 레벨을 더한다는 뜻이며, 저항의 점감이나 주문 파괴의 상한처럼 원래 설명에 있는 최종 효과 계산까지 선형이 된다는 뜻은 아니다.

난폭의 기존 한국어 설명은 체력이60%까지 낮아졌을 때 발동하는 것으로 읽혔다. 실제 코드는 최대 생명력에-60% 수정을 적용하고 마법부여 레벨에 따른 힘 효과를 부여하므로 이를 바로잡았다. 광분은 실제 비교 연산에 맞게 생명력50% 이하로 표기했다. 광란의 처치 후 성급함 버프 중첩 설명은 유지하며, 장비 여러 개의 마법부여 레벨을 합산한다는 뜻으로 읽히지 않게 적용 슬롯을 명시한다.

## 확인 근거

대상은 Minecraft1.20.1 Fabric의 MCDA5.0.2, MCDW9.0.4, ExtraSpellAttributes1.3.2다. 현재 클라이언트와 서버의 세 모드 JAR 해시가 일치했다. 다른 소스 브랜치의 구현을 현재 설치본으로 가정하지 않고 실제 클래스 바이트코드를 확인했다.

- MCDA EnchantmentEffects에서10종은 vanilla EnchantmentHelper.getEquipmentLevel(method_8203)을 호출한다. 이 함수는 지정된 장비 슬롯에서 최고 레벨을 선택하며 합산하지 않는다. EnchantsRegistry에서 해당10종의 방어구4슬롯을 확인했다. fire_trail/lucky_explorer/swiftfooted는 같은 함수를 쓰지만 등록 슬롯이feet 하나라 신발 전용으로 안내한다. Frenzied/Reckless 호출부는 서버 플레이어 틱 경로이며, Reckless는 고정UUID 최대 생명력 수정과 단일 힘 효과를 갱신한다.
- ExtraRPGEnchantment.getEnchantmentLevelEquipmentSum은 방어구와 양손의 레벨을 더한다. 실제 LivingEntityMixin/RecoupMixin의 EVASION/SUPPRESSING/SPELLBREAK/DEFIANCEENCHANT/WARDINGENCHANT/BATTLEROUSE 소비 경로를 확인했다.
- MCDW applyRampaging은 Better Combat 설치 시 장비 최고 레벨 조회를 사용하지만, Rampaging의 등록 슬롯 자체가MAINHAND 하나다. 그 결과 방어구에 억지로 부여해도 방어구4부위를 합산하지 않는다. 정상 주손 무기의 레벨과 처치 버프 중첩은 별개다.

MCDA의 나머지 항목이나 다른 모드의 모든 마법부여를 일괄적으로 중첩 불가로 분류하지 않았다. 원본에서 이번 집계 경로를 확인한20개 설명만 갱신했다.

## 검증과 배포

원본 언어 Mixin/언어 팩토리/조회/재로드/구형 Paxi와의 우선순위를 오프라인에서 검증했다. 기존 팩의 광분·전투 자극·광란 설명을 넣은 fixture 위에 새 문구가 적용되며, 영어 언어에는 한국어 설명이 섞이지 않는다.52개 실제 언어 조회 검사와10개 native method 검사를 통과했다. 게임 화면이나 실제 전투는 실행하지 않았다.

683개 기존 누적 클래스, updater helper, 내장팩10,999개 파일을 바이트 단위로 보존한다. 최종 JAR에서 변경하는 항목은 한국어 정정표, 내부 빌드 식별, fabric.mod.json의 빌드 숫자뿐이다. 기존 내장 한국어 팩과 퀘스트2,686개 정확 일치 규칙 및 등록 초기화도 검증했다.

자원만 갱신하는 재현 명령:

```text
python tools/build_language_update.py --base LATEST_NIGHTLY_JAR --output STAGING --build NEW_BUILD --label nightly-YYYYMMDD.N
python tools/verify_translation_native.py --build-dir STAGING --classpath CLASSPATH_JSON --java-home JDK_HOME
python tools/verify_korean_pack.py --build-dir STAGING --classpath CLASSPATH_JSON --java-home JDK_HOME
```

서명된 client nightly-20261104.4로 배포하며 기존 클라이언트 업데이트/종료 후 교체 절차를 따른다. stable main, 서버 JAR, 운영 설정과 다른 모드는 이번 언어 안내 수정으로 바꾸지 않는다. 실제 게임에서 툴팁 줄바꿈/폭을 확인하는 검수는 남아 있다.
