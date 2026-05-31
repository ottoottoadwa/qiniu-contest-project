#!/bin/bash
# 设置数据库连接信息（请根据实际情况修改）
export DB_URL="jdbc:mysql://localhost:3306/prreview?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="root123"

# 可选：设置AI相关的API密钥
# export DASHSCOPE_API_KEY="your_dashscope_key"
# export GITHUB_TOKEN="your_github_token"

cd prreview-web
mvn spring-boot:run
