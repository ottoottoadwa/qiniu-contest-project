#!/bin/bash

# AI PR Review Assistant - 快速功能测试脚本

API_URL="http://localhost:8080/api/reviews/v1"
API_KEY="dev-api-key-change-in-production"

echo "=========================================="
echo "AI PR Review Assistant - 功能测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 健康检查
echo "1. 检查应用健康状态..."
HEALTH=$(curl -s http://localhost:8080/actuator/health)
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✓ 应用运行正常${NC}"
else
    echo -e "${RED}✗ 应用未启动或异常${NC}"
    echo "请先启动应用: cd prreview-web && mvn spring-boot:run"
    exit 1
fi
echo ""

# 2. 提交快速扫描任务
echo "2. 提交 PR 审查任务（FAST 模式）..."
SUBMIT_RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: test-$(date +%s)" \
  -d '{
    "repository": "spring-projects/spring-boot",
    "pullRequestNumber": 1,
    "analysisProfile": "FAST",
    "riskCategories": ["SECURITY", "CORRECTNESS"]
  }')

REVIEW_ID=$(echo "$SUBMIT_RESPONSE" | grep -o '"reviewId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$REVIEW_ID" ]; then
    echo -e "${RED}✗ 提交失败${NC}"
    echo "响应: $SUBMIT_RESPONSE"
    exit 1
else
    echo -e "${GREEN}✓ 任务已提交${NC}"
    echo "Review ID: $REVIEW_ID"
fi
echo ""

# 3. 轮询任务状态
echo "3. 轮询任务状态..."
MAX_ATTEMPTS=30
ATTEMPT=0
STATUS="PENDING"

while [ "$STATUS" != "COMPLETED" ] && [ "$STATUS" != "FAILED" ] && [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))

    STATUS_RESPONSE=$(curl -s "$API_URL/$REVIEW_ID/status")
    STATUS=$(echo "$STATUS_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    PROGRESS=$(echo "$STATUS_RESPONSE" | grep -o '"progress":[0-9.]*' | cut -d':' -f2)

    echo -e "${YELLOW}[尝试 $ATTEMPT/$MAX_ATTEMPTS] 状态: $STATUS, 进度: ${PROGRESS}%${NC}"
done

if [ "$STATUS" = "COMPLETED" ]; then
    echo -e "${GREEN}✓ 分析完成${NC}"
else
    echo -e "${RED}✗ 分析超时或失败 (状态: $STATUS)${NC}"
    exit 1
fi
echo ""

# 4. 获取完整结果
echo "4. 获取审查结果..."
RESULT=$(curl -s "$API_URL/$REVIEW_ID")

# 解析结果
SUMMARY_HEADLINE=$(echo "$RESULT" | grep -o '"headline":"[^"]*"' | head -1 | cut -d'"' -f4)
RISK_COUNT=$(echo "$RESULT" | grep -o '"riskItems":\[' | wc -l)

echo -e "${GREEN}✓ 结果获取成功${NC}"
echo ""
echo "=========================================="
echo "审查结果摘要"
echo "=========================================="
echo "Review ID: $REVIEW_ID"
echo "变更总结: $SUMMARY_HEADLINE"
echo "风险项数量: $RISK_COUNT"
echo ""
echo "完整结果 JSON:"
echo "$RESULT" | jq '.' 2>/dev/null || echo "$RESULT"
echo ""

# 5. 测试幂等性
echo "5. 测试幂等性（使用相同 Idempotency-Key）..."
IDEMPOTENCY_KEY="test-idempotency-$(date +%s)"

FIRST_RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "repository": "owner/repo",
    "pullRequestNumber": 999,
    "analysisProfile": "FAST"
  }')

FIRST_ID=$(echo "$FIRST_RESPONSE" | grep -o '"reviewId":"[^"]*"' | cut -d'"' -f4)

sleep 1

SECOND_RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "repository": "owner/repo",
    "pullRequestNumber": 999,
    "analysisProfile": "FAST"
  }')

SECOND_ID=$(echo "$SECOND_RESPONSE" | grep -o '"reviewId":"[^"]*"' | cut -d'"' -f4)

if [ "$FIRST_ID" = "$SECOND_ID" ]; then
    echo -e "${GREEN}✓ 幂等性验证通过（两次请求返回相同 ID）${NC}"
    echo "Review ID: $FIRST_ID"
else
    echo -e "${RED}✗ 幂等性验证失败${NC}"
    echo "第一次: $FIRST_ID"
    echo "第二次: $SECOND_ID"
fi
echo ""

# 6. 测试输入验证
echo "6. 测试输入验证（无效的 repository 格式）..."
INVALID_RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "repository": "invalid-format",
    "pullRequestNumber": 1
  }')

if echo "$INVALID_RESPONSE" | grep -q "error\|validation\|must be in"; then
    echo -e "${GREEN}✓ 输入验证正常工作${NC}"
else
    echo -e "${YELLOW}⚠ 输入验证可能未生效${NC}"
fi
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "访问 Swagger UI 查看完整 API 文档:"
echo "http://localhost:8080/swagger-ui.html"
echo ""
echo "查看详细功能说明:"
echo "cat FEATURE_VERIFICATION.md"
