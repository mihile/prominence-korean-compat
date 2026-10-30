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
