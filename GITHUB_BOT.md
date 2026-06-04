# GitHub Bot 集成指南

## 功能说明

PRReview 现在支持完整的 GitHub Bot 集成，提供两种审查模式：

### 1. 自动审查模式（推荐）

Bot 会自动审查以下事件：
- ✅ **PR 创建** (`opened` 事件)
- ✅ **PR 更新** (`synchronize` 事件 - 新提交推送时)

无需任何手动操作！

### 2. 手动触发模式

在 PR 评论中输入 `/review` 命令来触发审查。

## 工作流程

### 自动审查流程

```
1. 用户创建/更新 PR
   ↓
2. GitHub 发送 webhook 到服务器
   ↓
3. Bot 自动检测事件（opened/synchronize）
   ↓
4. Bot 发布 "🔍 审查已启动" 评论
   ↓
5. 后台异步执行代码审查
   ↓
6. 审查完成后，通过事件监听器自动发布结果
```

### 手动触发流程

```
1. 用户在 PR 评论中输入 /review
   ↓
2. Bot 回复 👀 表情确认收到
   ↓
3. Bot 发布 "🔍 审查已启动" 评论
   ↓
4. 后台执行代码审查
   ↓
5. 审查完成后自动发布结果
```

### Bot 智能检测

为防止无限循环，Bot 会自动忽略来自其他 bot 的评论：
- 检查用户类型字段 (`type: "Bot"`)
- 检查用户名模式 (以 `[bot]` 结尾)

这确保 Bot 不会响应自己或其他 GitHub App 的评论。

## 配置步骤

### 1. 配置环境变量

```bash
# Windows
set aliQwen_api=your-dashscope-api-key
set GITHUB_TOKEN=your-github-token

# Linux/Mac
export aliQwen_api=your-dashscope-api-key
export GITHUB_TOKEN=your-github-token
```

**获取 API Key:**
- 阿里云 DashScope: https://dashscope.console.aliyun.com/apiKey
- GitHub Token: https://github.com/settings/tokens (需要 `repo` 权限)

### 2. 配置数据库

```sql
CREATE DATABASE prreview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

编辑 `prreview-web/src/main/resources/application.yml` 配置数据库连接。

### 3. 启动应用

**Windows (一键启动):**

```bash
# 构建并启动
build-and-start-prod.bat

# 或分步执行
mvn clean package -DskipTests
start-prod.bat
```

**Linux/Mac:**

```bash
# 构建项目
mvn clean package -DskipTests

# 启动服务
java -jar prreview-web/target/prreview-web-0.1.0-SNAPSHOT.jar
```

### 4. 配置 ngrok（本地开发必需）

由于 GitHub webhook 需要公网可访问的 URL，本地开发时需要使用 ngrok：

**Windows:**

```bash
start-ngrok.bat
```

**Linux/Mac:**

```bash
ngrok http 8080
```

ngrok 会显示一个公网 URL，例如：`https://xxxx-xx-xx-xxx-xxx.ngrok.io`

### 5. 配置 GitHub Webhook

在你的 GitHub 仓库中配置 Webhook：

1. 进入仓库 Settings → Webhooks → Add webhook
2. 填写配置：
   - **Payload URL**: `https://your-ngrok-url.ngrok.io/api/webhook/github`
   - **Content type**: `application/json`
   - **Secret**: （可选，用于验证请求）
   - **Events**: 选择以下事件
     - ✅ **Pull requests** (用于自动审查)
     - ✅ **Issue comments** (用于 `/review` 命令)
3. 点击 "Add webhook"

### 6. 测试配置

**方法一：健康检查**

```bash
curl http://localhost:8080/api/webhook/github/health
```

应该返回：
```json
{
  "status": "ok",
  "service": "prreview-webhook"
}
```

**方法二：创建测试 PR**

1. 在配置了 webhook 的仓库中创建一个新的 PR
2. 观察应用日志，应该看到：
   ```
   Auto-triggering review for PR: owner/repo#123 (action: opened)
   ```
3. Bot 会自动发布审查评论

## 使用方法

### 自动审查（无需手动操作）

创建或更新 PR 后，Bot 会自动：

1. **检测 PR 事件**
   ```
   INFO: Auto-triggering review for PR: owner/repo#123 (action: opened)
   ```

2. **发布初始评论**
   ```markdown
   🔍 **审查已启动！**
   
   正在分析您的 PR，这可能需要几分钟时间。
   
   审查 ID: `uuid`
   ```

3. **执行审查**（后台异步执行）
   - 分析所有变更文件
   - 识别风险项
   - 生成改进建议

4. **自动发布结果**（通过 ReviewCompletedListener 事件监听器）

### 手动触发

在任意 PR 的评论中输入：

```
/review
```

Bot 会：
1. 回复 👀 表情（确认收到）
2. 发布 "审查已启动" 评论
3. 执行审查并自动发布结果

### 审查结果格式

审查结果包含：

- **📋 Summary**: PR 概要和影响的模块
- **🚨 Critical Issues**: 严重问题（必须修复）
- **⚠️ High Priority**: 高优先级问题（建议修复）
- **⚡ Medium Priority**: 中等优先级问题
- **💡 Low Priority**: 低优先级建议

每个问题包含：
- 文件路径和行号
- 问题描述和原因
- 修复建议
- 代码补丁（如果适用）

## 架构说明

### 事件驱动设计

项目采用事件驱动架构，避免轮询和阻塞：

```
WebhookService (接收 webhook)
  ↓
创建 Review 实体
  ↓
提交 ReviewTaskRunner (异步执行)
  ↓
返回响应 (立即返回，不等待完成)
  ↓
ReviewOrchestrator 完成审查后发布 ReviewCompletedEvent
  ↓
ReviewCompletedListener 监听事件
  ↓
自动发布结果到 GitHub
```

**优势：**
- ✅ 无轮询开销
- ✅ 不阻塞 HTTP 线程
- ✅ 解耦业务逻辑和 GitHub 交互
- ✅ 易于扩展和测试

### 防重复机制

**Bot 循环检测：**
```java
// 检查用户类型
if ("Bot".equals(userType)) {
    log.info("Ignoring comment from bot user");
    return false;
}

// 检查用户名模式
if (login != null && login.endsWith("[bot]")) {
    log.info("Ignoring comment from bot user: {}", login);
    return false;
}
```

**重复审查检测：**
- 方法 `hasRecentReview()` 检查 5 分钟内是否已有审查
- 防止同一 PR 的重复 webhook 事件触发多次审查

## 配置说明

### 启用/禁用自动审查

编辑 `WebhookService.java` 中的 `handlePullRequest()` 方法：

```java
// 当前配置：触发 "opened" 和 "synchronize" 事件
if (!"opened".equals(action) && !"synchronize".equals(action)) {
    return false;
}

// 仅触发新 PR：
if (!"opened".equals(action)) {
    return false;
}

// 禁用自动审查（仅保留手动 /review 触发）：
// 在 WebhookController 中注释掉 handlePullRequest 的调用
```

### 自定义审查配置

编辑 `application.yml`：

```yaml
prreview:
  analysis:
    max-files: 300           # 单次审查最多分析的文件数
    parallel-degree: 10      # 并行分析文件数
    timeout-seconds: 300     # 审查超时时间（秒）
```

## 安全配置（生产环境）

### 1. 验证 Webhook 签名

在 `WebhookService` 中启用签名验证：

```java
// 取消注释这一行
webhookService.verifySignature(payload, signature);
```

实现签名验证方法：

```java
public void verifySignature(Map<String, Object> payload, String signature) {
    // 使用 HMAC-SHA256 验证签名
    // 详见 GitHub 文档：https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries
}
```

### 2. 限制 Webhook 来源

在 `SecurityConfig` 中添加 IP 白名单：

```java
.requestMatchers("/api/webhook/**")
    .hasIpAddress("192.30.252.0/22") // GitHub webhook IP 范围
```

### 3. 使用 GitHub App

生产环境建议使用 GitHub App 而不是 Personal Access Token：

1. 创建 GitHub App
2. 配置权限：`pull_requests: write`, `issues: write`
3. 安装到仓库
4. 使用 App 的 JWT 认证

## 故障排查

### Webhook 未触发

1. 检查 GitHub Webhook 配置页面的 "Recent Deliveries"
2. 查看响应状态码和错误信息
3. 确认 ngrok 或公网地址可访问
4. 检查应用日志中的 webhook 事件

### 审查失败

1. 检查 GitHub Token 权限（需要 `repo` 权限）
2. 检查阿里云 API Key 是否有效
3. 查看应用日志中的错误信息

### 评论未发布

1. 确认 GitHub Token 有 `repo` 权限
2. 检查网络连接
3. 查看 `GitHubCommentService` 日志
4. 确认事件监听器 `ReviewCompletedListener` 正常工作

### Bot 发布多条评论

如果 Bot 重复发布评论：

1. 检查是否有多个 webhook 配置
2. 确认没有重复触发审查
3. 查看日志中的重复检测信息

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/webhook/github` | POST | 接收 GitHub webhook |
| `/api/webhook/github/health` | GET | 健康检查 |
| `/api/reviews/{id}` | GET | 查询审查详情 |
| `/api/reviews/{id}/status` | GET | 查询审查状态 |

## 限制

- 单次审查最多分析 300 个文件
- 审查超时时间：5 分钟
- 并发审查数量：10 个（可配置）

## 下一步优化

- [ ] 支持自定义审查配置（通过评论参数）
- [ ] 支持增量审查（只审查新提交的文件）
- [ ] 添加审查报告导出功能（PDF/Markdown）
- [ ] 支持多种 AI 模型切换
- [ ] 添加审查质量评分系统
