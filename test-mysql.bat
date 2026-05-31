@echo off
echo ========================================
echo MySQL 连接测试
echo ========================================
echo.

echo 测试1: 空密码
mysql -h localhost -P 3306 -u root -e "SELECT 'Connection successful!' AS Status;" 2>nul
if %errorlevel% == 0 (
    echo [成功] MySQL root 用户使用空密码
    echo.
    echo 请使用以下配置启动项目：
    echo   application-dev.yml 中 password: ""
    goto :end
)

echo [失败] 空密码无法连接
echo.

echo 测试2: 密码 = root
mysql -h localhost -P 3306 -u root -proot -e "SELECT 'Connection successful!' AS Status;" 2>nul
if %errorlevel% == 0 (
    echo [成功] MySQL root 用户密码是: root
    echo.
    echo 请使用以下配置启动项目：
    echo   application-dev.yml 中 password: "root"
    goto :end
)

echo [失败] 密码 root 无法连接
echo.

echo 测试3: 密码 = 123456
mysql -h localhost -P 3306 -u root -p123456 -e "SELECT 'Connection successful!' AS Status;" 2>nul
if %errorlevel% == 0 (
    echo [成功] MySQL root 用户密码是: 123456
    echo.
    echo 请使用以下配置启动项目：
    echo   application-dev.yml 中 password: "123456"
    goto :end
)

echo [失败] 密码 123456 无法连接
echo.

echo ========================================
echo 所有常见密码测试失败
echo ========================================
echo.
echo 请手动确认MySQL密码，然后：
echo.
echo 1. 编辑文件: prreview-web\src\main\resources\application-dev.yml
echo 2. 修改 password 字段为正确的密码
echo 3. 运行: cd prreview-web ^&^& mvn spring-boot:run -Dspring-boot.run.profiles=dev
echo.
echo 或者参考 STARTUP_GUIDE.md 中的密码重置方法
echo.

:end
pause
