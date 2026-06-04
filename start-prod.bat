@echo off
echo ========================================
echo PR Review - Production Mode
echo ========================================
echo.

REM Check if JAR exists
if not exist "prreview-web\target\prreview-web-0.1.0-SNAPSHOT.jar" (
    echo JAR file not found. Building project...
    echo.
    call mvn clean package -DskipTests
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
)

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
echo Starting application in production mode...
echo - Analyze all files in PR
echo - Generate detailed suggestions
echo - Full risk analysis
echo.
echo Webhook endpoint: http://localhost:8080/api/webhook/github
echo.

REM Start with prod profile
java -jar prreview-web\target\prreview-web-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod

pause

