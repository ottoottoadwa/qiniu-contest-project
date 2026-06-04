@echo off
echo ========================================
echo PR Review - Fast Demo Mode
echo ========================================
echo.

REM Check environment variables
if "%aliQwen_api%"=="" (
    echo [WARNING] aliQwen_api not set - AI features will fail
) else (
    echo [OK] aliQwen_api configured
)

if "%GITHUB_TOKEN%"=="" (
    echo [WARNING] GITHUB_TOKEN not set - GitHub API will fail
) else (
    echo [OK] GITHUB_TOKEN configured
)

echo.
echo Starting application in fast demo mode...
echo - Max 3 files analyzed
echo - Skip detailed suggestions
echo - Faster review speed
echo.
echo Webhook endpoint: http://localhost:8080/api/webhook/github
echo.

REM Start with demo profile
java -jar prreview-web\target\prreview-web-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo
