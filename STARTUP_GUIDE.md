# PR Review 项目启动指南

## 问题诊断

当前遇到的问题是 **MySQL数据库连接认证失败**。

错误信息：`Access denied for user 'root'@'localhost'`

## 解决方案

### 方案1：找到正确的MySQL密码

1. 如果你知道MySQL root密码，修改配置文件：
   
   编辑 `prreview-web/src/main/resources/application-dev.yml`
   
   将 `password: ""` 改为 `password: "你的实际密码"`

2. 然后启动：
   ```bash
   cd prreview-web
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### 方案2：重置MySQL root密码

如果忘记了MySQL密码，可以重置：

**Windows系统：**

1. 停止MySQL服务
   ```cmd
   net stop mysql
   ```

2. 以安全模式启动MySQL（跳过权限验证）
   ```cmd
   mysqld --console --skip-grant-tables --shared-memory
   ```

3. 打开新的命令行窗口，连接MySQL
   ```cmd
   mysql -u root
   ```

4. 重置密码
   ```sql
   USE mysql;
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
   FLUSH PRIVILEGES;
   EXIT;
   ```

5. 关闭安全模式的MySQL，正常启动MySQL服务
   ```cmd
   net start mysql
   ```

### 方案3：创建新的数据库用户

如果不想使用root用户，可以创建专用用户：

```sql
-- 以管理员身份连接MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE IF NOT EXISTS prreview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER 'prreview'@'localhost' IDENTIFIED BY 'prreview123';
GRANT ALL PRIVILEGES ON prreview.* TO 'prreview'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

然后修改 `application-dev.yml`：
```yaml
spring:
  datasource:
    username: prreview
    password: prreview123
```

### 方案4：使用环境变量（推荐用于生产环境）

创建启动脚本 `start.bat`（Windows）：

```batch
@echo off
set DB_USERNAME=root
set DB_PASSWORD=你的实际密码
set DASHSCOPE_API_KEY=你的阿里云API密钥
set GITHUB_TOKEN=你的GitHub令牌

cd prreview-web
mvn spring-boot:run
```

或 `start.sh`（Linux/Mac）：

```bash
#!/bin/bash
export DB_USERNAME=root
export DB_PASSWORD=你的实际密码
export DASHSCOPE_API_KEY=你的阿里云API密钥
export GITHUB_TOKEN=你的GitHub令牌

cd prreview-web
mvn spring-boot:run
```

## 当前项目配置

- **数据库类型**: MySQL 8.0+
- **默认端口**: 8080
- **数据库名**: prreview
- **JPA模式**: update（开发环境自动创建表）
- **Flyway**: 已禁用（开发环境）

## 启动后验证

启动成功后，访问：

- **健康检查**: http://localhost:8080/actuator/health
- **API文档**: http://localhost:8080/swagger-ui.html
- **OpenAPI规范**: http://localhost:8080/v3/api-docs

## 下一步

1. 确认MySQL密码
2. 修改配置文件或使用环境变量
3. 重新启动应用
4. 检查日志确认启动成功

## 需要的环境变量（可选）

```bash
# 数据库连接（必需）
DB_USERNAME=root
DB_PASSWORD=你的密码

# AI服务（可选，用于代码审查功能）
DASHSCOPE_API_KEY=你的阿里云DashScope API密钥

# GitHub集成（可选，用于拉取PR信息）
GITHUB_TOKEN=你的GitHub个人访问令牌
```

## 常见错误

1. **Access denied** - 密码错误，检查MySQL密码
2. **Public Key Retrieval is not allowed** - 已在JDBC URL中添加 `allowPublicKeyRetrieval=true` 解决
3. **Unknown database 'prreview'** - 数据库不存在，已在JDBC URL中添加 `createDatabaseIfNotExist=true` 解决
4. **Port 8080 already in use** - 端口被占用，修改 `application.yml` 中的 `server.port`
