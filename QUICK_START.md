# 快速开始指南

⏱️ **3 步快速启动，5 分钟内运行 AI PR 审查服务！**

## 前置要求

- ✅ Java 21+
- ✅ Maven 3.6+
- ✅ MySQL 8.0+
- ✅ 阿里云 DashScope API Key
- ✅ GitHub Personal Access Token

## 第一步：配置环境变量

### Windows

打开命令行，设置环境变量：

```bash
set aliQwen_api=sk-xxxxxxxxxxxxxxxx
set GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxx
```

### Linux/Mac

```bash
export aliQwen_api=sk-xxxxxxxxxxxxxxxx
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxx
```

### 获取 API Key

| 服务 | 地址 | 权限要求 |
|------|------|---------|
| 阿里云 DashScope | https://dashscope.console.aliyun.com/apiKey | - |
| GitHub Token | https://github.com/settings/tokens | `repo` 权限 |

## 第二步：配置数据库

### 创建数据库

```sql
CREATE DATABASE prreview 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 配置连接

编辑 `prreview-web/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prreview?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

## 第三步：启动服务

### Windows (推荐)

**一键启动：**

```bash
build-and-start-prod.bat
```

这个脚本会：
1. ✅ 检查环境变量
2. ✅ 构建项目（跳过测试）
3. ✅ 启动服务

**或分步执行：**

```bash
# 构建
mvn clean package -DskipTests

# 启动
start-prod.bat
```

### Linux/Mac

```bash
# 构建
mvn clean package -DskipTests

# 启动
cd prreview-web
mvn spring-boot:run
```

## 验证安装

### 1. 检查服务状态

打开浏览器访问：

```
http://localhost:8080/actuator/health
```

应该返回：

```json
{
  "status": "UP"
}
```

### 2. 访问 Swagger UI

```
http://localhost:8080/swagger-ui.html
```

你可以在这里测试所有的 API 接口。

### 3. 测试 API

使用 curl 提交一个审查任务：

```bash
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "X-API-Key: dev-api-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "owner/repo-name",
    "pullRequestNumber": 1
  }'
```

## 配置 GitHub Bot（可选）

如果想要使用 GitHub Bot 自动审查功能：

### 1. 启动 ngrok

本地开发需要 ngrok 暴露服务到公网：

**Windows:**

```bash
start-ngrok.bat
```

**Linux/Mac:**

```bash
ngrok http 8080
```

复制 ngrok 提供的公网 URL，例如：`https://xxxx.ngrok.io`

### 2. 配置 GitHub Webhook

1. 进入你的 GitHub 仓库
2. Settings → Webhooks → Add webhook
3. 配置：
   - **Payload URL**: `https://xxxx.ngrok.io/api/webhook/github`
   - **Content type**: `application/json`
   - **Events**: 
     - ✅ Pull requests
     - ✅ Issue comments
4. 保存

### 3. 测试 Webhook

创建一个新的 PR，Bot 会自动发布审查评论！

或者在任何 PR 评论中输入 `/review` 手动触发。

## 常见问题

### Q1: 启动失败，提示 "Failed to configure a DataSource"

**原因：** 数据库连接配置错误

**解决：**
1. 确认 MySQL 服务正在运行
2. 检查 `application.yml` 中的数据库配置
3. 确认数据库 `prreview` 已创建

### Q2: AI 调用超时

**原因：** 网络问题或 API Key 无效

**解决：**
1. 确认 `aliQwen_api` 环境变量已设置
2. 测试 API Key：访问 https://dashscope.console.aliyun.com/
3. 检查网络连接

### Q3: GitHub API 调用失败

**原因：** Token 权限不足或无效

**解决：**
1. 确认 `GITHUB_TOKEN` 环境变量已设置
2. 确认 Token 有 `repo` 权限
3. 在 GitHub 检查 Token 是否过期

### Q4: Webhook 未触发

**原因：** ngrok 未启动或 webhook 配置错误

**解决：**
1. 确认 ngrok 正在运行
2. 在 GitHub Webhook 页面检查 "Recent Deliveries"
3. 查看响应状态码和错误信息

### Q5: 端口 8080 已被占用

**解决：**

编辑 `application.yml`，修改端口：

```yaml
server:
  port: 8081
```

然后重启服务。

## 下一步

- 📖 阅读完整文档：[README.md](./README.md)
- 🤖 配置 GitHub Bot：[GITHUB_BOT.md](./GITHUB_BOT.md)
- 🔧 查看 API 文档：http://localhost:8080/swagger-ui.html

## 获取帮助

如有问题：
1. 查看应用日志
2. 查看 [GitHub Issues](https://github.com/ottoottoadwa/qiniu-contest-project/issues)
3. 提交新的 Issue

---

**恭喜！🎉 你的 AI PR 审查服务已成功启动！**
