# GitHub Bot 集成指南

## 功能说明

PRReview 现在支持 GitHub Bot 集成，可以通过在 PR 评论中输入 `/review` 命令来自动触发代码审查。

## 工作流程

1. 用户在 PR 评论中输入 `/review`
2. Bot 回复 👀 表情确认收到
3. Bot 发布 "Review started" 评论
4. 后台执行代码审查（分析风险、生成建议）
5. 审查完成后，Bot 发布详细的审查结果评论

## 配置步骤

### 1. 配置 GitHub Webhook

在你的 GitHub 仓库中配置 Webhook：

1. 进入仓库 Settings → Webhooks → Add webhook
2. 填写配置：
   - **Payload URL**: `https://your-domain.com/api/webhook/github`
   - **Content type**: `application/json`
   - **Secret**: （可选，用于验证请求）
   - **Events**: 选择 `Issue comments` 和 `Pull requests`
3. 点击 "Add webhook"

### 2. 配置环境变量

确保以下环境变量已配置：

```bash
# GitHub Personal Access Token (需要 repo 权限)
export GITHUB_TOKEN=ghp_your_token_here

# 阿里云通义千问 API Key
export aliQwen_api=sk-your-api-key

# 数据库配置
export DB_URL=jdbc:mysql://localhost:3306/prreview
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

### 3. 启动应用

```bash
# 使用启动脚本（会检查环境变量）
./start-with-env.bat

# 或者直接运行
mvn spring-boot:run
```

### 4. 测试 Webhook

测试 webhook 是否配置成功：

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

## 使用方法

### 触发审查

在任意 PR 的评论中输入：

```
/review
```

Bot 会自动：
1. 回复 👀 表情
2. 发布 "Review started" 评论
3. 执行审查（可能需要几分钟）
4. 发布审查结果

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

## 示例

### 触发审查

<img src="docs/images/trigger-review.png" alt="触发审查" width="600"/>

### 审查结果

<img src="docs/images/review-result.png" alt="审查结果" width="600"/>

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
3. 检查应用日志：`tail -f logs/prreview.log`

### 审查失败

1. 检查 GitHub Token 权限
2. 检查 API Key 是否有效
3. 查看错误日志：`grep ERROR logs/prreview.log`

### 评论未发布

1. 确认 GitHub Token 有 `repo` 权限
2. 检查网络连接
3. 查看 `GitHubCommentService` 日志

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/webhook/github` | POST | 接收 GitHub webhook |
| `/api/webhook/github/health` | GET | 健康检查 |
| `/api/reviews/{id}` | GET | 查询审查状态 |

## 限制

- 单次审查最多分析 300 个文件
- 审查超时时间：5 分钟
- 并发审查数量：10 个

## 下一步

- [ ] 支持自定义审查配置（通过评论参数）
- [ ] 支持 PR 打开时自动审查
- [ ] 支持增量审查（只审查新提交）
- [ ] 添加审查报告导出功能
