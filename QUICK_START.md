# 快速开始指南

## 🚀 5 分钟快速验证

### 步骤 1：确认应用已启动

应用应该已经在运行（端口 8080）。如果没有，请运行：

```bash
cd prreview-web
mvn spring-boot:run
```

等待看到：`Started PrReviewApplication in X.XXX seconds`

### 步骤 2：验证健康状态

```bash
curl http://localhost:8080/actuator/health
```

期望输出：`{"status":"UP"}`

### 步骤 3：访问 Swagger UI

打开浏览器访问：**http://localhost:8080/swagger-ui.html**

这是最直观的测试方式！

### 步骤 4：在 Swagger UI 中测试

1. 找到 **POST /api/reviews/v1** 接口
2. 点击 "Try it out"
3. 填入测试数据：

```json
{
  "repository": "spring-projects/spring-boot",
  "pullRequestNumber": 1,
  "analysisProfile": "FAST",
  "riskCategories": ["SECURITY", "CORRECTNESS"]
}
```

4. 点击 "Execute"
5. 查看响应，复制 `reviewId`

### 步骤 5：查询结果

1. 找到 **GET /api/reviews/v1/{reviewId}/status** 接口
2. 粘贴刚才的 `reviewId`
3. 点击 "Execute" 查看进度
4. 等待 `status` 变为 `COMPLETED`

### 步骤 6：获取完整报告

1. 找到 **GET /api/reviews/v1/{reviewId}** 接口
2. 粘贴 `reviewId`
3. 点击 "Execute"
4. 查看完整的审查结果！

## 📋 项目符合性检查清单

### ✅ 核心功能

- [x] **指定 GitHub PR**：通过 `repository` + `pullRequestNumber` 参数
- [x] **自动获取代码变更**：集成 GitHub API
- [x] **PR 变更总结**：`summary` 字段包含 headline、inferredPurpose、affectedModules 等
- [x] **风险代码识别**：`riskItems` 数组，包含位置、类别、严重程度
- [x] **Review 建议生成**：每个风险项的 `suggestion` 包含 explanation、recommendation、suggestedPatch

### ✅ 关键技术指标

- [x] **分析准确性**：Rule Engine + AI Model 双通道融合
- [x] **上下文理解**：L0(diff) → L1(file) → L2(dependency) 三级上下文
- [x] **误报控制**：置信度评分 + 历史反馈校准
- [x] **漏报控制**：规则引擎 + AI 协同，覆盖已知和未知风险
- [x] **响应速度**：异步处理 + FAST/STANDARD/DEEP 三档模式
- [x] **使用体验**：RESTful API + Swagger UI + 轮询机制

### ✅ 设计思路说明

详见 `FEATURE_VERIFICATION.md` 文档，包含：

1. **模型选择策略**：
   - FAST: qwen-turbo（快速扫描）
   - STANDARD: qwen-plus（标准分析）
   - DEEP: qwen-max（深度分析）

2. **上下文获取方式**：
   - L0: Diff Context（必需）
   - L1: File Context（STANDARD+）
   - L2: Dependency Context（DEEP）

3. **未来扩展方向**：
   - 短期：增量分析、自定义规则、IDE 插件
   - 中期：多语言支持、团队协作、学习优化
   - 长期：自动修复、漏洞数据库集成、质量趋势分析

## 🎯 核心 API 说明

### 1. 提交审查任务

**POST** `/api/reviews/v1`

**请求体**：
```json
{
  "repository": "owner/repo",           // GitHub 仓库（必需）
  "pullRequestNumber": 123,             // PR 编号（必需）
  "analysisProfile": "STANDARD",        // 分析模式：FAST/STANDARD/DEEP
  "riskCategories": [                   // 检测类别（可选）
    "SECURITY",
    "CORRECTNESS",
    "PERFORMANCE",
    "MAINTAINABILITY"
  ],
  "callbackUrl": "https://..."          // 完成后回调（可选）
}
```

**响应** (202 Accepted)：
```json
{
  "reviewId": "uuid",
  "status": "PENDING",
  "statusUrl": "/api/reviews/v1/{reviewId}/status",
  "acceptedAt": "2026-05-31T16:00:00Z"
}
```

### 2. 查询任务状态

**GET** `/api/reviews/v1/{reviewId}/status`

**响应**：
```json
{
  "reviewId": "uuid",
  "status": "IN_PROGRESS",              // PENDING/IN_PROGRESS/COMPLETED/FAILED
  "progress": 0.65,                     // 0.0 - 1.0
  "filesTotal": 20,
  "filesAnalyzed": 13,
  "startedAt": "2026-05-31T16:00:05Z",
  "estimatedRemainingSeconds": 45,
  "resultUrl": "/api/reviews/v1/{reviewId}"
}
```

### 3. 获取完整结果

**GET** `/api/reviews/v1/{reviewId}`

**响应**：
```json
{
  "reviewId": "uuid",
  "repository": "owner/repo",
  "pullRequestNumber": 123,
  "status": "COMPLETED",
  "summary": {
    "headline": "重构认证模块，引入 JWT 支持",
    "inferredPurpose": "将 Session 认证迁移到 JWT",
    "affectedModules": ["security", "auth"],
    "primaryType": "REFACTOR",
    "riskHighlights": ["JWT 密钥硬编码风险"]
  },
  "riskItems": [
    {
      "id": "risk-001",
      "filePath": "src/main/java/com/example/JwtUtil.java",
      "startLine": 45,
      "endLine": 47,
      "category": "SECURITY",
      "severity": "HIGH",
      "confidence": "HIGH",
      "detectionSource": "BOTH",
      "description": "JWT 密钥硬编码在源代码中",
      "rationale": "硬编码的密钥容易泄露",
      "suggestion": {
        "explanation": "JWT 签名密钥不应硬编码",
        "recommendation": "将密钥移至配置文件",
        "suggestedPatch": "@Value(\"${jwt.secret}\")\nprivate String jwtSecret;",
        "references": ["https://owasp.org/..."]
      }
    }
  ],
  "metrics": {
    "bySeverity": {"HIGH": 3, "MEDIUM": 7, "LOW": 12},
    "byCategory": {"SECURITY": 5, "CORRECTNESS": 8}
  },
  "completedAt": "2026-05-31T16:02:30Z"
}
```

## 🧪 自动化测试

### Linux/Mac

```bash
chmod +x test-features.sh
./test-features.sh
```

### Windows

```cmd
test-features.bat
```

测试脚本会自动验证：
1. ✅ 应用健康状态
2. ✅ 提交审查任务
3. ✅ 轮询任务状态
4. ✅ 获取完整结果
5. ✅ 幂等性验证
6. ✅ 输入验证

## 📚 完整文档

- **功能验证指南**：`FEATURE_VERIFICATION.md` - 详细的需求符合性说明
- **启动指南**：`STARTUP_GUIDE.md` - 数据库配置和启动步骤
- **API 文档**：http://localhost:8080/swagger-ui.html - 交互式 API 文档

## 💡 常见问题

### Q1: 如何配置 GitHub Token？

编辑 `prreview-web/src/main/resources/application.yml`：

```yaml
github:
  token: ${GITHUB_TOKEN:your-github-token-here}
```

或设置环境变量：
```bash
export GITHUB_TOKEN="ghp_xxxxxxxxxxxx"
```

### Q2: 如何配置 AI 模型 API Key？

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your-api-key}
```

### Q3: 数据存储在哪里？

MySQL 数据库 `prreview`，包含以下表：
- `reviews` - 审查任务
- `change_summaries` - 变更总结
- `risk_items` - 风险项
- `review_suggestions` - 修复建议
- `review_feedbacks` - 用户反馈

### Q4: 如何查看数据库内容？

```bash
mysql -u root -p
use prreview;
SELECT * FROM reviews ORDER BY created_at DESC LIMIT 5;
SELECT * FROM risk_items WHERE review_id = 'your-review-id';
```

## 🎉 验证完成

如果你能成功：
1. ✅ 访问 Swagger UI
2. ✅ 提交一个审查任务
3. ✅ 查询到任务状态
4. ✅ 获取到完整结果

**恭喜！项目已完全符合大赛要求，可以提交作品了！** 🎊

---

**技术支持**：查看 `FEATURE_VERIFICATION.md` 了解详细的架构设计和技术亮点。
