# 유지보수와 로컬 정리 기준

2026-10-28 사용자 결정: 배포 기준은 누적 통합 JAR과 별도 설치 모드다. 별도 모드는 설치 목록으로 안내한다. 번역을 위해 과거 작업 폴더와 배포 ZIP을 계속 개인 PC에 보관할 필요가 없도록 GitHub에 원본·용어·변경 이유를 관리한다. 추가 지도 아이콘은 기본 번역 설치 요건이 아니다. 이미 내장된 아이콘을 이번 결정으로 제거한 것은 아니다.

## 수정할 기준 경로

- `translation/Prominence_II_Korean_Translate.zip`: 책·언어·기타 번역 리소스 원본.
- `src/main/resources/assets/prominence_korean_compat/translation_overrides/ko_kr.json`: 검수한 언어 정정.
- `translation/glossary/current-terms.json`: 현행 용어의 작은 시작 목록. 게임에서 읽는 번역 파일은 아니다.
- `translation/glossary/`: 과거 대조 기록과 퀘스트 미대응 검수 자료.
- `src/main/resources/assets/prominence_korean_compat/quest_reviewed_text.json`: 퀘스트 표시 번역.
- `src/`, `tools/`, `docs/`: 구현·빌드·검증·수정 의도와 근거.

번역 수정은 용어집만 고치고 끝내지 않는다. 대응하는 실제 언어 키·책·퀘스트 매핑도 함께 고치고, 영향 키와 이유를 커밋 및 릴리스 설명에 적는다. 누적 JAR을 검증하여 nightly에 게시하고, 실사용 검증 후 main으로 병합한다.

## 이미 공개된 범위와 남은 확인

`nightly-20261028.1`에 10,999 리소스 파일을 포함한 클라이언트 JAR을 공개했다. 단순히 같은 내용의 JAR을 다시 빌드하거나 새 버전처럼 게시할 필요는 없다. 퀘스트 원문 충돌 14개와 설명 줄 수 불일치 78개는 검수 대상으로 남아 있다. 외부 퀘스트·스킬 설정까지 모두 JAR만으로 대체된 것으로 판단하지 않는다. `docs/TRANSLATION_UPDATES.md`를 따른다.

## 로컬 보관

이 저장소의 소스·현재 작업 변경과 필요한 개발 설정을 유지한다. 공개 완료된 작업마다 별도 payload/backup/ZIP 사본을 장기 보관하지 않는다. 과거 작업별 백업을 유지할 필요가 없다는 사용자 결정을 반영한다. 설치 중 실패 복구를 위한 updater의 짧은 수명 백업은 기능상 별개다.

과거 작업 폴더를 통째로 삭제하기 전에는 아직 저장소나 공개 자료에 없는 소스·검수 근거가 있는지 대조한다. 개인키·토큰·개인 설정·실제 월드·원시 프로파일은 GitHub에 올리지 않는다. GitHub에 올렸다는 사실은 추적되지 않은 로컬 변경까지 보존됐다는 뜻이 아니다.
