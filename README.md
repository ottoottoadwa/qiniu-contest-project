# AI PR Review Assistant

基于 Spring Boot 3.4 + Spring AI 1.0 的智能 Pull Request 代码审查助手，使用阿里云通义千问大模型 API 提供 AI 驱动的代码审查服务。

## 项目简介

这是一个 REST API 后端服务，提供以下核心功能：

- **AI 代码审查**：使用阿里云通义千问大模型分析代码变更，提供智能审查意见
- **GitHub Bot 集成**：自动审查新建或更新的 PR，也支持 `/review` 命令手动触发
- **事件驱动架构**：基于 Spring Events 的异步结果发布，无轮询开销
- **多维度分析**：代码质量、安全性、性能、最佳实践等多角度评估
- **实时进度跟踪**：支持查询审查任务的实时进度和状态
- **GitHub 集成**：直接从 GitHub 拉取 PR 信息进行分析
- **RESTful API**：标准化的 API 接口，易于集成
- **Swagger UI**：内置 API 文档和测试界面

## 技术栈

- **Java 21**
- **Spring Boot 3.4.1**
- **Spring AI 1.0.0**：集成阿里云通义千问大模型
- **Maven**：项目构建工具
- **MySQL 8.0**：生产数据库
- **Flyway**：数据库版本管理
- **SpringDoc OpenAPI**：API 文档生成

## 项目结构

```
prreview/
├── prreview-domain/          # 领域模型层
├── prreview-application/     # 应用服务层
├── prreview-infrastructure/  # 基础设施层（AI、GitHub、数据库）
└── prreview-web/            # Web 接口层
```

采用 DDD（领域驱动设计）分层架构，清晰的职责划分。

## 前置要求

- **Java 21** 或更高版本
- **Maven 3.6+**
- **MySQL 8.0+**
- **阿里云 DashScope API Key**（通义千问）
- **GitHub Personal Access Token**（用于访问 GitHub API）

## 快速开始

### 1. 配置环境变量

**Windows:**

```bash
set aliQwen_api=your-dashscope-api-key
set GITHUB_TOKEN=your-github-token
```

**Linux/Mac:**

```bash
export aliQwen_api=your-dashscope-api-key
export GITHUB_TOKEN=your-github-token
```

**获取 API Key:**
- 阿里云 DashScope API Key: https://dashscope.console.aliyun.com/apiKey
- GitHub Token: https://github.com/settings/tokens (需要 `repo` 权限)

### 2. 配置数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE prreview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

编辑 `prreview-web/src/main/resources/application.yml`，配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prreview?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: your-username
    password: your-password
```

### 3. 构建并启动

**Windows (一键启动):**

```bash
build-and-start-prod.bat
```

或分步执行：

```bash
# 构建项目
mvn clean package -DskipTests

# 启动服务
start-prod.bat
```

**Linux/Mac:**

```bash
# 构建项目
mvn clean package -DskipTests

# 启动服务
cd prreview-web
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

### 4. 配置 GitHub Webhook（可选，用于自动审查）

如果需要使用 GitHub Bot 自动审查功能：

**使用 ngrok 暴露本地服务：**

```bash
# 启动 ngrok（另开一个终端）
start-ngrok.bat

# 或直接运行
ngrok http 8080
```

**配置 GitHub Webhook：**

1. 进入你的 GitHub 仓库 → Settings → Webhooks → Add webhook
2. 配置：
   - **Payload URL**: `https://your-ngrok-url/api/webhook/github`
   - **Content type**: `application/json`
   - **Events**: 选择 `Pull requests` 和 `Issue comments`
3. 保存

现在，每次创建或更新 PR 时，Bot 会自动进行审查！

### 5. 访问 Swagger UI

打开浏览器访问：

```
http://localhost:8080/swagger-ui.html
```

在 Swagger UI 中可以：
- 查看所有 API 接口文档
- 直接测试 API 接口
- 查看请求/响应示例

## GitHub Bot 使用

### 自动审查模式

Bot 会自动审查以下事件：
- ✅ 新 PR 创建时 (`opened` 事件)
- ✅ PR 更新时 (`synchronize` 事件)

无需任何手动操作，Bot 会自动：
1. 检测到 PR 事件
2. 发布 "审查已启动" 评论
3. 执行完整的代码审查
4. 发布详细的审查结果

### 手动触发模式

在任何 PR 的评论中输入：

```
/review
```

Bot 会：
1. 回复 👀 表情确认
2. 发布 "审查已启动" 评论
3. 执行审查并发布结果

### Bot 智能检测

Bot 会自动忽略来自其他 bot 的评论，防止无限循环：
- 检测 GitHub 用户类型为 `Bot`
- 检测用户名以 `[bot]` 结尾

详细配置请参考 [GITHUB_BOT.md](./GITHUB_BOT.md)

## API 使用示例

### 提交代码审查任务

**POST** `/api/reviews/v1`

请求头：
```
X-API-Key: dev-api-key-change-in-production
Content-Type: application/json
```

请求体：
```json
{
  "repository": "owner/repo-name",
  "pullRequestNumber": 1
}
```

响应：
```json
{
  "reviewId": "uuid",
  "status": "PENDING",
  "statusUrl": "/api/reviews/v1/{reviewId}/status",
  "submittedAt": "2024-01-01T12:00:00Z"
}
```

### 查询审查进度

**GET** `/api/reviews/v1/{reviewId}/status`

响应：
```json
{
  "reviewId": "uuid",
  "status": "RUNNING",
  "progress": 0.65,
  "filesTotal": 26,
  "filesAnalyzed": 17,
  "startedAt": "2024-01-01T12:00:00Z",
  "estimatedRemainingSeconds": 10,
  "resultUrl": null
}
```

状态说明：
- `PENDING`: 等待处理
- `RUNNING`: 正在分析
- `COMPLETED`: 分析完成
- `FAILED`: 分析失败

### 获取审查结果

**GET** `/api/reviews/v1/{reviewId}`

返回详细的审查报告，包括：
- PR 变更摘要
- 风险项列表（按严重程度分类）
- 每个风险项的详细描述和改进建议
- 置信度评分

## 测试

### 运行所有测试

```bash
mvn test
```

### 运行特定模块测试

```bash
cd prreview-application
mvn test
```

## 开发说明

### 项目架构

采用六边形架构（端口-适配器模式）+ 事件驱动设计：

- **Domain Layer**: 核心业务逻辑，不依赖外部框架
- **Application Layer**: 用例编排，协调领域对象，发布领域事件
- **Infrastructure Layer**: 技术实现（数据库、AI、GitHub API）
- **Web Layer**: REST API 接口、Webhook 处理、事件监听器

### 关键组件

1. **DirectQwenClient**: 直接调用阿里云 Qwen API，绕过 Spring AI 的超时问题
2. **ReviewOrchestrator**: 审查流程编排器，管理整个审查生命周期
3. **AnalysisEngine**: 分析引擎，并行处理文件分析和风险识别
4. **ProgressCallback**: 进度回调机制，实时更新审查进度
5. **ReviewCompletedListener**: 事件监听器，异步发布审查结果到 GitHub
6. **WebhookService**: 处理 GitHub webhook 事件，防止 bot 循环

### 添加新功能

1. 在 `prreview-domain` 中定义领域模型和端口接口
2. 在 `prreview-application` 中实现用例逻辑
3. 在 `prreview-infrastructure` 中实现适配器
4. 在 `prreview-web` 中暴露 REST API

### 代码规范

- 使用 Lombok 减少样板代码
- 遵循 DDD 分层原则
- 编写单元测试和集成测试
- 使用 ArchUnit 验证架构规则

## 部署

### 打包应用

```bash
mvn clean package
```

生成的 JAR 文件位于 `prreview-web/target/prreview-web-0.1.0-SNAPSHOT.jar`

### 运行 JAR

```bash
java -jar prreview-web/target/prreview-web-0.1.0-SNAPSHOT.jar
```

### 环境变量配置

生产环境建议使用环境变量配置敏感信息：

```bash
export aliQwen_api=your-api-key
export GITHUB_TOKEN=your-github-token
export SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/prreview
export SPRING_DATASOURCE_USERNAME=your-username
export SPRING_DATASOURCE_PASSWORD=your-password
java -jar prreview-web-0.1.0-SNAPSHOT.jar
```

### Docker 部署（可选）

```bash
# 构建镜像
docker build -t prreview:latest .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e aliQwen_api=your-api-key \
  -e GITHUB_TOKEN=your-github-token \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/prreview \
  prreview:latest
```

## 故障排除

### AI 调用超时

如果遇到 AI 调用超时问题，项目已经实现了以下解决方案：

1. **DirectQwenClient**: 绕过 Spring AI，直接使用 RestClient 调用 API
2. **HTTP 超时配置**: 连接超时 10 秒，读取超时 60 秒
3. **单例 RestClient**: 避免连接池耗尽

### 进度不更新

项目使用 `REQUIRES_NEW` 事务传播确保进度实时提交到数据库。如果仍有问题：

1. 检查数据库连接是否正常
2. 查看应用日志中的 "Progress updated" 消息
3. 确认没有长时间运行的事务阻塞

### GitHub API 限流

GitHub API 有速率限制。如果遇到 403 错误：

1. 确保使用了有效的 Personal Access Token
2. 检查 token 的权限范围（需要 `repo` 权限）
3. 等待速率限制重置（通常是每小时）

### Webhook 未触发

如果 Bot 没有自动审查：

1. 检查 GitHub Webhook 配置页面的 "Recent Deliveries"
2. 查看响应状态码和错误信息
3. 确认 ngrok 或公网地址可访问
4. 检查应用日志中的 webhook 事件

### Bot 发布多条评论

如果 Bot 重复发布评论：

1. 检查是否有多个 webhook 配置
2. 确认没有重复触发审查
3. 查看日志中的重复检测信息

## 常见问题

### Q: 没有前端界面吗？

A: 这是一个纯后端 REST API 项目。你可以：
- 使用 Swagger UI（`http://localhost:8080/swagger-ui.html`）进行测试
- 使用 Postman 或 curl 调用 API
- 自行开发前端界面集成此 API

### Q: 支持哪些编程语言的代码审查？

A: 理论上支持所有编程语言，因为使用的是通用的大语言模型。实际效果取决于：
- 代码的复杂度
- 模型对该语言的训练程度
- 提供的上下文信息

### Q: 如何更换其他 AI 模型？

A: 可以通过以下方式：
1. 修改 `application.yml` 中的模型配置
2. 实现新的 `ChatModelPort` 适配器
3. Spring AI 支持多种模型提供商（OpenAI、Azure、Anthropic 等）

### Q: 数据存储在哪里？

A: 生产环境使用 MySQL 数据库存储：
- 审查任务元数据
- 分析结果
- 风险项和建议

### Q: 审查一个 PR 需要多长时间？

A: 取决于多个因素：
- PR 的文件数量（每个文件约 5-10 秒）
- 发现的风险项数量（每个建议约 10-15 秒）
- 网络延迟
- AI API 的响应速度

典型的 20-30 个文件的 PR 大约需要 3-5 分钟。

## 性能优化建议

1. **并行处理**: 项目使用虚拟线程并行分析文件
2. **事件驱动**: 使用 Spring Events 异步发布结果，无轮询开销
3. **缓存**: 可以添加 Redis 缓存重复的分析结果
4. **批量处理**: 对于大型 PR，可以考虑分批处理
5. **异步处理**: 所有审查任务都是异步执行的

## 贡献指南

欢迎提交 Issue 和 Pull Request！

在提交 PR 之前，请确保：
1. 代码通过所有测试
2. 遵循项目的代码规范
3. 更新相关文档
4. 添加必要的测试用例

## 许可证

MIT License

## 致谢

- Spring AI 团队提供的优秀框架
- 阿里云通义千问提供的强大 AI 能力
- GitHub 提供的开放 API

## 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。
