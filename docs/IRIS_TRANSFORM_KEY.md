# Iris 변환 결과 캐시 키 안정화

대상은 Fabric Minecraft1.20.1 / Iris1.7.6+mc1.20.1이다. `20261030-115641-6db6ea`의 JFR에서는 엔드 재방문·오버월드 복귀에도 `TransformPatcher.patchVanilla/patchComposite`의 파싱·AST 변환이 관측됐다. 긴 프레임은 각각809/1,067ms였고, 첫 접속에는2,865/4,368ms가 남았다. GC 최장71ms로 이 수초 정지를 설명할 수 없다. 서로 다른 원인이 겹친 전체 프레임 시간이며 전부 Iris 비용이라고 보지 않는다.

## 확인한 결함

설치 JAR의 `TransformPatcher$CacheKey`는 입력 `Parameters`를 그대로 보관한다. 조회 시 `type`은null이지만 AST 작업이 같은 객체의 `type`을FRAGMENT/COMPUTE로 바꾸며, 그 뒤 native LRU에 넣는다. 다음 요청은 다시null로 시작해서 동일 입력의 기존 결과와 일치하지 않는다. `Parameters.hashCode/equals`에type이 포함돼 있다.

기존 사후 힙을 MAT로 재조회했을 때 보관된 키116개 모두type=FRAGMENT였다. Vanilla82 / Composite25 / Sodium9개이며 textureMap은 모두 native Object2ObjectOpenHashMap, size0이었다. 이 사실만으로 소요 시간을 계산하지는 않는다.

[Iris 1.20.1 브랜치의 Parameters](https://github.com/IrisShaders/Iris/blob/f8d7bea2ab82d21737e7aa8e4b2fb3a429078106/src/main/java/net/irisshaders/iris/pipeline/transform/parameter/Parameters.java)와 [TransformPatcher](https://github.com/IrisShaders/Iris/blob/f8d7bea2ab82d21737e7aa8e4b2fb3a429078106/src/main/java/net/irisshaders/iris/pipeline/transform/TransformPatcher.java)에서도 같은 흐름을 확인했다. 브랜치 commit이 설치 빌드와 동일하다고 가정하지 않고 실제 JAR의 bytecode와12개 클래스 SHA256을 기준으로 검증한다.

## 변경과 보존

두 native CacheKey 생성자에서만 Parameters의 입력 스냅샷을 보관한다. 원래 작업 객체·AST 변환·stage 처리에는 손대지 않는다. 정확히 확인한 Vanilla/Sodium/Composite/Compute/DH 클래스만 복사하며, 입력 flags·alpha·attribute inputs·texture map·초기type을 보존한다. 이름은 native 규칙대로 키 비교 대상이 아니다. texture map은 native 데이터 맵만 복사하고 낯선 하위 클래스·맵·값에는 원래 참조를 사용한다.

기존400항목 LRU, 소스 문자열 비교, 결과 맵, GL 컴파일, 셰이더 품질·설정, pipeline 파기, 리소스 리로드와 이전 parser scratch 해제 정책은 유지한다. 디스크 캐시·백그라운드 변환·매 프레임 폴링을 추가하지 않는다. snapshot과 작은 texture map 복사 비용은 요청마다 발생하며 native LRU 범위 안에서만 보관된다. 이번 측정의 map은 모두 비어 있었다.

12개 클래스/게임/Iris 버전이 다르면 Mixin을 적용하지 않는다. `-Dprominence.shaderTransformKey=false`로 제외할 수 있다. 클라이언트만 필요한 변경이다.

## 검증과 한계

- 실제 native CacheKey의 두 생성자에 Mixin을 적용하고5메서드를 ASM 검사했다.
- native Iris 변환과 native LRU를 그대로 실행했다. 유효한40입력의 첫 변환은 양쪽40회, 두 번의 반복 요청은 원본80회 / 수정0회였다. GLSL 결과120개가 같았다. Vanilla/Sodium/Composite/DH/Compute를 포함한다.
- native Parameters의 격리·맵 변경·flag 구분·알 수 없는 입력 fallback·fingerprint·opt-out154검사를 통과했다.
- 테스트는 메모리 내 IrisConfig와 짧은 GLSL fixture를 쓴다. 게임·월드·GL context를 시작하지 않으며 fixture 실행 시간을 실제 팩 가속률로 바꾸지 않는다.

기존 측정은 수정 전이다. 같은 입력의 재변환 제거는 재현했지만 실제 게임의 프레임 감소량은 아직 측정하지 않았다. 첫 방문의 최초 변환·드라이버 작업·PBR 조회와 첫 접속 EMI creative/tab·태그/registry 초기화는 남는다. 이전 캐시·품질·기능을 무작정 끄지 않고 확인된 중복 작업만 제거했다.
