# EMI 첫 접속 초기화 개선 분석·구현 인계

작성일: 2026-10-30. 대상 측정: `20261030-151854-35065c`, 클라이언트 build **2026103007**.

최초 문서는 기존 JFR·PresentMon·힙 덤프/MAT 및 설치된 모드의 코드 조사다. 이후 구현 결과는 아래 2026103008 항목에 기록했다. 아래 수치의 게임 측정 기준은 계속 build2026103007이며, 새 수정의 접속 시간은 아직 실측하지 않았다.

## 1. 결론과 구현 순서

EMI는 이번 접속에서 **18.723초** 만에 초기화를 끝냈다. 원인은 작은 문자열이나 NBT를 디스크에서 읽는 일 하나가 아니다. 서버 데이터 수신 후 각 플러그인이 합성 레시피와 안내를 만들고, 이를 숨김 검사·재료/결과 역색인·정렬·툴팁 검색으로 여러 차례 변환한다. 그 과정에서 많은 임시 ItemStack과 부가 객체가 만들어진다.

큰 폭으로 줄이려면 **레시피 정보를 한 번 해석하고 재사용하는 구조**가 핵심이다. 기존 패치는 조회·문자열·불필요한 복사의 일부를 줄였지만, 반복 해석과 두 번째 전체 색인 생성은 남아 있다.

권장 구현 순서:

| 순서 | 작업 | 판단 |
|---|---|---|
| A | Prominent의 숨김 아이템 판정에서 ItemStack 생성 제거 | 가장 좁고 근거가 명확한 첫 수정 |
| B | EMI의 정렬 후 두 번째 전체 색인 생성을 기존 관계표의 정렬로 대체 | 구조적인 핵심. 별도 구현 단위로 충분히 검증 |
| C | 모루 마법부여 레시피의 재료·책 표현 반복 생성 줄이기 | B와 연계하되 중복 계산한 절감량을 합산하지 않음 |
| D | EMI Enchanting의 전체 아이템×인챈트 검사와 화면 생성 분리 | UI만 지연해도 전체 검사가 사라지지는 않음. 순수 후보 판정부터 |
| E | Create의 아이템×유체 조합 탐색 중 무효 후보 제거 | 큰 할당 후보. Fabric lookup의 fallback 때문에 조건부 설계 필요 |
| F | 검색 문서 생성 개선, 이어서 검증 가능한 검색 캐시 | 현재 4.025초. 접미 배열 교체만으로는 부족 |

처음부터 A~F를 한 패치로 모두 재작성하기보다는 **A+B+C를 첫 구현 묶음**으로 한다. 실제 검증이 완료된 소규모 개선은 유지하고, B의 새 경로와 기능적으로 겹치는 이전 삽입 보완은 경로별로 정리한다. 단순 스레드 수·우선순위 변경, 접속 뒤로 작업 미루기보다 총 작업량을 줄이는 것이 목표다.

목표치는 `18.7초 → 10초 이하`를 **개발 목표**로 둘 수 있지만, 현재 자료로 달성을 보장할 수는 없다. `5초 이하`를 약속할 근거도 없다. 완료 시간과 긴 프레임을 따로 평가해야 한다.

## 2. 경로와 실행 기준

| 역할 | 경로 |
|---|---|
| 마스터 인스턴스 | `C:\Users\maste\AppData\Roaming\PrismLauncher\instances\Prominence™ II- Hasturian Era` |
| 현재 소스 저장소 | 위 경로의 `KoreanPatch\Compat\repository` |
| 현재 설치 JAR | 위 경로의 `minecraft\mods\prominence-korean-compat-1.0.0.jar` |
| 현재 실행 로그 | 위 경로의 `minecraft\logs\latest.log` |
| 측정 폴더 | `C:\Users\maste\AppData\Local\Temp\prominence-profile\20261030-151854-35065c` |
| 이번 조사 결과·추가 MAT·역컴파일 | 마스터의 `KoreanPatch\Compat\build\emi-review-151854` |
| 최근 빌드 입력·검증 참고 | 마스터의 `KoreanPatch\Compat\build\reviewed3007` |
| classpath | 마스터의 `KoreanPatch\Compat\classpath.json` |
| 재사용 측정 도구 | 마스터의 `Profile_Client.bat`, `ClientProfiler\` |

측정 `state.json`의 설치 SHA-256:
`7e2ac9fbf4821361d6cb414be2cc66a40d242e30f93c62146fd5e4e379814785`.

실제 대상은 Minecraft **1.20.1 / Fabric**, EMI **1.1.24+1.20.1+fabric**, EMI Enchanting **0.1.2+1.20.1**, EMI Loot **0.7.9+1.20.1+fabric**, Create **6.0.8.1+build.1744-mc1.20.1**, Prominent **4.1.1**이다. 팩 버전 4.1.3과 Prominent 모드 버전은 서로 다르다.

실제 게임의 공용 Gson은 **2.10**이다. `reviewed3007/runtime-classpath.json`은 실제 Gson과 네이티브 Mixin 검증용 의존성을 포함한다. 지난 build3006은 빌드용 Gson2.10.1에서만 존재하는 메서드 때문에 시작에 실패했다. 이후 구현도 설치 모드뿐 아니라 런처의 라이브러리 ABI를 맞춰야 한다.

## 3. 측정 결과

### EMI 단계별 벽시계 시간

시각은 한국시간. 정상 초기화는 **15:19:05~15:19:23**에 한 번 실행됐다. 태그 동기화 로그가 여러 번 있다고 전체 EMI 초기화가 그만큼 반복된 것은 아니다. 접속 전·종료 시의 `completed=false` 정리 작업과 구분했다.

| 단계 | ms | 전체 비율 |
|---|---:|---:|
| 데이터 정리 | 83.777 | 0.45% |
| 플러그인 initialize | 34.603 | 0.18% |
| 태그 처리 | 244.088 | 1.30% |
| 아이템 목록 구성 | 2,417.599 | 12.91% |
| 플러그인 register | 6,473.618 | 34.58% |
| 아이템 색인 마무리 | 511.812 | 2.73% |
| 후속 레시피 등록 | 1,792.741 | 9.58% |
| 레시피 색인·관련 후처리 | 3,108.338 | 16.60% |
| 검색 구축 | 4,024.875 | 21.50% |
| 마무리 | 31.610 | 0.17% |
| 전체 | **18,723.078** | 100% |

`Baked 145136 recipes in 3026ms`는 `EmiRecipes.bake()` 로그다. 3,108ms 단계에는 BoM·사용자 데이터 처리도 포함된다. **145,136은 필터 적용 전 recipes.size()**이며 화면에 실제 보이는 레시피 수라고 부르면 안 된다.

별도 `Thread-70`에서 정렬된 Manager를 다시 만드는 데 **2,220ms**가 걸렸다. 이 작업은 검색 단계와 겹치므로 전체 18.723초에 추가로 더하지 않는다. 이를 제거할 때의 이득은 순수 정렬 시간 전체가 아니라, 중복 재료 확장·해시 조회·복사·색인 재구축을 제거하는 부분과 검색과의 CPU/할당 경합 완화다.

### 플러그인 register 비용

| 플러그인 | ms |
|---|---:|
| EMI Enchanting | 1,940 |
| EMI 기본 VanillaPlugin | 1,692 |
| Modern Industrialization | 403 |
| Create | 396 |
| Farmer's Delight | 367 |
| All The Trims | 355 |
| EMI Loot | 280 |
| Bumblezone | 201 |
| EMIffect | 130 |

Create의 **후속 등록**은 위 396ms에 포함되지 않는다. 후속 등록 전체 1,793ms의 JFR에서 `CreateEmiPlugin.addDeferredRecipes`가 확인됐다. 별도 플러그인별 후속 타이머가 없으므로 1,793ms 전부를 Create에 귀속시키지는 않는다.

EMI Loot의 세 번의 메인 스레드 대기는 각각 **165.07 / 29.74 / 77.40ms**였다. 합계 약272ms로 플러그인 280ms 대부분이다. 현재 전리품 확률 기능을 없애는 것이 18초 문제의 주된 해결책이라는 근거는 없다.

### JFR 할당·CPU 표본

`analysis/allocation.tsv`는 샘플링 가중치로 추정한 할당량이다. 실제 계속 보유하는 힙, 정확한 생성 객체 수, 개별 함수 소요 시간과 다르다.

| 호출 경로 | 이번 추정 할당량 |
|---|---:|
| EMI 작업 스레드 Thread-67 + 정렬 Thread-70 전체 | 13,262,228,136 bytes |
| Create 후속 유체 탐색 | 2,799,659,768 bytes |
| EMI Enchanting 생성자 | 2,224,517,360 bytes |
| Prominent의 SKILLET 숨김 predicate | 943,109,376 bytes |
| EmiAnvilEnchantRecipe가 포함된 호출 전체 | 1,354,343,160 bytes |

마지막 행 등은 다른 분류와 겹칠 수 있다. 이 표를 합산해서 새로운 총량을 만들지 않는다. ItemStack 생성은 이 팩에서 NBT뿐 아니라 CIT Resewn, AzureLib 등 여러 Mixin의 부가 객체·콜백을 유발한다. 그래서 단순 아이템 비교에 `getItemStack()`을 호출하는 것도 값비싸다.

두 EMI 작업 스레드의 실행/네이티브 표본은 총719개였다. 이번 조사 분류에서 Manager 경로214개, 검색174개, Vanilla `addRepair`60개, Create 후속52개, Enchanting 생성자43개였다. 분류 우선순위가 있는 표본 집계이며 시간 비율로 환산하지 않는다.

### 힙 덤프와 MAT

기존 덤프의 인덱스를 재사용했다. 새 게임 덤프나 강제 GC를 실행하지 않았다.

- MaxHeap: **6,341,787,648 bytes**, 측정 중 GC 후 최대 **5,666,504,704 bytes**, 약89.35%.
- 기록 중 GC pause 합계 **1,551.255ms**, 최장 **92.749ms**, Full GC **0회**. 사후 힙 덤프의 정지는 이 합계에서 제외됐다.
- `ItemEmiStack`: **2,546,337개**, shallow heap142,594,872 bytes.
- Minecraft ItemStack: **1,916,630개**, shallow heap229,995,600 bytes. 모두 EMI 소유라고 단정하지 않는다.
- `EmiAnvilEnchantRecipe`: **55,436개**. 등록 전 전체 레시피 수와 단순 비교하면 약38% 규모지만, 이것이 정확한 필터 후 카테고리 비율은 아니다.
- `EMI Enchanting.EnchantmentRecipe`: **179개**. 개별 retained heap 합계30,080,752 bytes. 공유 객체는 이 합계만으로 모두 잡히지 않는다.
- 실제 Manager는 **사용 Manager 하나 + EMPTY 하나**였다. 사용 Manager의 retained heap26,143,624 bytes, 입력 키35,863 / 출력 키24,102. 두 번 생성한다는 이유로 두 대형 Manager가 계속 누수된다고 설명하면 틀린다.
- Minecraft `class_1128` 검색 배열4개의 retained heap 합계21,772,104 bytes. 기본 수집 XML은 `dev.emi.emi.search.SuffixArray`를 조회해 실제 클래스를 놓쳤으므로 이번에는 정확한 native 클래스로 추가 조회했다. 이번 요청에서 수집 도구 자체는 수정하지 않았다.

2.55백만 개의 ItemEmiStack이 전부 어느 단일 기능 때문인지까지 증명한 것은 아니다. 향후 메모리 최적화에서는 공유 참조를 포함한 retained set/루트별 소유 경로를 확인하고, 전체 shallow 합계를 절감 가능량으로 쓰지 않는다.

### 현재 로그와 프레임 해석

조사 시 현재 로그는590,867 bytes /5,509줄 /WARN968 /ERROR31, 측정 저장 로그는575,344 bytes /5,364줄 /WARN955 /ERROR31이었다. 같은 build3007 실행의 후속 종료 로그가 현재 파일에 추가돼 있다.

주요 반복은 Moonlight/Supplementaries 셰이더 uniform/sampler 경고(종류별59회), 그 외 누락 리소스·EMF 모델 재생성·Xaero 변종 ID 경고가 있다. 검색 중 `Connector positionNot a map: null`도 나온다. 현재 자료에 EMI 전체 초기화 실패나 재시도 루프는 없다. `tainted by`는 Mixin 적용 공지이며 그 자체가 실패는 아니다.

PresentMon에는15:19:01.892524→15:19:10.505930 사이 **8,613ms 관측 공백**이 있지만 보고된 FrameTime과 일치하지 않는다. 이를 하나의 정확한8.6초 프레임이나 전부 EMI가 멈춘 시간으로 보고하지 않는다. EMI 작업 중에도 Iris·Voxy·렌더 작업이 함께 있었다.

아이템 그룹 구성에서 EMI 스레드는 실제로2,292.26ms 동안 `CompletableFuture.join()`으로 메인 스레드 작업을 기다렸다. 이 시간에는 큐 대기와 실행이 섞인다. 그룹별로는 AE2 덮개571.85ms, Create191.18ms 등이 기록됐다. 이 부분은 백그라운드 등록과 달리 첫 접속의 직접적인 렌더 정지 개선 후보다.

## 4. 기존 수정은 적용됐는가

적용됐다. 이번 상태·로그에서 lookup, recipe generation, suffix dedupe, tooltip, attribute candidates, recipeInsertion 등이 enabled이고 계측 값도 나온다. 다만 활성화됐다는 것과 전체 시간이 짧아졌다는 것은 다르다.

- 전체 reload CIA 조회2,462,168회 중 hit2,462,062 /miss106. 조회가 사라진 것은 아니며 반복 캐시 조회 비용은 남는다.
- MCDW 조건 조회5,170,768회 중 빈 view 생성5,100,682회 회피. 전체 인챈트 적합성 루프는 여전히 돈다.
- 검색 attribute 후보: 원래 후보46,443,158개 대신2,259개 선택, fallback0. 이 부분을 다시 같은 목적의 캐시로 덮는 것이 우선은 아니다.
- suffix add127,457회, 중복15,099회 회피. suffix generate 네 번의 합계는 **808.908ms**(497.565/213.098/98.238/0.007)이다. 검색 전체4,024.875ms 중 약20%이고, 전체 초기화의 약4.3%다.
- 기존 RecipeIndexInsertion은 특정 안전한 ItemEmiStack의 **기존 버킷 삽입 때 복사**를 피한다. Manager 두 번 생성, getInputs/getOutputs의 반복 객체 생성, 집합 탐색 자체를 없애지는 않았다.
- AE2 덮개 중복 후보 제거는 후보16,296개 중876회였다. fixture에서 큰 배수가 나왔어도 실제 팩의 중복 비율은 약5.4%다. 이번 덮개 작업568ms가 남아 있는 것을 설명한다.
- `limitedBudgetCalls=0`, meshAdmission/unchangedStateRemap 등은 퇴역 상태다. 오래된 문서의 조정 설명을 현재 동작으로 재활성화하지 않고 현행 코드와 가드를 기준으로 삼는다.

11:56 측정의 전체 EMI18.034초와 이번18.723초는 같은 서버 진입 조건의 엄격한 A/B가 아니다. build3007의 주요 변경은 Iris 캐시 및 시작 ABI 복구이고 EMI 구조를 재작성한 것이 아니므로, 이번 숫자로 특정 EMI 패치의 회귀나 개선을 확정하지 않는다.

## 5. A — Prominent 숨김 predicate의 불필요한 스택 생성 제거

**확정한 코드:** 설치된 Prominent4.1.1의 `elocindev.prominent.plugin.ProminentEMIPlugin.initialize`는 `emiStack.getItemStack().isOf(ItemRegistry.SKILLET)` 형태의 predicate를 등록한다. 숨기려는 대상은 아이템 종류 하나인데 전체 레시피의 숨김 검사에서 스택을 계속 만든다. 해당 경로의 JFR 할당 추정은943MB였다.

### 구현 설계

- 그 predicate의 아이템 비교 경로만 좁게 보완한다. 정확한 native `ItemEmiStack`은 `getKey()`가 Item이고, 내부 `item`은 final이다.
- positive int로 손실 없이 표현되는 amount, native item/key 구현 등 동등성이 확인되는 경우 item identity로 SKILLET 여부를 검사한다. zero/negative/long→int overflow는 native `getItemStack()`의 empty 판정과 차이가 있을 수 있어 원본 fallback 또는 동일한 cast/empty 의미가 필요하다.
- custom EmiStack/서브클래스/유체/자체 getItemStack 구현은 원래 predicate를 실행한다. 다른 플러그인의 NBT·진행도·플레이어 의존 필터는 건드리지 않는다.
- native ItemStack의 설치된 Mixin까지 대조해 `isOf` 의미를 확인한다. 이 목적 때문에 모든 `getItemStack()`에 전역 캐시를 추가하지 않는다.
- 수정 지점은 Prominent의 lambda 또는 initialize에 전달하는 predicate. 원래 SKILLET 숨김 및 ID·레시피 필터 결과가 동일해야 한다.

### 검증·완료 조건

정상 아이템/숨김 아이템/NBT/0개/음수/overflow/empty/custom 스택의 원본·패치 판정 비교. 실제 native Mixin 적용 및 cumulative 검증. `fastPath/fallback/avoidedStackBuilds`는 reload 끝 한 줄로 집계한다. 다음 측정에서 해당943MB 표본 경로와 필터 단계 시간이 줄었는지 확인한다. 전체943MB가 정확히 절감된다고 미리 주장하지 않는다.

## 6. B — 관계표는 한 번, 정렬은 별도 뷰로

**확정한 코드:** `EmiRecipes.bake()`는 필터 후 `new Manager(..., false)`를 만들고 곧바로 Worker를 시작한다. Worker는 `new Manager(..., true)`로 거의 같은 처리 전체를 다시 한다. true의 추가 목적은 카테고리별 정렬이다. 양쪽 모두 재료·촉매·출력을 펼치고 스택 비교/복사/집합 삽입 및 byWorkstation 구성을 수행한다. 이번 Worker는2.220초였다.

### 추천 구조

1. 한 reload generation 안에서 **관계표 생성**과 **표시 순서 정하기**를 분리한다. 첫 Manager를 만드는 동안 비교 의미가 검증된 재료 키와 recipe membership을 한 번만 구축한다.
2. `byId`, 카테고리 membership, 입력/출력 membership은 재사용 가능한 snapshot으로 보관한다. 바깥에 공개할 EmiStack 자체를 전역 intern하거나 mutable 스택을 다른 레시피와 공유하지 않는다.
3. 정렬 Worker는 기존 comparator로 카테고리별 recipe 순서만 정한다. 정렬된 recipe→rank를 만든 뒤 기존 입력/출력 버킷의 목록 순서를 그 순서에 맞게 재구성한다. rank의 기준은 native의 카테고리 순회 순서와 카테고리 내부 stable sort다. 버킷의 LinkedHashSet이 쓰던 중복 제거 의미도 보존하고, recipe.equals/hashCode가 특이한 구현은 별도 검증 또는 fallback한다. 다른 카테고리 사이 순서와 comparator 동률의 stable order도 native와 같아야 한다.
4. 완성된 별도 뷰를 한 번에 publish한다. 현재 Manager의 목록을 정렬 Worker가 제자리 수정하지 않는다. recipe를 다시 펼치거나 ItemStack을 재생성할 필요가 없는 경로를 목표로 한다.
5. disconnect/reload/retry 시 generation을 폐기한다. 오래된 Worker가 새 Manager나 새 activeWorker를 덮지 못하도록 generation/worker identity를 확인한다. 기존 Worker의 마지막 `setWorker(null)`도 함께 검토한다.
6. native가 전역 `byWorkstation`을 각 Manager 생성 시 addAll하는 부작용이 있다. 단순 생성자 복제/재사용은 중복 누적·동시 수정 문제를 만들 수 있다. 새 결과에 대응하는 workstation 뷰를 별도로 만들고 publish하는 설계가 필요하다. 이 변경의 중복·순서 차이는 별도 회귀 항목으로 기록한다.

### 동적 레시피와 비교 전략

공개 EMI API의 임의 `getInputs/getOutputs/getCatalysts`가 순수하다고 보장할 수 없다. 처음에는 native 클래스와 이번 팩에서 검증한 안정된 구현을 대상으로 한다. 원본 callback 재평가가 필요한 custom 레시피/비교 함수는 원래 경로를 유지하거나 동일 횟수로 재평가한다. 전체 Manager를 대체할 때에는 custom 경로의 membership과 정렬 결과를 새 뷰에 합치는 과정까지 설계해야 한다.

`EmiStackList.ComparisonHashStrategy`, Comparison.DEFAULT/compareNbt/custom의 의미, NBT variant, crafting remainder, catalyst, duplicate ingredient, duplicate recipe ID의 first-wins, null ID, 카테고리 정렬/동률을 보존한다. 순서가 붙은 set을 무조건 hash set으로 바꾸지 않는다.

### 구현 선택지와 판단

- **추천:** unsorted 뷰의 관계표를 재사용하고 sorted 뷰만 생성한다. 현재 빠른 중간 결과와 최종 정렬을 모두 유지한다.
- **비교용 대안:** 처음부터 sorted Manager 하나만 만들기. 총 CPU는 줄일 수 있지만 정렬 비용이 동기 초기화의 critical path로 옮겨져 완료가 더 느려질 수 있다. 측정 없이 기본안으로 채택하지 않는다.
- **기각:** Worker만 없애고 정렬 기능을 포기하기. 사용자에게 보이는 동작을 바꾸며 구조 개선이 아니다.

### 검증

현재 패치가 적용된 native를 기준으로 input/output/category/workstation/id별 **결과와 순서**를 대조한다. custom comparisons, 동일 ID 복수 recipe, 레시피 수정/재로드, 취소·연속 접속, 검색과 정렬의 동시 접근을 포함한다. 정렬 후 membership 확장 횟수가 두 번째 전체 탐색 대신0 또는 명시한 fallback 수로 줄어야 한다.

현재 `RecipeIndexInsertion`은 원본 fallback용으로 남길 수 있다. 새 경로에서도 중복 적용하면 모든 스택마다 guard/HashMap 조회를 또 하게 되므로 중첩 경로를 분리한다. 이번 JFR에 그 helper가 보인다는 이유만으로 기존 패치가 역효과라고 단정하지 않는다.

## 7. C — 모루 안내 55,436개의 반복 스택 구성 줄이기

`EmiAnvilEnchantRecipe`는 도구·인챈트·레벨·ID만 보관한다. `getInputs()` 호출 때마다 도구 스택과 책을 새로 만들고, `getOutputs()`도 매번 도구 스택을 만든다. `getBook()`은 **책을 만들기 위해 먼저 인챈트된 도구를 만든 뒤 그 도구의 첫 Enchantments 태그에서 ID를 읽는다.** 필터/첫 Manager/정렬 comparator/두 번째 Manager가 이를 반복한다.

### 설계

- B의 reload snapshot에서 검증된 native 레시피의 입력/출력 표현을 한 번 계산해 색인·필터·정렬에 재사용하는 것을 우선한다. UI의 `addWidgets/getTool`은 현재 의미를 유지한다.
- 필요하면 `{item, original default-stack NBT, enchantment, level}`을 읽기 전용 descriptor로 만들고 색인용 값과 화면용 mutable 객체를 분리한다. recipe가 사라지면 참조도 해제한다.
- 인챈트 책은 같은 native 의미를 가진 경우 `{book enchant ID, level, NBT}` 단위로 불변 원자료만 공유한다. mutable EmiStack/List/NBT를 그대로 공유해 플러그인이 한 번 수정하면 다른 레시피가 바뀌는 구조는 피한다.
- **주의할 실제 함정:** MCDW처럼 getDefaultStack()에 고유 인챈트가 이미 있는 도구는 `getBook()`이 읽는 첫 태그가 생성자의 enchantment와 같지 않을 수 있다. 생성자 인챈트 ID로 무조건 바꾸면 성능 수정 도중 출력 의미까지 바뀐다. 원래 결과 비교 또는 해당 도구 fallback이 필요하다. 별도 기능 버그 수정과 섞지 않는다.

B가 제거하는 반복과 C의 절감이 겹친다. `1.354GB + B 전체 할당`을 그대로 합산하지 않는다. 검증 시 getInputs/getOutputs/getBook 호출 수와 임시 스택 생성 수를 분리한다.

## 8. D — Enchanting: 적합성 표와 화면을 분리

설치된0.1.2 생성자는 인챈트마다 전체 아이템을 순회하며 **새 ItemStack으로 `isAcceptableItem`**을 호출한다. 유효 아이템을8개 표시 그룹으로 묶고, 배타 인챈트 목록·설명 줄 나눔·PageWidget까지 접속 시 생성한다. 179개의 안내 레시피를 위해 큰 중간 작업이 발생한다. 기존 EnchantmentBuildScope는 MCDW 빈 조건 view와 CIA lookup을 줄일 뿐 이 루프를 제거하지 않는다.

### 실용적인 순서

1. 생성자 작업을 적합 아이템 목록 계산 / 입력 관계 / 화면 PageWidget 생성으로 구분해 계측한다. 전체1.94초 중 어느 비중인지 현재 계측에는 없다.
2. **화면 레이아웃만** `addWidgets` 때 처음 만들도록 분리할 수 있다. `getInputs()`에는 유효 아이템·배타 책이 들어가므로 그 계산까지 화면으로 미루면 레시피 검색 의미가 바뀐다. 한꺼번에 수백 PageWidget을 만드는 비용 감소와 첫 페이지 열기 지연을 함께 확인한다.
3. 적합성은 정확한 native/설치 Mixin에서 **Item 종류·태그만 읽는다고 증명한 검사**에 한해 `{registry/tag/config generation, enchantment, item}` 결과표를 만든다. unknown predicate는 매번 새 스택으로 원래 검사를 수행한다.
4. VanillaPlugin.addRepair의 검사와 Enchanting 검사를 무작정 합치지 않는다. 전자는 `getDefaultStack()`과 추가 enchantable 조건을 쓰고, 후자는 `new ItemStack(item)`을 쓴다. 고유 인챈트/NBT가 있는 도구는 입력 자체가 다르다. 공유하려면 입력 방식까지 키에 넣고 실제 동일성을 확인해야 한다.
5. 상충 책 표현은 같은 register 범위에서 원본 데이터를 재사용할 후보지만, 페이지 위젯의 상태/currentPage는 레시피별로 유지한다.

이 기능을 제거하거나 유효 아이템 목록을 빈 값으로 만드는 것은 이번 목표가 아니다. 실패 시 해당 후보만 원본으로 돌아가는 독립 가드가 좋다.

## 9. E — Create 유체 탐색

`CreateEmiPlugin.addDeferredRecipes`는 index의 아이템마다 유체 목록 전체를 돌며 ItemStack 복사 → MutableContainerItemContext → FluidStorage.ITEM lookup → 삽입 트랜잭션을 시도한다. 액체를 담을 수 없는 대다수 아이템에도 동일한 준비를 유체 수만큼 반복한다. 이번 추정 할당 약2.80GB다.

### 안전한 후보 축소

- 먼저 설치된 Fabric `ItemApiLookup` 구현/등록 상태를 읽어 **아이템별 provider와 fallback/self provider**를 구분한다. 정적 등록 테이블에 없다는 사실만으로 부정하면 fallback으로 작동하는 모드 컨테이너를 놓친다.
- 검증된 순수 provider 경로에 한해, 정확한 초기 item/NBT/amount/context에서 저장소가 없다는 판정을 같은 reload 안에서 재사용한다. 단순 item ID 키는 NBT 컨테이너를 구분하지 못한다.
- provider가 알려지지 않거나 side effect/외부 상태가 있는 경우 기존 루프를 유지한다. 첫 null을 모든 유체로 일반화하는 패치부터 넣는 것은 아직 근거가 부족하다.
- 저장소가 있는 아이템은 fluid별 새 초기 상태·원래 transaction/commit·잔여 용기·유체량을 유지한다. Potion filling/draining의 별도 분기도 보존한다.

필요한 추가 정보는 **이미 설치된 클래스와 registry/provider 데이터**다. 또3분 플레이 측정부터 요구할 일이 아니다. 오프라인 fixture에서 known/fallback/NBT별 결과·등록 recipe ID·입출력·유체량을 비교한 뒤 다음 통합 측정으로 확인한다.

## 10. F — 검색 구조 개선

검색4.025초 중 suffix generate는0.809초다. 따라서 suffix 정렬만 대체해서 전체 EMI를 획기적으로 줄인다는 설명은 맞지 않는다. 나머지는 모든 index 스택의 이름·툴팁·모드/태그 정보 생성, 문자열 추출 및 배열 삽입 등이다. JFR에는 Accessories/Zenith/Custom Tooltips/Create 등 툴팁 경로가 보인다.

### 현재 접속을 줄이는 방향

- 한 검색 문서 작성 안에서 같은 ItemEmiStack의 `getItemStack()`을 name/tooltip/enchant 구분용으로 여러 번 만드는지 확인한다. 함수별 새 스택을 전제로 하는 callback의 변이 가능성을 검증한 경로부터 불변 원자료/중간 읽기 결과를 공유한다.
- 원래 tooltip 내용과 한글 검색을 유지한 상태에서 문서 `{name strings, tooltip strings, mod strings, aliases}`를 한 번 구성한다. 필터와 suffix insertion 단계가 같은 정보를 다시 만들지 않도록 한다.
- `#설명`, `@모드`, alias, 일반/정규식 검색, 한국어 및 NBT 다른 스택을 기준으로 원본과 검색 결과를 대조한다. 표시용 줄바꿈을 줄이거나 tooltip callback을 생략하면 검색어가 사라질 수 있다.
- 이름만 먼저 제공하고 tooltip을 나중에 만드는 방식은 **준비 완료 기준을 바꾸는 UX 변경**이다. 사용자에게 완성되지 않은 상태를 완성으로 표시하는 방법을 속도 개선으로 계산하지 않는다.

### 큰 추가 도약 후보: 재사용 가능한 검색 문서 캐시

재접속/다음 실행까지 매번 같은 검색 문서를 만들지 않는 방식은 큰 후보지만 **신규 캐시가 없는 첫 실행**을 줄이는 방법은 아니다. 순서상 A~E 이후다.

- 디스크에는 문자열·아이템/변형 식별자만 저장하고 Minecraft 객체·world·player를 직렬화하지 않는다.
- 유효성 키에 Minecraft/모드 버전과 relevant class pins, 언어, 리소스팩 내용·순서, 툴팁 관련 설정, registry/tag/recipe/data generation, custom 비교/alias/비활성화 필터가 포함돼야 한다.
- 플레이어 스탯/현재 키 설정/진행도에 의존하는 tooltip은 persistent cache 대상에서 제외하거나 값 변경 세대로 분리한다. 현재 JAR에는 동적 키/확률/속성 표시도 있다.
- 알려진 결정적 문서만 재사용하고 나머지는 그 접속에서 계산하는 혼합 경로가 현실적이다. unknown callback까지 전체 캐시 hit로 간주하면 번역·퀘스트·능력치 갱신이 오래된 상태로 남는다.
- 캐시 검증을 위해 모든 tooltip을 다시 생성한다면 목적을 잃는다. 원인별 generation/fingerprint를 저렴하게 얻는 설계가 선행돼야 한다.

## 11. 첫 프레임 멈춤을 따로 줄일 후보

아이템 그룹의 메인 스레드 작업2.292초와 EMI의 백그라운드18.723초를 분리한다. `EmiStackList.reload()`는 client.submit(...).join()으로 전체 그룹을 한 번에 갱신한다. AE2 덮개만571ms이므로 group 사이 yield만 해도 단일 그룹의 긴 정지는 남는다.

가능한 별도 개선은 그룹별 순서·context·수집 완료 시점을 유지하면서 메인 스레드 일감을 분할하고, 결과가 완성된 뒤 EMI 목록을 publish하는 방식이다. 다만 총 시간이 늘 수 있고 그룹 하나의 콜백을 중간에서 쪼개려면 해당 모드의 안전한 반복 경계가 필요하다. **벽시계 단축과 프레임 완화는 별도 성과**로 기록한다. 이전 Fastload처럼 아직 준비되지 않은 화면을 급하게 보여주는 방식은 해결책이 아니다.

## 12. 구현자가 바로 볼 파일

저장소에서 직접 관리 중인 파일:

- `src/main/java/local/prominence/work/RecipeIndexInsertion.java`
- `src/main/java/local/prominence/work/mixin/EmiRecipeInsertionMixin.java`
- `src/main/java/local/prominence/work/mixin/EmiInitialWorkMixin.java`
- `src/main/java/local/prominence/work/mixin/EmiCreativeTimingMixin.java`
- `src/main/java/local/prominence/work/WorkGuard.java`
- `src/main/resources/prominence-emi-home-pins.json`
- `src/main/resources/prominence-emi-home.client.mixins.json`
- `tools/verify_emi_home_native.py`, `tools/verify_emi_refresh.py`, `tools/verify.py`
- `tests/RuntimeGsonAbiTests.java`
- `docs/EMI_INITIAL_WORK.md`, `EMI_CREATIVE_GROUPS.md`, `EMI_ATTRIBUTE_CANDIDATES.md`, `JOIN_CONTENTION.md`, `SOURCE_REVIEW_WORKFLOW.md`

이전 누적 모듈 일부는 현재 src 트리에 없고 최신 통합 JAR 내부 소스에 있다. 소스가 안 보인다고 누락된 기능으로 판단하지 않는다. 이번에는 필요한 소스만 조사 폴더 `compat-source/`에 추출했다.

- JAR `META-INF/prominence-korean-compat-source/emi-recipe-generation/local/prominence/recipegeneration/EnchantmentBuildScope.java`
- 같은 모듈의 `mixin/EmiEnchantingRegisterMixin.java`, `CreateCuttingScopeMixin.java`
- `.../emi-index-build/local/prominence/emiindex/EmiIndexBuildScope.java`와 recipe/search scope Mixins
- `.../emi-item-lookups/local/prominence/emilookup/EmiItemLookupScope.java`
- `.../emi-search-tooltips-emiffect/local/prominence/tooltipopt/TooltipMatchScope.java`

새 수정은 현행 저장소에 source를 관리하고, build.py가 최신 누적 base와 합치는 기존 방식을 쓴다. A/B 가드는 독립적으로 두어 새 구조만 비활성화하고 기존 호환 기능을 유지할 수 있게 한다.

설치 JAR에서 확인한 코드는 조사 폴더 `native/` 아래에 있다:

- `dev/emi/emi/runtime/EmiReloadManager.java`
- `dev/emi/emi/registry/EmiRecipes.java` — Manager/Worker 포함
- `dev/emi/emi/registry/EmiRecipeSorter.java`, `EmiStackList.java`
- `dev/emi/emi/runtime/EmiHidden.java`, `search/EmiSearch.java`
- `dev/emi/emi/api/stack/ItemEmiStack.java`
- `dev/emi/emi/VanillaPlugin.java`, `recipe/special/EmiAnvilEnchantRecipe.java`
- `fzzyhmstrs/emi_enchanting/EnchantmentRecipe.java`, `EmiClientPlugin.java`
- `com/simibubi/create/compat/emi/CreateEmiPlugin.java`
- `elocindev/prominent/plugin/ProminentEMIPlugin.java`

CFR의 intermediary 이름은 `class_1799=ItemStack`, `class_1792=Item`, `class_1887=Enchantment`, `class_1128=SuffixArray`다. 실제 Mixin descriptor/명령은 javap/ASM으로 확인한다. 역컴파일 결과 자체를 무검토로 제품 소스로 복사하지 않는다.

## 13. 검증과 한 번의 후속 측정

오프라인 검증 기준은 **원본이 아닌 현재 build3007 누적 패치까지 적용된 경로**다. 앞선 최적화가 켜진 상태에서 새 수정의 추가 이득과 결과를 비교한다.

| 영역 | 유지해야 할 결과 |
|---|---|
| 색인 | recipe ID/입력/출력/catalyst/카테고리/workstation/순서 |
| 변형 | NBT·amount·remainder·custom comparison·사용자 subclass |
| 화면 | 마법부여 적합 아이템·배타 목록·설명·페이지 이동 |
| 상태 | 재로드/연속 접속/취소/서버 변경/언어·팩·태그 변경 |
| 기존 기능 | 한국어, 전리품 현재 확률과 Shift 식, 배낭 레시피 채우기, 자동 업데이트 |
| 실행 ABI | MC1.20.1 Fabric, 실제 Gson2.10, 핀 불일치 fallback, 누적 클래스·helper 보존 |

구현 시 남길 최소 집계: generation ID, 플러그인별 등록 수, 필터 전후 수, native/새 경로 호출 수, 첫 색인/정렬 뷰의 별도 시간, snapshot 수/바이트 추정, fallback 수, suffix 외 문서 생성 시간. per-item 로그는 만들지 않는다.

이미 확보한 자료로 A~C를 구현·검증한 뒤 **통합해서 한 번** 같은 조건으로 측정한다. 실제 배포 전에는 앱 기동/서버 접속 테스트를 오프라인 검증과 구분한다. 기존 updater/서명 공개 절차를 사용하고 실행 중 설치 JAR을 바꾸지 않는다.

측정은 기존 BAT로180초 JFR+PresentMon/GPU, 기록 종료 뒤 heap dump, 클라이언트 종료 뒤 MAT. 다음 비교는 EMI 전체/단계 시간, long-frame tail, 일시 할당, GC 후 힙, 레시피·검색 기능을 같이 본다. 게임 설정·셰이더·집 위치·서버 데이터를 맞추고 최초 접속과 재접속을 섞지 않는다.

완료 판정은 단순히 로그에 enabled가 나온 것이 아니라 **동일 기능이 보존되고 총 작업량과 실제 초기화 시간이 함께 감소한 것**이다. A만으로 큰 개선이 안 나오면 캐시가 실패했다고 단정하지 말고 B~F의 남은 단계 비용을 본다.

## 14. 근거 파일과 원문

로컬 조사 폴더:

- `evidence-summary.json`: 실행 버전, 단계·플러그인 시간, 로그 집계, GC/할당 요약.
- `native-class-hashes.json`: 검토한 실제 native 클래스 SHA-256.
- `execution-filtered.json`, `allocation-filtered.json`: 이번 조사에서 EMI 두 스레드만 추린 JSON 데이터.
- `EmiTimeline.java`, `timed-samples.tsv`, `timed-allocations.tsv`, `emi-waits.tsv`: 원본 JFR의 시간대별 오프라인 추출.
- `Object_histogram3.csv`, `Largest_retained_own2.csv`: 기존 MAT 보고서 추출.
- `emi-mat.xml`, `Managers2.csv`, `Enchant_recipes3.csv`, `Suffix_arrays4.csv`, `mat-extra.log`: 기존 heap 인덱스에 대한 추가 질의. sorting-cache class 질의는 결과가 없어 근거로 쓰지 않았다.

원본 측정의 `analysis/*.tsv`, `capture.jfr`, `latest.log`, `state.json`, `frame-consistency.json`, `mat-uncompressed/heap_Client_Retention.zip`, 추가 `heap_EMI_Ownership.zip`을 함께 보면 재현할 수 있다. 원시 힙/로그에는 개인 데이터가 있을 수 있으므로 이 문서를 전달할 때 원시 덤프까지 GitHub에 올릴 필요는 없다.

Upstream와 설치 클래스 대조:

- [EMI1.1.24 /MC1.20.1 gradle 설정](https://github.com/emilyploszaj/emi/blob/23d251ea8ea3a5fd7d760948f36014b185eac69f/gradle.properties): 이 commit의 version/game을 이번에 다시 확인했다. Fabric 설치 JAR metadata도 별도 확인했다.
- [EMI 레시피 Manager/Worker](https://github.com/emilyploszaj/emi/blob/23d251ea8ea3a5fd7d760948f36014b185eac69f/xplat/src/main/java/dev/emi/emi/registry/EmiRecipes.java): 두 번 생성하는 흐름을 upstream와 실제 설치 바이트코드 양쪽에서 확인했다.
- [EMI Enchanting1.20.1 참고 소스](https://github.com/fzzyhmstrs/EMI_enchanting/blob/1da5c47aefb2fa0e11ab5b4cc65586422e7b7636/src/main/java/fzzyhmstrs/emi_enchanting/EnchantmentRecipe.java): 이전 검토에서 source metadata0.1.1과 설치0.1.2가 달랐으므로 이번 결론은 설치0.1.2 역컴파일을 기준으로 했다.
- [Fabric Create 참고 commit](https://github.com/Fabricators-of-Create/Create/tree/4c3fe779d135d592548e4cb09da04d742d2637b7): 이전 source-review 문서의 참고 경로다. 이번에는 설치6.0.8.1 build1744의 `addDeferredRecipes`를 직접 확인했으며 이 commit과 정확히 같은 빌드라고 주장하지 않는다.

이번 문서는 새로운 분석·구현 설계다. 과거 문서의 “효과 미측정”, “후보 미해결”과 실제 최신 enabled 여부를 구분했고, 제품 코드는 그대로 두었다.


## 15. 2026103008 구현 결과 — 첫 묶음

**구현 범위는 A와 C의 안전한 부분이다. B의 두 번째 전체 Manager 제거와 D/E/F는 아직 구현하지 않았다.** 이를 전체 설계 완료 또는 10초 목표 달성으로 표시하지 않는다.

- A: pinned Prominent SKILLET predicate에서 정확한 native ItemEmiStack과 양의 int 범위 수량은 Item identity로 비교한다. 0/음수/overflow/custom 스택은 원본 predicate를 실행한다.
- C: native EmiAnvilEnchantRecipe의 입력/출력 준비에서 getDefaultStack을 Item 기본 구현 그대로 쓰고 실제 반환값에 NBT가 없는 일반 도구만 범위 내 template을 재사용한다. Book도 그 도구의 native getBook 결과를 private copy로 보관해 재사용하므로 원래 첫 인챈트 선택 의미를 바꾸지 않는다.
- caller에는 항상 별도 stack/NBT copy를 반환한다. 기본 스택을 오버라이드한 도구와 생성자 NBT가 있는 도구는 원본 계산을 유지한다. 실제 화면 밖으로 캐시 수명을 늘리지 않으며 UI의 addWidgets/getTool은 그대로다.
- recipe bake와 sorted-worker는 각각 독립 ThreadLocal scope다. 정상/예외/nested scope 종료 때 참조를 지운다. 최대 item8192/book65536 entries이며 world/player 참조를 보관하지 않는다. 책 template은 처리 중 일시 보유량을 늘릴 수 있으므로 다음 MAT에서 피크와 종료 후 해제를 함께 평가한다.
- 로그 `EMI stack preparation: hiddenItem=enabled`, `anvilPreparation=enabled` 및 각 단계 끝 `EMI native stack preparation`의 itemHits/bookHits/fallbacks로 실제 적용 범위를 확인한다. opt-out은 `-Dprominence.emiPreparation.hiddenItem=false`, `-Dprominence.emiPreparation.anvilPreparation=false`이다.

검증 도구 `tools/verify_emi_preparation.py`는 실제 설치된 native 4클래스에 기존 recipe-index wrapper와 새 Mixin을 함께 적용해54메서드를 ASM으로 검증한다. native 모루 getter/getBook/getTool과 숨김 predicate의 메서드 본문을 실행하되 Minecraft 아이템/NBT 경계는 별도 data facade다. 실제 게임/렌더러는 실행하지 않았다.

검사2056개: 반복 출력/NBT 동일, 반환 객체 변이 격리, 고유 기본 인챈트 순서, 생성자 NBT fallback, 범위 종료/예외/중첩/다른 스레드 격리, 수량 경계와 custom 스택. 반복 모루 사례의 Minecraft stack 생성은4000→6, 숨김 검사10000→0이었다. fixture 감소 배수를 실제 전체 로딩 개선량으로 환산하지 않는다. native 버전/해시 guard10시나리오, 현재 실제 resource fingerprint2그룹, 누적639클래스의 실제 Gson2.10 멤버44연결 및 전체 기존 회귀 검사를 통과했다.

이전632클래스와 updater helper는 byte-identical이며 새7클래스가 추가됐다. 준비된 파일은 `Compat/build/reviewed3008/prominence-korean-compat-1.0.0.jar`다. 실사용 성능 검증은 다음 같은 조건의 측정에서 한다. B는 custom getter/비교 전략/순서 및 worker 취소를 함께 다루는 별도 구조 변경으로 남아 있다.
