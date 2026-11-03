@echo off
setlocal
chcp 65001 >nul
set "PYTHONUTF8=1"
cd /d "%~dp0"
title Prominence - Client Profiler
where python >nul 2>nul
if errorlevel 1 (
  echo Python 3을 찾을 수 없습니다. Python 설치 또는 PATH를 확인해 주세요.
  pause
  exit /b 1
)
echo.
echo ==== Prominence 클라이언트 성능 측정 ====
echo JFR + 프레임/GPU + 힙 덤프 + MAT 분석
echo.
if "%~1"=="" (
  python -u "%~dp0ClientProfiler\profile.py" menu --instance "%~dp0."
) else (
  python -u "%~dp0ClientProfiler\profile.py" %* --instance "%~dp0."
)
set "PROFILE_RESULT=%ERRORLEVEL%"
echo.
if not "%PROFILE_RESULT%"=="0" echo 완료되지 않았습니다. 위 안내를 확인하세요. 저장이 끝난 자료는 2번으로 분석을 이어갈 수 있습니다.
pause
exit /b %PROFILE_RESULT%
