@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM AI PR Review Assistant - 快速功能测试脚本 (Windows)

set API_URL=http://localhost:8080/api/reviews/v1
set API_KEY=dev-api-key-change-in-production

echo ==========================================
echo AI PR Review Assistant - 功能测试
echo ==========================================
echo.

REM 1. 健康检查
echo 1. 检查应用健康状态...
curl -s http://localhost:8080/actuator/health > health.tmp
findstr /C:"\"status\":\"UP\"" health.tmp >nul
if %errorlevel% equ 0 (
    echo [32m✓ 应用运行正常[0m
) else (
    echo [31m✗ 应用未启动或异常[0m
    echo 请先启动应用: cd prreview-web ^&^& mvn spring-boot:run
    del health.tmp
    exit /b 1
)
del health.tmp
echo.

REM 2. 提交快速扫描任务
echo 2. 提交 PR 审查任务（FAST 模式）...
set IDEMPOTENCY_KEY=test-%RANDOM%

curl -s -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: %API_KEY%" ^
  -H "Idempotency-Key: %IDEMPOTENCY_KEY%" ^
  -d "{\"repository\":\"spring-projects/spring-boot\",\"pullRequestNumber\":1,\"analysisProfile\":\"FAST\",\"riskCategories\":[\"SECURITY\",\"CORRECTNESS\"]}" > submit.tmp

REM 提取 reviewId (简化版，实际可能需要 jq 或 PowerShell)
for /f "tokens=2 delims=:," %%a in ('findstr "reviewId" submit.tmp') do (
    set REVIEW_ID=%%a
    set REVIEW_ID=!REVIEW_ID:"=!
    set REVIEW_ID=!REVIEW_ID: =!
)

if "!REVIEW_ID!"=="" (
    echo [31m✗ 提交失败[0m
    type submit.tmp
    del submit.tmp
    exit /b 1
) else (
    echo [32m✓ 任务已提交[0m
    echo Review ID: !REVIEW_ID!
)
del submit.tmp
echo.

REM 3. 轮询任务状态
echo 3. 轮询任务状态...
set MAX_ATTEMPTS=30
set ATTEMPT=0
set STATUS=PENDING

:poll_loop
if !ATTEMPT! geq %MAX_ATTEMPTS% goto poll_timeout
if "!STATUS!"=="COMPLETED" goto poll_done
if "!STATUS!"=="FAILED" goto poll_failed

timeout /t 2 /nobreak >nul
set /a ATTEMPT+=1

curl -s "%API_URL%/!REVIEW_ID!/status" > status.tmp
for /f "tokens=2 delims=:," %%a in ('findstr "\"status\"" status.tmp') do (
    set STATUS=%%a
    set STATUS=!STATUS:"=!
    set STATUS=!STATUS: =!
)

echo [33m[尝试 !ATTEMPT!/%MAX_ATTEMPTS%] 状态: !STATUS![0m
goto poll_loop

:poll_timeout
echo [31m✗ 分析超时[0m
del status.tmp
exit /b 1

:poll_failed
echo [31m✗ 分析失败[0m
del status.tmp
exit /b 1

:poll_done
echo [32m✓ 分析完成[0m
del status.tmp
echo.

REM 4. 获取完整结果
echo 4. 获取审查结果...
curl -s "%API_URL%/!REVIEW_ID!" > result.tmp

echo [32m✓ 结果获取成功[0m
echo.
echo ==========================================
echo 审查结果摘要
echo ==========================================
echo Review ID: !REVIEW_ID!
echo.
echo 完整结果已保存到: result.tmp
echo 使用以下命令查看格式化结果:
echo   type result.tmp ^| jq .
echo 或直接查看:
echo   type result.tmp
echo.

REM 5. 测试幂等性
echo 5. 测试幂等性（使用相同 Idempotency-Key）...
set IDEM_KEY=test-idem-%RANDOM%

curl -s -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: %API_KEY%" ^
  -H "Idempotency-Key: %IDEM_KEY%" ^
  -d "{\"repository\":\"owner/repo\",\"pullRequestNumber\":999,\"analysisProfile\":\"FAST\"}" > first.tmp

for /f "tokens=2 delims=:," %%a in ('findstr "reviewId" first.tmp') do (
    set FIRST_ID=%%a
    set FIRST_ID=!FIRST_ID:"=!
    set FIRST_ID=!FIRST_ID: =!
)

timeout /t 1 /nobreak >nul

curl -s -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: %API_KEY%" ^
  -H "Idempotency-Key: %IDEM_KEY%" ^
  -d "{\"repository\":\"owner/repo\",\"pullRequestNumber\":999,\"analysisProfile\":\"FAST\"}" > second.tmp

for /f "tokens=2 delims=:," %%a in ('findstr "reviewId" second.tmp') do (
    set SECOND_ID=%%a
    set SECOND_ID=!SECOND_ID:"=!
    set SECOND_ID=!SECOND_ID: =!
)

if "!FIRST_ID!"=="!SECOND_ID!" (
    echo [32m✓ 幂等性验证通过（两次请求返回相同 ID）[0m
    echo Review ID: !FIRST_ID!
) else (
    echo [31m✗ 幂等性验证失败[0m
    echo 第一次: !FIRST_ID!
    echo 第二次: !SECOND_ID!
)
del first.tmp second.tmp
echo.

REM 6. 测试输入验证
echo 6. 测试输入验证（无效的 repository 格式）...
curl -s -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: %API_KEY%" ^
  -d "{\"repository\":\"invalid-format\",\"pullRequestNumber\":1}" > invalid.tmp

findstr /C:"error" /C:"validation" /C:"must be in" invalid.tmp >nul
if %errorlevel% equ 0 (
    echo [32m✓ 输入验证正常工作[0m
) else (
    echo [33m⚠ 输入验证可能未生效[0m
)
del invalid.tmp
echo.

echo ==========================================
echo 测试完成！
echo ==========================================
echo.
echo 访问 Swagger UI 查看完整 API 文档:
echo http://localhost:8080/swagger-ui.html
echo.
echo 查看详细功能说明:
echo type FEATURE_VERIFICATION.md
echo.

pause
