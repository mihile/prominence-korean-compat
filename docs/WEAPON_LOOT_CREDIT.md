# Weapon loot credit — 2026-10-29

MCDW9.0.4 Committed(몰아붙이기)는 LivingEntityPlayerEntityMixin에서 기존 피해량에 더하므로 원래 공격자와 약탈/수집가 판정이 유지된다. Thundering/Thundering Shot, Shockwave, Swirling, causeSmitingAttack은 공격자 없는 lightningBolt/generic/magic 피해를 따로 주므로 누락됐다. 실제 설치 Fabric1.20.1 클래스와 Zenith Renewed2.0.2 Scavenger, vanilla loot context/looting helper, Porting Lib drop event를 확인했다.

## 구현 범위

- 피해량, 피해 속성, 원래 combat DamageSource는 유지한다. 소유자 보완은 dropLoot에 전달하는 전리품용 DamageSource와 killed-by-player loot context에서만 수행한다. 적중 콜백에서 다른 인챈트를 재귀 발동시키는 변경을 넣지 않는다.
- 천둥/천둥 사격/충격파/회오리/강타 보조 영역 피해에 정확한 사용자와 주손/보조손 보너스 문맥을 전달한다. 몰아붙이기 같은 원래 합산 피해는 원래 공식을 유지한다.
- 일반 투사체는 owner 지정 시 사용 중인 스택(없으면 주손)의 약탈/수집가 레벨과 플레이어UUID를 저장한다. 나중의 무기 교체·리비아탄 투척 빈손이 보너스를 바꾸지 않는다. 투사체NBT에 저장해 청크 저장/재로드도 처리한다.
- 리비아탄 collide의 부수 얼음 폭발, Simply Swords Imbued/Pincushion, Soulkeeper 등불 fallback에 제한된 소유자 문맥을 전달한다. SimplySwordsAPI의 위임 타격/후처리와 HelperMethods.applyAbilityDamageEnchantments의 실제 스택을 사용해 지연 타격의 원래 무기 보너스를 보관한다.
- 메아리 등 설치 무기 모드 상태 효과와 무기에서 부여한 독/위더는 적용 당시 소유자/레벨을 상태 인스턴스에 기록하고 틱 피해에 전달한다. 효과 병합·복사와NBT 저장/로드를 처리한다. 효과 제거 후에는 상태 인스턴스와 함께 사라진다.
- Soulslike 출혈은 상태 인스턴스가 아닌 BleedData 누적값이다. 증가를 발생시킨 마지막 무기 문맥을 별도 persistent NBT에 기록한다. 자연 감소는 기록을 유지하고0/혈손실 발동 후에는 지운다. 소유자를 확인할 수 없는 새 증가가 들어오면 예전 플레이어 기록을 지워 몹/환경 출혈을 빼앗지 않는다.
- 약탈은 현재 무기 레벨을 원래 무기 레벨로 교체하며 다른 기존 보정분을 유지한다. 수집가는 기존확률/추가굴림을 그대로 쓰고 레벨 조회만 보정한다. 레벨을 중복 가산하거나 강제로 전리품 테이블을 추가 호출하지 않는다.

## 제한과 검증

발생자와 원래 무기를 확인한 경로에만 적용한다. 환경 피해·소유자 없는 효과·자기 피해·다른 명시적 공격자의 반격은 기존 문맥을 상속하지 않는다. 소유자가 로그아웃했거나 다른 차원으로 이동해 해당 월드에서 조회되지 않으면 가짜 플레이어를 만들지 않는다. 이미 존재하던 투사체/효과에 과거 무기 정보를 추측해 넣지 않는다. 별도의 모드가 독자적으로 생성한 장판/소환수 등까지 모두 지원한다고 주장하지 않는다.

확인: 현재 클라/서버 대상 모드의 버전과 실제class SHA256, 원래 전투 source 유지 검사, 1.20.1 클라/전용 서버 클래스의 실제 Mixin 변환과ASM검사, 실제 production LootCredit 로직의 투척/무기교체/추가타/환경·자기·반격·오프라인 격리 fixture, 실제NBT roundtrip/구형NBT, 중첩/예외 ThreadLocal 복원, 누적클래스 보존 및 updater 검증. 실제 플레이 중 드롭 통계 검수는 아직 하지 않았다.

공통 소스는 client `src/main/java/local/prominence/lootcredit`, server `src/compat/loot-credit/java/local/prominence/lootcredit`에 동일하게 유지한다. 패키지에 client API는 없다. 클라이언트 nightly-20261029.3 / server build-20261029.1로 공개하며 멀티플레이의 실제 판정에는 서버 업데이트가 필요하다. 서버는 다음 정상 시작 시 updater 적용 대상이며 실행 중 JAR 교체/서버 재시작은 하지 않았다. JAR 전용 수정으로 ZIP은 별도 갱신하지 않는다.
