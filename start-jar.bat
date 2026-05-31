@echo off
REM PR Review Application Startup Script (JAR mode)
REM 请先配置以下环境变量

echo ========================================
echo PR Review Application Startup (JAR)
echo ========================================
echo.

REM 检查环境变量
if "%aliQwen_api%"=="" (
    echo [ERROR] aliQwen_api is not set!
    echo Please set: set aliQwen_api=your_api_key
    pause
    exit /b 1
)

if "%GITHUB_TOKEN%"=="" (
    echo [WARN] GITHUB_TOKEN is not set - GitHub features will be limited
)

echo [OK] Environment variables configured
echo.
echo Starting application with HTTP timeouts...
echo.

cd prreview-web\target
java -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -jar prreview-web-0.1.0-SNAPSHOT.jar
