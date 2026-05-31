# AI PR Review Assistant - 功能验证指南

## 项目概述

这是一个基于 AI 的 GitHub Pull Request 代码审查助手，符合七牛云大模型应用开发大赛的所有要求。

## ✅ 需求符合性检查

### 1. 核心功能实现

| 需求 | 实现状态 | 说明 |
|------|---------|------|
| **指定 GitHub PR** | ✅ 已实现 | 通过 `repository` + `pullRequestNumber` 参数指定 |
| **自动获取代码变更** | ✅ 已实现 | 集成 GitHub API，自动拉取 PR diff |
| **PR 变更总结** | ✅ 已实现 | `ChangeSummary` 包含：headline、inferredPurpose、affectedModules、primaryType、riskHighlights |
| **风险代码识别** | ✅ 已实现 | 多维度风险检测：SECURITY、CORRECTNESS、PERFORMANCE、MAINTAINABILITY |
| **Review 建议生成** | ✅ 已实现 | `ReviewSuggestion` 包含：explanation、recommendation、suggestedPatch、references |

### 2. 关键技术指标

| 指标 | 实现方案 | 说明 |
|------|---------|------|
| **分析准确性** | 双通道融合 | Rule Engine + AI Model 双重验证，降低误报 |
| **上下文理解** | 分层上下文获取 | L0(diff) → L1(文件) → L2(依赖) 三级上下文 |
| **误报控制** | 置信度评分 + 反馈校准 | 基于历史反馈动态调整检测阈值 |
| **漏报控制** | 多模型协同 | 规则引擎覆盖已知模式，AI 发现未知风险 |
| **响应速度** | 异步处理 + 模型分层 | Fast/Standard/Deep 三档分析速度 |
| **使用体验** | RESTful API + 轮询机制 | 202 Accepted → 轮询 status → 获取 result |

## 🏗️ 系统架构设计

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Web Layer                             │
│  ReviewController (REST API) + Swagger UI                   │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                   Application Layer                          │
│  SubmitReviewService → ReviewTaskRunner (Async)             │
│  ReviewOrchestrator → AnalysisEngine                         │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    Domain Layer                              │
│  Review (Aggregate Root) + RiskItem + ChangeSummary         │
│  RiskMergeService + ConfidenceScoringService                │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                Infrastructure Layer                          │
│  GitHub API Client + AI Model Router + MySQL Repository     │
└─────────────────────────────────────────────────────────────┘
```

### 核心设计思路

#### 1. 模型选择策略

**三档分析模式**：
- **FAST**：快速扫描（~30s），仅规则引擎 + 轻量 AI
- **STANDARD**：标准分析（~2min），规则 + AI 双通道
- **DEEP**：深度分析（~5min），完整上下文 + 多轮推理

**模型路由**：
```java
public class ModelRoutingProperties {
    private String fastModel = "qwen-turbo";      // 快速模型
    private String standardModel = "qwen-plus";   // 标准模型
    private String deepModel = "qwen-max";        // 深度模型
}
```

#### 2. 上下文获取方式

**分层上下文策略**：

```
L0: Diff Context (必需)
├─ 变更的代码行
├─ 前后 3 行上下文
└─ 文件路径和变更类型

L1: File Context (STANDARD+)
├─ 完整文件内容
├─ 函数/类定义
└─ Import 依赖

L2: Dependency Context (DEEP)
├─ 被调用的其他文件
├─ 接口定义
└─ 配置文件
```

**实现**：
```java
public class ContextBuilder {
    public AnalysisContext build(PullRequest pr, AnalysisProfile profile) {
        AnalysisContext ctx = new AnalysisContext();
        ctx.addDiffContext(pr.getDiff());  // L0
        
        if (profile.isStandardOrDeep()) {
            ctx.addFileContext(fetchFullFiles(pr));  // L1
        }
        
        if (profile.isDeep()) {
            ctx.addDependencyContext(fetchDependencies(pr));  // L2
        }
        
        return ctx;
    }
}
```

#### 3. 误报与漏报控制

**双通道融合机制**：

```java
public class RiskMergeService {
    public List<RiskItem> merge(
        List<AiRiskFinding> ruleFindings,  // 规则引擎结果
        List<AiRiskFinding> aiFindings,    // AI 模型结果
        Map<String, Double> calibrationMap  // 历史反馈校准
    ) {
        // 规则 + AI 都命中 → HIGH confidence (降低误报)
        // 仅规则命中 → MEDIUM confidence
        // 仅 AI 命中 → 使用 AI 自评分 (发现新风险，可能漏报)
    }
}
```

**置信度评分公式**：
```
score = base × categoryWeight × contextPenalty × calibrationFactor

其中：
- base: BOTH=0.9, RULE_ONLY=0.75, AI_ONLY=aiSelfConfidence
- categoryWeight: SECURITY/CORRECTNESS=1.0, 其他=0.8
- contextPenalty: 完整上下文=1.0, 不完整=0.85
- calibrationFactor: 基于历史反馈的动态调整 (0.5-1.5)
```

**反馈校准循环**：
```
用户反馈 → 更新 calibration_factor → 影响后续检测阈值
```

## 📋 API 使用指南

### 1. 提交 PR 审查任务

**请求**：
```bash
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "repository": "spring-projects/spring-boot",
    "pullRequestNumber": 12345,
    "analysisProfile": "STANDARD",
    "riskCategories": ["SECURITY", "CORRECTNESS", "PERFORMANCE"],
    "callbackUrl": "https://your-webhook.com/callback"
  }'
```

**响应** (202 Accepted)：
```json
{
  "reviewId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "statusUrl": "/api/reviews/v1/550e8400-e29b-41d4-a716-446655440000/status",
  "acceptedAt": "2026-05-31T16:00:00Z"
}
```

### 2. 轮询任务状态

**请求**：
```bash
curl http://localhost:8080/api/reviews/v1/550e8400-e29b-41d4-a716-446655440000/status
```

**响应**：
```json
{
  "reviewId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "progress": 0.65,
  "filesTotal": 20,
  "filesAnalyzed": 13,
  "startedAt": "2026-05-31T16:00:05Z",
  "estimatedRemainingSeconds": 45,
  "resultUrl": "/api/reviews/v1/550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. 获取完整结果

**请求**：
```bash
curl http://localhost:8080/api/reviews/v1/550e8400-e29b-41d4-a716-446655440000
```

**响应**：
```json
{
  "reviewId": "550e8400-e29b-41d4-a716-446655440000",
  "repository": "spring-projects/spring-boot",
  "pullRequestNumber": 12345,
  "status": "COMPLETED",
  "summary": {
    "headline": "重构认证模块，引入 JWT 支持",
    "inferredPurpose": "将现有的 Session 认证迁移到无状态 JWT 认证",
    "affectedModules": ["security", "auth", "user-service"],
    "primaryType": "REFACTOR",
    "riskHighlights": [
      "JWT 密钥硬编码风险",
      "缺少 token 过期时间配置"
    ]
  },
  "riskItems": [
    {
      "id": "risk-001",
      "filePath": "src/main/java/com/example/security/JwtUtil.java",
      "startLine": 45,
      "endLine": 47,
      "category": "SECURITY",
      "severity": "HIGH",
      "confidence": "HIGH",
      "detectionSource": "BOTH",
      "description": "JWT 密钥硬编码在源代码中",
      "rationale": "硬编码的密钥容易泄露，应使用环境变量或密钥管理服务",
      "suggestion": {
        "explanation": "JWT 签名密钥不应硬编码在代码中，一旦泄露将导致所有 token 可被伪造",
        "recommendation": "将密钥移至配置文件，并使用环境变量注入",
        "suggestedPatch": "@Value(\"${jwt.secret}\")\nprivate String jwtSecret;",
        "references": [
          "https://owasp.org/www-project-top-ten/2017/A3_2017-Sensitive_Data_Exposure"
        ]
      }
    }
  ],
  "metrics": {
    "bySeverity": {
      "CRITICAL": 0,
      "HIGH": 3,
      "MEDIUM": 7,
      "LOW": 12
    },
    "byCategory": {
      "SECURITY": 5,
      "CORRECTNESS": 8,
      "PERFORMANCE": 4,
      "MAINTAINABILITY": 5
    }
  },
  "completedAt": "2026-05-31T16:02:30Z"
}
```

## 🧪 功能验证步骤

### 前置准备

1. **启动应用**：
```bash
cd prreview-web
mvn spring-boot:run
```

2. **验证健康状态**：
```bash
curl http://localhost:8080/actuator/health
# 期望输出: {"status":"UP"}
```

3. **访问 Swagger UI**：
```
http://localhost:8080/swagger-ui.html
```

### 测试场景 1：快速扫描

```bash
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "spring-projects/spring-boot",
    "pullRequestNumber": 1,
    "analysisProfile": "FAST",
    "riskCategories": ["SECURITY", "CORRECTNESS"]
  }'
```

**验证点**：
- ✅ 返回 202 Accepted
- ✅ 包含 reviewId 和 statusUrl
- ✅ 30 秒内完成分析

### 测试场景 2：标准分析

```bash
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "Content-Type: application/json" \
  -d '{
    "repository": "spring-projects/spring-boot",
    "pullRequestNumber": 1,
    "analysisProfile": "STANDARD",
    "riskCategories": ["SECURITY", "CORRECTNESS", "PERFORMANCE", "MAINTAINABILITY"]
  }'
```

**验证点**：
- ✅ 生成 PR 变更总结
- ✅ 识别多维度风险
- ✅ 提供修复建议
- ✅ 2 分钟内完成

### 测试场景 3：幂等性验证

```bash
# 第一次请求
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-001" \
  -d '{"repository": "owner/repo", "pullRequestNumber": 1}'

# 第二次相同请求（应返回相同 reviewId）
curl -X POST http://localhost:8080/api/reviews/v1 \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-001" \
  -d '{"repository": "owner/repo", "pullRequestNumber": 1}'
```

**验证点**：
- ✅ 两次请求返回相同 reviewId
- ✅ 不会重复执行分析

## 🎯 未来扩展方向

### 1. 短期优化（1-3 个月）

- **增量分析**：仅分析新增的 commit，不重复分析已审查的代码
- **自定义规则**：允许团队配置自定义检测规则
- **IDE 插件**：VS Code / IntelliJ IDEA 插件，实时代码审查
- **Webhook 集成**：PR 创建/更新时自动触发审查

### 2. 中期扩展（3-6 个月）

- **多语言支持**：扩展到 Python、Go、Rust、TypeScript
- **团队协作**：审查结果共享、评论、讨论功能
- **学习优化**：基于团队反馈持续优化检测规则
- **性能优化**：分布式任务队列、结果缓存

### 3. 长期规划（6-12 个月）

- **代码修复建议自动应用**：一键应用修复补丁
- **安全漏洞数据库集成**：对接 CVE、OWASP 等数据库
- **代码质量趋势分析**：团队代码质量仪表盘
- **CI/CD 深度集成**：GitHub Actions、GitLab CI 原生支持

## 📊 技术亮点

### 1. 架构设计

- **DDD 分层架构**：清晰的职责划分，易于维护和扩展
- **六边形架构**：领域层零框架依赖，纯业务逻辑
- **CQRS 模式**：读写分离，优化查询性能

### 2. 工程实践

- **类型安全**：Java 21 Record + Bean Validation
- **异步处理**：Spring @Async + CompletableFuture
- **数据库迁移**：Flyway 版本化 schema 管理
- **API 文档**：OpenAPI 3.0 + Swagger UI

### 3. AI 集成

- **模型路由**：根据任务复杂度选择合适模型
- **上下文管理**：分层上下文获取，平衡准确性和成本
- **置信度评分**：量化风险可信度，辅助决策
- **反馈闭环**：持续学习，降低误报率

## 🔧 配置说明

### 必需配置

编辑 `prreview-web/src/main/resources/application.yml`：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prreview
    username: root
    password: your-password

# AI 模型配置（七牛云大模型）
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}

# GitHub API 配置
github:
  api-base-url: https://api.github.com
  token: ${GITHUB_TOKEN}
```

### 环境变量

```bash
export DASHSCOPE_API_KEY="your-dashscope-api-key"
export GITHUB_TOKEN="your-github-personal-access-token"
export DB_PASSWORD="your-mysql-password"
```

## 📝 总结

本项目完整实现了七牛云大模型应用开发大赛的所有要求：

✅ **核心功能**：PR 变更总结、风险识别、Review 建议  
✅ **准确性**：双通道融合 + 置信度评分  
✅ **上下文理解**：三级上下文获取策略  
✅ **误报控制**：反馈校准 + 动态阈值  
✅ **响应速度**：异步处理 + 模型分层  
✅ **使用体验**：RESTful API + Swagger UI  
✅ **设计思路**：详细的架构文档和扩展规划  

项目采用 DDD + 六边形架构，代码结构清晰，易于维护和扩展。通过模型路由、上下文分层、置信度评分等机制，在准确性、速度和成本之间取得了良好的平衡。
