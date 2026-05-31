@echo off
echo ========================================
echo Rebuilding and Restarting Application
echo ========================================
echo.

REM Check environment variables
if "%aliQwen_api%"=="" (
    echo [ERROR] aliQwen_api is not set!
    echo Please set: set aliQwen_api=your_api_key
    pause
    exit /b 1
)
echo [OK] Environment variable aliQwen_api is set
echo.

echo [1/4] Compiling...
cd C:\Users\otto_\Desktop\qiniu\prreview
call mvn clean compile -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [OK] Compilation successful
echo.

echo [2/4] Finding old process...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do set PID=%%a
if defined PID (
    echo [OK] Found process PID: %PID%
    echo [3/4] Stopping old process...
    taskkill /PID %PID% /F >nul 2>&1
    timeout /t 3 /nobreak >nul
    echo [OK] Process stopped
) else (
    echo [WARN] No process found on port 8080
)
echo.

echo [4/4] Starting application...
cd prreview-web
start "PR Review App" mvn spring-boot:run
echo [OK] Application starting in background...
echo.
echo Waiting for startup (20 seconds)...
timeout /t 20 /nobreak >nul
echo.
echo ========================================
echo Application should be ready!
echo Test with: curl -X POST http://localhost:8080/api/reviews/v1 ...
echo ========================================
