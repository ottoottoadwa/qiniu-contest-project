@echo off
echo ========================================
echo Building PRReview Application
echo ========================================
echo.

cd prreview-web
echo Building with Maven (skip tests)...
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build successful!
echo ========================================
echo.

cd ..
call start-prod.bat
