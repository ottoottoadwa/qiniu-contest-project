@echo off
echo ========================================
echo PR Review Bot - Auto Review Test
echo ========================================
echo.

REM Check environment variables
if "%GITHUB_TOKEN%"=="" (
    echo [ERROR] GITHUB_TOKEN not set!
    echo.
    echo Please run: set GITHUB_TOKEN=your_token
    echo.
    pause
    exit /b 1
)

if "%aliQwen_api%"=="" (
    echo [ERROR] aliQwen_api not set!
    echo.
    echo Please run: set aliQwen_api=your_api_key
    echo.
    pause
    exit /b 1
)

echo [OK] Environment variables configured
echo.

REM Check if application is running
echo Checking if application is running...
curl -s http://localhost:8080/api/webhook/github/health > health.txt 2>&1
findstr /C:"ok" health.txt > nul
if errorlevel 1 (
    echo [ERROR] Application not running!
    echo.
    echo Please start: start-demo.bat
    echo.
    del health.txt
    pause
    exit /b 1
)
echo [OK] Application is running
del health.txt
echo.

REM Get latest PR
echo Fetching latest PR from GitHub...
curl -s -H "Authorization: Bearer %GITHUB_TOKEN%" "https://api.github.com/repos/ottoottoadwa/qiniu-contest-project/pulls?state=open&per_page=1" > pr.json

REM Check file
for %%A in (pr.json) do set size=%%~zA
if %size% LSS 10 (
    echo [ERROR] Cannot fetch PR
    del pr.json
    pause
    exit /b 1
)

REM Extract PR number
for /f "tokens=2 delims=:, " %%a in ('findstr /C:"\"number\"" pr.json') do (
    set PR_NUM=%%a
    goto found
)

:found
set PR_NUM=%PR_NUM:"=%
set PR_NUM=%PR_NUM: =%

if "%PR_NUM%"=="" (
    echo [ERROR] No open PR found!
    echo Please create a PR first
    del pr.json
    pause
    exit /b 1
)

echo [OK] Found PR #%PR_NUM%
del pr.json
echo.

REM Create pull_request event payload (simulates PR opened)
echo Creating pull_request event payload...
(
echo {
echo   "action": "opened",
echo   "pull_request": {
echo     "number": %PR_NUM%,
echo     "title": "Test PR",
echo     "state": "open"
echo   },
echo   "repository": {
echo     "full_name": "ottoottoadwa/qiniu-contest-project"
echo   }
echo }
) > payload.json

echo [OK] Payload created
echo.

REM Send pull_request webhook
echo Simulating PR opened event for PR #%PR_NUM%...
echo This will trigger automatic review!
echo.

curl -X POST http://localhost:8080/api/webhook/github -H "Content-Type: application/json" -H "X-GitHub-Event: pull_request" -d @payload.json

echo.
echo.
echo ========================================
echo Auto-review triggered!
echo.
echo View PR: https://github.com/ottoottoadwa/qiniu-contest-project/pull/%PR_NUM%
echo.
echo Check application logs for progress
echo Bot will automatically post review in 30s-1min
echo ========================================
echo.

if exist payload.json del payload.json

pause
