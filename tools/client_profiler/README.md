# 클라이언트 프로파일링

인스턴스 루트의 Profile_Client.bat을 실행한다.

- Enter / 1: 3분 측정 또는 진행 중인 작업에 다시 연결
- 2: 최근 자료의 JFR·MAT 분석 이어하기 (다시 측정하거나 힙을 덤프하지 않음)
- 3: 최근 결과 폴더 열기
- 4: 최근 결과 폴더 경로 복사

서버 목록에서 1번을 선택하고 **측정 시작** 안내 후 접속한다. JFR·프레임·GPU·CPU/메모리를 기록한 뒤 힙을 저장한다. 덤프 중에는 Full GC로 잠시 멈출 수 있다. **힙 덤프 저장 완료** 안내가 나오면 게임을 종료한다. 이 창은 **MAT 분석 완료**까지 열어 둔다. 남은 시간·덤프 크기·MAT 진행 상황이 표시된다.

기본은 180초다. 2분만 기록하려면 `Profile_Client.bat run --seconds 120`을 실행한다. 서버용 도구는 기록만 저장하고 MAT를 실행하지 않으므로 완료 시간이 더 짧다.

결과는 Temp/prominence-profile 아래에 저장한다. README.txt는 파일 설명, state.json은 실제 완료/실패 상태, log-summary.json은 측정 구간 오류 요약이다. 프레임 수집 실패는 완료 화면에서도 별도로 안내한다.

settings.local.json은 개인 도구 경로, policy.json은 측정 정책, latest.json은 최근 결과 위치다. .cache/summary는 재사용하는 분석기 컴파일 캐시다. 이 폴더는 배포 복사에서 제외한다. 소스와 BAT 템플릿은 KoreanPatch/Compat/repository/tools/client_profiler에 함께 관리한다.
