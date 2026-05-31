# AI PR Review Assistant

基于 Spring Boot 3.4 + Spring AI 1.0 的智能 Pull Request 代码审查助手，使用七牛云大模型 API 提供 AI 驱动的代码审查服务。

## 项目简介

这是一个 REST API 后端服务，提供以下核心功能：

- **AI 代码审查**：使用七牛云大模型分析代码变更，提供智能审查意见
- **多维度分析**：代码质量、安全性、性能、最佳实践等多角度评估
- **RESTful API**：标准化的 API 接口，易于集成
- **Swagger UI**：内置 API 文档和测试界面

## 技术栈

- **Java 21**
- **Spring Boot 3.4.1**
- **Spring AI 1.0.0**：集成七牛云大模型
- **Maven**：项目构建工具
- **H2 Database**：内存数据库（开发/测试）
- **SpringDoc OpenAPI**：API 文档生成

## 项目结构

```
prreview/
├── prreview-domain/          # 领域模型层
├── prreview-application/     # 应用服务层
├── prreview-infrastructure/  # 基础设施层
└── prreview-web/            # Web 接口层
```

采用 DDD（领域驱动设计）分层架构，清晰的职责划分。

## 前置要求

- **Java 21** 或更高版本
- **Maven 3.6+**
- **七牛云大模型 API Key**

## 快速开始

### 1. 配置 API Key

编辑 `prreview-web/src/main/resources/application.yml`，填入你的七牛云 API 密钥：

```yaml
spring:
  ai:
    qianfan:
      api-key: your-api-key-here
      secret-key: your-secret-key-here
```

### 2. 构建项目

```bash
mvn clean install
```

### 3. 启动服务

```bash
cd prreview-web
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

### 4. 访问 Swagger UI

打开浏览器访问：

```
http://localhost:8080/swagger-ui.html
```

在 Swagger UI 中可以：
- 查看所有 API 接口文档
- 直接测试 API 接口
- 查看请求/响应示例

## API 使用示例

### 创建代码审查任务

**POST** `/api/reviews`

```json
{
  "repositoryUrl": "https://github.com/user/repo",
  "pullRequestNumber": 123,
  "diffContent": "diff --git a/src/Main.java...",
  "reviewOptions": {
    "checkSecurity": true,
    "checkPerformance": true,
    "checkBestPractices": true
  }
}
```

### 查询审查结果

**GET** `/api/reviews/{reviewId}`

返回详细的审查报告，包括：
- 代码质量评分
- 发现的问题列表
- 改进建议
- 安全风险提示

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

### 添加新功能

1. 在 `prreview-domain` 中定义领域模型
2. 在 `prreview-application` 中实现业务逻辑
3. 在 `prreview-infrastructure` 中实现技术细节
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
export QIANFAN_API_KEY=your-api-key
export QIANFAN_SECRET_KEY=your-secret-key
java -jar prreview-web-0.1.0-SNAPSHOT.jar
```

## 常见问题

### Q: 没有前端界面吗？

A: 这是一个纯后端 REST API 项目。你可以：
- 使用 Swagger UI（`http://localhost:8080/swagger-ui.html`）进行测试
- 使用 Postman 或 curl 调用 API
- 自行开发前端界面集成此 API

### Q: 如何更换其他 AI 模型？

A: 修改 `application.yml` 中的 Spring AI 配置，Spring AI 支持多种模型提供商。

### Q: 数据存储在哪里？

A: 默认使用 H2 内存数据库。生产环境建议配置 PostgreSQL 或 MySQL。

## 许可证

MIT License

## 联系方式

如有问题或建议，欢迎提交 Issue。
