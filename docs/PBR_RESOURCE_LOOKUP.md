# 첫 접속 PBR 리소스 조회 비용

20261030-090519-b15424는 nightly2026103003이며 JFR·프레임·GPU·사후 heap/MAT가 완료됐다. 현재 로그는 저장 로그를 포함하고 정상 종료 기록이 추가됐다. 저장 ERROR29/WARN933, 종료 후 ERROR29/WARN938이다. 두 번 모두 같은 Bliss ZIP을 사용했지만 방문 순서·driver cache 상태가 통제된 A/B는 아니다.

| 구간 | Iris 준비 | 첫 렌더 재질표 | 긴 프레임 |
|---|---:|---:|---:|
| 첫 입장 | 507ms | 126ms | 2,995ms |
| 엔드 | 1,173ms | 197ms | 1,990ms |
| 네더 | 737ms | 92ms | 1,186ms |
| 오버월드 복귀 | 487ms | 127ms | 1,297ms |

첫 입장 관측 start gap8,430ms와 보고된 frame time2,995ms가 불일치하므로 정확한 단일 프리즈 길이나 개선률로 쓰지 않는다. 이전 capture의438/637/1717/418ms와 비교해 일관된 개선은 입증되지 않았다. materialPredicates는 활성화됐고 호출당 정수 조건 후보38,432→5,054 / 이미 배정한 상태103개를 생략했다. 이 작업은 실행됐지만 전체 끊김의 주원인까지 해결한 것은 아니다.

JFR Render thread에는 `PBRTextureManager.getOrLoadHolder → AtlasPBRLoader.load/createPBRSprite → ResourceManager.getResource → ModNioResourcePack.getFilename/getPath`가 첫 입장 때 기록됐다. PBR 관련68표본, 재질표47표본이다. 표본 수는 소요 ms가 아니다. 09:05:26~27의 리소스 조회, 이어서 registry/state 갱신, Flywheel 작업 대기·Colorwheel mesh 준비가 겹친다.

EMI18,118ms 중 plugin6,309 / index2,281 / late1,741 / recipe2,745 / search4,186ms였다. 이는 단일 렌더 정지18초를 뜻하지 않는다. RAM GC후 최고5,842,665,472 / 최대6,341,787,648B(92.1%), GC총1,396ms / 최대74ms / trace중 Full0회다. 수초 정지를 GC만으로 설명하지 않는다. MAT의 Voxy WorldEngine538.7MB·ModelFactory363.2MB는 별도 보유 객체이며 이 수치만으로 누수라고 판단하지 않는다. ItemEmiStack254만개·ItemStack191만개도 보이나 histogram 개수만으로 소유자나 제거 안전성을 단정하지 않는다.

## 좁은 수정

Fabric resource-loader0.11.12+fb82e9d777와 Minecraft1.20.1의 실제 native bytecode에서 resource filename은 `String.format(Locale.ROOT,"%s/%s/%s",type,namespace,path)`였다. 세 인수는 이미 문자열이다. 이 정확한 호출에서만 같은 경로를 문자열 연결로 만들고 포맷터·포맷 해석 객체를 없앤다.

- Locale.ROOT / 정확한 포맷 / 인수3개 / 문자열 또는null일 때만 대체한다. 다른 입력·사용자 정의 Formattable은 원래 함수를 호출한다.
- 검색 순서·파일 존재 확인·예외·리소스팩 우선순위·리로드는 그대로다. 리소스 캐시, 별도 thread, GPU/셰이더 품질 변경은 없다.
- 두 클래스의 exact SHA256/버전이 맞을 때만 client JoinGuard resourceNames가 적용한다. `-Dprominence.join.resourceNames=false`로 제외할 수 있다.
- fixture는 native String.format과2,197개 경로 조합의 결과를 비교하며 null·Unicode·구분자·포맷 문자·예상 외 입력 fallback을 검사한다. 실제 native 5타깃83메서드 Mixin/ASM 변환과 누적 회귀 검사도 수행한다. 게임 가속률을 뜻하지 않는다.

## 남은 문제

Bliss shaders.properties661행의 `isInPaleGardenBiomes=in(biome,BIOME_PALE_GARDEN)`은 설치 Iris1.7.6에서 알 수 없는 변수 경고를5회 발생시킨다. 셰이더 SHA256은94fe5abd6f8963d2a3b1aff96af26a1c22c2dc184574493b0afcd4d9f78e6c20이다. 이 정의의 호환 문제는 확인했지만 이번 path 수정과 별개이며 경고를 필터로 숨기지 않았다. pipeline 준비·전처리·GL 작업, PBR 파일 조회 자체, dynamic model bake 대기, EMI recipe generation은 남아 있다.

지금 수정은 반복 경로 포맷의 불필요한 비용만 제거한다. 수초 끊김 전체 해소나 실제 프레임 감소량은 아직 입증되지 않았다. 설치본·서버·셰이더 파일·개인 설정은 직접 수정하지 않고 nightly JAR로 배포한다.

## 후속 측정: 20261030-115641-6db6ea / build2026103004

서명된 build3004의 SHA256과 resourceNames=enabled를 확인했다. JFR·PresentMon·GPU·사후 heap/MAT가 완료됐고 이번 frame-consistency에는 불일치가 없다. 저장 로그 ERROR29/WARN955, 종료 후 현재 로그 ERROR31/WARN978이다.

| 구간 | 긴 프레임 | Iris 준비 | 첫 렌더 재질표 |
|---|---:|---:|---:|
| 첫 입장 | 2,865ms + 4,368ms | 386ms | 162ms |
| 엔드 첫 방문 | 1,929ms | 1,221ms | 139ms |
| 네더 | 1,237ms | 807ms | 89ms |
| 엔드 재방문 | 809ms | 376ms | 97ms |
| 오버월드 복귀 | 1,067ms | 423ms | 90ms |

첫 두 프레임은 UTC02:56:52.564~55.429, 55.429~59.797이다. GPU busy는 각각123/212ms이며 GPU wait는2,748/4,152ms였다. GPU 사용률만으로 원인을 판단하지 않는다. 이전 capture 첫 프레임의 불일치와 방문 순서 차이 때문에 수치를 단순 개선률로 계산하지 않는다.

첫 구간 Render 표본142개 중 PBR39 / registry sync27 / shader transform18이 관측됐다. 다음4.37초 구간177표본에는 EMI creative41 / tag sync33 / Voxy mapper load9 등이 있었다. 이들은 겹칠 수 있는 call-stack 집계이며 시간 비율이 아니다. 첫 구간 이후에도 main-thread 초기화가 이어진다는 증거다. 구체적으로 Create의 모델 분류가 lazy baking을 유발했고 tab195.6ms, AE2 덮개 tab405.2ms가 기록됐다. 이 두 값만으로4.37초 전체를 설명하지 않는다. 태그 적용 뒤 block-state cache 갱신, 스킬 배경 텍스처 준비, Voxy 매핑 로드도 같은 구간에 있다.

ResourceFilename 실행과 native Formatter 경로 축소를 확인했지만 PBR 조회 자체가 남아 있다. EMI 전체18,034ms는 직전18,118ms와 비슷하다. index1,702 / plugin7,241 / recipe2,626 / search4,178ms다. 현재 근거로 큰 체감 개선을 주장하지 않는다.

엔드 재방문과 오버월드 복귀에도 TransformPatcher.patchVanilla/patchComposite의 AST 파싱·변환이 실행됐다. 차원 재방문은 최초보다 짧지만 재구축 비용이 남는다. 같은 셰이더의 native 결과 캐시가 이미 존재하므로 다음 조사는 재방문 입력·파라미터와 miss 경로를 확인하는 쪽이다. 첫 입장은 creative/tab·태그 동기 초기화의 누적을 우선 조사한다. Create195ms 한 경로만 고쳐 전체 수초를 해결했다고 보지 않는다.

GC총1,561ms / 최장71ms / Full0회, GC후 최고5,632,950,272 / 최대6,341,787,648B(88.8%)다. MAT Voxy WorldEngine538.7MB / ModelFactory361.0MB, ItemEmiStack약254만개다. 메모리 보유 양상은 크게 달라지지 않았고 수초 정지를 FullGC로 설명할 수 없다.

현재 로그의 Xaero minecraft:item variant ID 클래스 불일치(MIIdentifier)는11:59:57로 trace 종료11:59:43 뒤다. Azure armor texture 누락도12:02:04로 이후다. 실제 별도 오류로 남기되 이번 첫 접속 프레임의 원인에 합산하지 않는다. 이 후속 기록에서는 분석만 갱신했고 새 JAR이나 운영 파일을 변경하지 않았다.
