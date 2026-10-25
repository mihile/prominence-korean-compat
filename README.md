# Prominence II 한국어 호환 패치

Prominence II **4.1.3 · Minecraft 1.20.1 · Fabric**용 누적 호환 패치입니다.

**처음 한 번 설치하면, 다음 업데이트부터 게임에서 변경 내역을 확인하고 적용할 수 있습니다.** 파일명과 모드 버전은 `1.0.0`을 유지하며 빌드 번호로 업데이트를 구분합니다.

[최신 다운로드](https://github.com/mihile/prominence-korean-compat/releases/latest) · [업데이트 내역](https://github.com/mihile/prominence-korean-compat/releases) · [문제 제보](https://github.com/mihile/prominence-korean-compat/issues)

## 설치와 업데이트

1. 게임을 종료합니다.
2. 릴리스의 `prominence-korean-compat-1.0.0.jar`을 인스턴스의 `minecraft/mods`에 덮어씁니다.
3. 다음부터 게임을 켜면 업데이트를 확인하고, 로딩이 끝난 **메인 메뉴에서** 변경 내역과 **업데이트 / 나중에 / GitHub** 버튼이 표시됩니다.

업데이트를 선택하면 다운로드와 검증을 진행합니다. **게임을 종료한 뒤 교체하며, 다음 실행부터 적용됩니다.** 다운로드 후 계속 플레이해도 됩니다. 인터넷 연결이 없어도 기존 패치는 그대로 작동합니다.

메인 메뉴 우측 위의 **패치 업데이트** 버튼으로 언제든 다시 확인할 수 있습니다. 월드 입장이나 명령어 입력은 필요하지 않습니다. 확인 중·연결 실패·다시 확인도 같은 화면에서 처리합니다.

- `/kpatch update` — 월드 안에서 사용할 수 있는 추가 확인 방법
- `/kpatch auto off` / `/kpatch auto on` — 자동 확인 끄기/켜기

자동 업데이트 범위는 **클라이언트 호환 JAR 한 개**입니다. 서버용 JAR, 리소스팩, 퀘스트 파일과 다른 모드는 별도 배포를 사용합니다. 서버 판정이 필요한 기능은 서버에도 해당 서버용 패치가 적용돼 있어야 합니다. 이 클라이언트 JAR을 전용 서버에 넣지 마세요.

## 포함된 기능

<details>
<summary>한국어 안내와 검색</summary>

고정 문구·책·퀘스트·툴팁 표시 보완, 실제 조작키 안내, 탐험가의 나침반 한국어·영어 검색, EMI 획득 경로 안내를 포함합니다. 별도 번역 리소스팩/퀘스트 파일이 필요한 변경도 있습니다.

</details>

<details>
<summary>장비와 제작 호환</summary>

화살통과 화살 스킬, 대장장이 장비 정보 보존, 정교한 배낭 대장장이 EMI 전송·모루 비용 표시·재단조 연결 등을 보완합니다. 멀티플레이의 제작·전투 판정 보완에는 대응 서버용 JAR이 필요합니다.

</details>

<details>
<summary>화면과 실행 보완</summary>

탈것 시야 보완, UI/폰트 호환 및 기존 EMI 초기화·조회 보완을 유지합니다. Iris 셰이더 준비 후 분석용 중간 캐시를 정리하고, 배낭 이벤트가 이전 월드를 계속 붙잡는 참조도 보완합니다. 컴파일된 셰이더와 재방문용 결과 캐시는 보존합니다. Voxy/VoxyServer 같은 별도 모드의 업데이트는 이 JAR의 자동 업데이트 대상이 아닙니다.

</details>

<details>
<summary>자동 업데이트의 검증과 백업</summary>

서명된 배포 정보와 파일의 SHA-256·크기·클라이언트 빌드를 확인합니다. 승인 없이 파일을 바꾸지 않으며, 실행 중인 게임 JAR을 교체하지 않습니다. 기존 JAR은 `minecraft/.prominence-update/backups`에 남습니다. 설정·월드·Prism 실행 명령은 건드리지 않습니다.

</details>

## 개발과 배포

이 저장소의 `src`에는 자동 업데이트와 클라이언트 메모리 참조 수명 보완 모듈이 있습니다. 누적 호환 JAR에 기존 기능을 보존한 채 통합하며, 기존 소스·라이선스·출처는 배포 JAR 내부에도 유지합니다.

[관리자 빌드·배포 절차](docs/MAINTAINER.md) · [검증 범위](docs/VALIDATION.md)
