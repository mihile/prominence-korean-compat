# 새 업데이트 배포

모드 버전과 설치 파일명은 `1.0.0`으로 유지합니다. 새 바이너리를 공개할 때마다 내부 `build`를 증가시킵니다. `2026102501`처럼 날짜+순번을 사용할 수 있습니다. 기존과 같거나 낮은 번호는 클라이언트가 제안하지 않습니다.

## 준비

- 수정·검증한 **최신 전체 클라이언트 호환 JAR**을 base로 사용합니다. 오래된 ZIP에서 누락 모드나 이전 클래스를 복원하지 않습니다.
- JDK17 이상, Python3, GitHub CLI가 필요합니다. 서버용 바이너리는 이 자동 배포에 섞지 않습니다.
- 컴파일 classpath JSON 배열에 현재 Minecraft intermediary JAR, Fabric Loader/API의 networking/lifecycle/screen/client-command 모듈과 기존 의존성을 넣습니다. 경로가 들어간 개인 classpath 파일은 커밋하지 않습니다.
- Ed25519 개인키는 저장소 밖에 보관합니다. `public-key.der`와 짝을 이루는 키를 계속 사용합니다. 개인키·개인 설정·토큰·월드·로그를 업로드하지 않습니다.

## 빌드

```text
python tools/build.py --base base-client.jar --classpath classpath.json --java-home JAVA_HOME --build 2026102502 --label 2026.10.25-2 --output build
```

기존 클래스/믹신/진입점과 최신 누적 수정 보존, 네이티브 버전 guard 및 필요한 실제 동작을 확인합니다. `tests/UpdaterTests.java`는 별도 임시 파일과 짧은 Java 프로세스만 사용하며 게임을 실행하지 않습니다. 새 UI와 모드 기능의 실제 게임 검수는 별도로 수행합니다.

## 서명과 릴리스

`changes.json`에 `tag`, `title`, `notes` 배열을 작성합니다. `notes`는 게임 화면에 즉시 보이는 짧은 문장입니다. GitHub용 Markdown 내역은 별도 파일에 쓰며 상세 항목을 `<details>`로 접습니다.

```text
python tools/publish.py --build-dir build --changes changes.json --signing-key PRIVATE_KEY_OUTSIDE_REPO --java-home JAVA_HOME --notes-file release.md --prepare-only
```

준비된 JAR·서명·테스트를 검토하고 소스를 커밋·푸시한 뒤 `--prepare-only`를 빼고 실행합니다. 생성된 릴리스의 target은 전체40자리 commit SHA입니다. stable 채널은 GitHub의 latest **정식 릴리스**를 사용하므로 draft/prerelease는 자동 제안하지 않습니다.

배포 후 릴리스 파일과 `SHA256SUMS.txt`를 다시 내려받고, `VerifyRelease`로 실제 공개 피드의 서명·파일을 재검증합니다. 동시에 기존 이름의 클라이언트 배포 ZIP과 최신 preparedSource도 갱신합니다. 실행 중 설치 금지와 별도 서버 승인 규칙은 유지합니다.

## 실패·복구

다운로드나 서명 검증 실패는 기존 JAR을 바꾸지 않습니다. 교체 전 설치 SHA가 달라졌으면 수동 업데이트를 보존하고 중단합니다. 적용 결과는 `.prominence-update/last-update.properties`, 자세한 로컬 로그는 `apply.log`, 백업은 `backups/`에 있습니다. 실패 시 다음 접속에서 `/kpatch update`로 다시 준비할 수 있습니다. 강제 종료나 외부 프로세스 종료 도구가 도우미까지 죽인 경우에는 다시 준비하거나 백업을 수동 복구합니다.

이 프로토콜은 JAR 한 개만 교체합니다. 리소스팩·퀘스트·개인 설정을 업데이트 대상으로 조용히 확장하지 않습니다. 지원 팩 버전 변경이나 서명키 교체는 별도의 호환·배포 계획이 필요합니다.
