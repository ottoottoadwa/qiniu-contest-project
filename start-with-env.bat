@echo off
REM PR Review Application Startup Script
REM 请先配置以下环境变量

echo ========================================
echo PR Review Application Startup
echo ========================================
echo.

REM 检查环境变量
if "%aliQwen_api%"=="" (
    echo [ERROR] aliQwen_api is not set!
    echo Please set: set aliQwen_api=your_api_key
    echo Get it from: https://dashscope.console.aliyun.com/apiKey
    echo.
    pause
    exit /b 1
)

if "%GITHUB_TOKEN%"=="" (
    echo [ERROR] GITHUB_TOKEN is not set!
    echo Please set: set GITHUB_TOKEN=your_token
    echo Get it from: https://github.com/settings/tokens
    echo.
    pause
    exit /b 1
)

echo [OK] Environment variables configured
echo.
echo Starting application with HTTP timeouts...
echo.

cd prreview-web
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000"
