@echo off
echo Debug Test Script
echo.

REM Test 1: Check environment variables
echo Test 1: Check environment variables
if "%GITHUB_TOKEN%"=="" (
    echo GITHUB_TOKEN not set
) else (
    echo GITHUB_TOKEN is set
)

if "%aliQwen_api%"=="" (
    echo aliQwen_api not set
) else (
    echo aliQwen_api is set
)
echo.

REM Test 2: Check application
echo Test 2: Check if application is running
curl -s http://localhost:8080/api/webhook/github/health
echo.
echo.

REM Test 3: Get PR info
echo Test 3: Get PR information
if not "%GITHUB_TOKEN%"=="" (
    curl -s -H "Authorization: Bearer %GITHUB_TOKEN%" "https://api.github.com/repos/ottoottoadwa/qiniu-contest-project/pulls?state=open&per_page=1"
    echo.
) else (
    echo Skipped - GITHUB_TOKEN not set
)
echo.

echo Test completed!
echo.
pause
