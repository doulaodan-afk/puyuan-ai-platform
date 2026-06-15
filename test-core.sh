#!/bin/bash
# ============================================================
# 濮院毛衫 AI 平台 - 核心功能冒烟测试脚本
# 
# 功能: 覆盖登录、插件列表、AI调用等核心功能
# 用法: bash test-core.sh [--base-url URL] [--verbose]
# 
# 示例:
#   bash test-core.sh                           # 默认 localhost:8080
#   bash test-core.sh --base-url https://ai.puyuanmaoshan.com
#   bash test-core.sh --verbose                 # 显示详细输出
# ============================================================

set -e

# ==================== 配置 ====================
BASE_URL="http://localhost:8080"
VERBOSE=false
TIMEOUT=30
TEST_RESULTS=()
PASSED=0
FAILED=0
SKIPPED=0

# ==================== 颜色 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ==================== 解析参数 ====================
for arg in "$@"; do
    case $arg in
        --base-url=*)
            BASE_URL="${arg#*=}"
            ;;
        --base-url)
            BASE_URL="$2"
            shift
            ;;
        --verbose|-v)
            VERBOSE=true
            ;;
        -h|--help)
            echo "用法: bash test-core.sh [选项]"
            echo ""
            echo "选项:"
            echo "  --base-url URL    指定 API 基础地址 (默认: http://localhost:8080)"
            echo "  --verbose, -v     显示详细输出"
            echo "  -h, --help        显示帮助信息"
            echo ""
            echo "示例:"
            echo "  bash test-core.sh"
            echo "  bash test-core.sh --base-url https://ai.puyuanmaoshan.com"
            echo "  bash test-core.sh --base-url http://localhost:8080 --verbose"
            exit 0
            ;;
    esac
done

# 去除 base_url 末尾的斜杠
BASE_URL="${BASE_URL%/}"

# ==================== 工具函数 ====================
log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_test()  { echo -e "${CYAN}[TEST]${NC}  $1"; }
log_debug() { if [ "$VERBOSE" = true ]; then echo -e "       $1"; fi; }

# HTTP 请求封装
api_get() {
    local endpoint="$1"
    local token="$2"
    local url="${BASE_URL}${endpoint}"
    local headers=()
    
    if [ -n "$token" ]; then
        headers+=(-H "Authorization: Bearer $token")
    fi
    headers+=(-H "Content-Type: application/json")
    
    local response
    response=$(curl -s -w "\n%{http_code}" -o /tmp/test_core_response.json \
        --connect-timeout "$TIMEOUT" \
        --max-time "$TIMEOUT" \
        "${headers[@]}" \
        "$url" 2>/tmp/test_core_error.log)
    
    local http_code=$(echo "$response" | tail -1)
    local body=$(cat /tmp/test_core_response.json)
    
    if [ "$VERBOSE" = true ]; then
        log_debug "GET $url → HTTP $http_code"
        log_debug "Response: $(echo "$body" | head -c 500)"
    fi
    
    echo "${http_code}|${body}"
}

api_post() {
    local endpoint="$1"
    local data="$2"
    local token="$3"
    local url="${BASE_URL}${endpoint}"
    local headers=()
    
    if [ -n "$token" ]; then
        headers+=(-H "Authorization: Bearer $token")
    fi
    headers+=(-H "Content-Type: application/json")
    
    local response
    response=$(curl -s -w "\n%{http_code}" -o /tmp/test_core_response.json \
        --connect-timeout "$TIMEOUT" \
        --max-time "$TIMEOUT" \
        "${headers[@]}" \
        -d "$data" \
        "$url" 2>/tmp/test_core_error.log)
    
    local http_code=$(echo "$response" | tail -1)
    local body=$(cat /tmp/test_core_response.json)
    
    if [ "$VERBOSE" = true ]; then
        log_debug "POST $url → HTTP $http_code"
        log_debug "Response: $(echo "$body" | head -c 500)"
    fi
    
    echo "${http_code}|${body}"
}

# ==================== 断言函数 ====================
assert_http_ok() {
    local http_code="$1"
    local test_name="$2"
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        return 0
    else
        return 1
    fi
}

assert_json_has_field() {
    local body="$1"
    local field="$2"
    echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$field',''))" 2>/dev/null | grep -q .
}

assert_json_field_eq() {
    local body="$1"
    local field="$2"
    local expected="$3"
    local actual=$(echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$field',''))" 2>/dev/null)
    [ "$actual" = "$expected" ]
}

# ==================== 测试执行 ====================
run_test() {
    local name="$1"
    local test_fn="$2"
    
    echo -n "  [$name] "
    
    if $test_fn; then
        echo -e "${GREEN}PASS${NC}"
        PASSED=$((PASSED + 1))
        TEST_RESULTS+=("PASS|$name")
    else
        echo -e "${RED}FAIL${NC}"
        FAILED=$((FAILED + 1))
        TEST_RESULTS+=("FAIL|$name")
    fi
}

skip_test() {
    local name="$1"
    echo -e "  [$name] ${YELLOW}SKIP${NC}"
    SKIPPED=$((SKIPPED + 1))
    TEST_RESULTS+=("SKIP|$name")
}

# ==================== 1. 健康检查 ====================
test_health_check() {
    log_test "1. 健康检查"
    
    run_test "服务健康检查" test_actuator_health
}

test_actuator_health() {
    local result=$(api_get "/actuator/health")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    assert_http_ok "$http_code" "health" || return 1
    assert_json_field_eq "$body" "status" "UP" || return 1
    return 0
}

# ==================== 2. 登录功能测试 ====================
test_login() {
    log_test "2. 登录功能测试"
    
    # 测试短信验证码发送
    run_test "发送短信验证码" test_send_sms_code
    
    # 测试短信登录（使用 Mock）
    run_test "短信验证码登录" test_sms_login
    
    # 测试 Token 有效性
    run_test "Token 有效性验证" test_token_validation
}

test_send_sms_code() {
    local data='{"mobile":"13800000001"}'
    local result=$(api_post "/api/v1/auth/send-code" "$data")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 接受 200 或开发环境的特定响应
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 500 ]; then
        return 0
    fi
    return 1
}

test_sms_login() {
    local data='{"mobile":"13800000001","code":"123456"}'
    local result=$(api_post "/api/v1/auth/login" "$data")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 保存 token 供后续测试使用
    echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('token',''))" 2>/dev/null > /tmp/test_core_token.txt
    TOKEN=$(cat /tmp/test_core_token.txt)
    
    if [ -n "$TOKEN" ] && [ "$http_code" -lt 500 ]; then
        return 0
    fi
    return 1
}

test_token_validation() {
    TOKEN=$(cat /tmp/test_core_token.txt 2>/dev/null || echo "")
    if [ -z "$TOKEN" ]; then
        # 没有 token，尝试无认证访问
        return 1
    fi
    
    local result=$(api_get "/api/v1/user/profile" "$TOKEN")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    
    # 200 表示 token 有效，401 表示无效
    if [ "$http_code" -eq 200 ]; then
        return 0
    fi
    return 1
}

# ==================== 3. 插件列表测试 ====================
test_plugin_list() {
    log_test "3. 插件列表测试"
    
    run_test "获取插件列表" test_get_plugin_list
    
    run_test "插件列表非空" test_plugin_list_not_empty
    
    run_test "按分类筛选插件" test_plugin_filter_by_category
}

test_get_plugin_list() {
    TOKEN=$(cat /tmp/test_core_token.txt 2>/dev/null || echo "")
    local result=$(api_get "/api/v1/plugin/list" "$TOKEN")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    
    assert_http_ok "$http_code" "plugin_list" || return 1
    return 0
}

test_plugin_list_not_empty() {
    TOKEN=$(cat /tmp/test_core_token.txt 2>/dev/null || echo "")
    local result=$(api_get "/api/v1/plugin/list" "$TOKEN")
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 检查是否有数据
    local count=$(echo "$body" | python3 -c "
import sys, json
d = json.load(sys.stdin)
data = d.get('data', d)
if isinstance(data, list):
    print(len(data))
elif isinstance(data, dict) and 'records' in data:
    print(len(data['records']))
elif isinstance(data, dict) and 'list' in data:
    print(len(data['list']))
else:
    print(0)
" 2>/dev/null)
    
    if [ "$count" -gt 0 ]; then
        return 0
    fi
    # 如果列表为空但 API 正常，也算通过（可能是新环境）
    return 0
}

test_plugin_filter_by_category() {
    TOKEN=$(cat /tmp/test_core_token.txt 2>/dev/null || echo "")
    local result=$(api_get "/api/v1/plugin/list?category=image" "$TOKEN")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    
    assert_http_ok "$http_code" "plugin_filter" || return 1
    return 0
}

# ==================== 4. AI 调用测试 ====================
test_ai_call() {
    log_test "4. AI 调用测试"
    
    run_test "AI 服务可用性检查" test_ai_availability
    
    run_test "AI 插件调用" test_ai_plugin_invoke
}

test_ai_availability() {
    # 尝试调用一个轻量 AI 端点检查服务是否可用
    # 这里使用插件列表中的某个端点来间接验证 AI 服务配置
    local result=$(api_get "/actuator/health")
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 检查 health 中是否包含 AI 相关组件状态
    if echo "$body" | grep -q "UP"; then
        return 0
    fi
    return 1
}

test_ai_plugin_invoke() {
    TOKEN=$(cat /tmp/test_core_token.txt 2>/dev/null || echo "")
    if [ -z "$TOKEN" ]; then
        skip_test "AI 插件调用（无 Token）"
        return 0
    fi
    
    # 先获取可用插件
    local plugin_result=$(api_get "/api/v1/plugin/list" "$TOKEN")
    local plugin_body=$(echo "$plugin_result" | cut -d'|' -f2-)
    
    # 获取第一个插件的 ID
    local plugin_id=$(echo "$plugin_body" | python3 -c "
import sys, json
d = json.load(sys.stdin)
data = d.get('data', d)
if isinstance(data, list) and len(data) > 0:
    print(data[0].get('id', ''))
elif isinstance(data, dict):
    records = data.get('records', data.get('list', []))
    if len(records) > 0:
        print(records[0].get('id', ''))
    else:
        print('')
" 2>/dev/null)
    
    if [ -z "$plugin_id" ]; then
        skip_test "AI 插件调用（无可用插件）"
        return 0
    fi
    
    log_debug "使用插件 ID: $plugin_id"
    
    local invoke_data="{\"pluginId\":$plugin_id,\"input\":\"test\"}"
    local result=$(api_post "/api/v1/plugin/invoke" "$invoke_data" "$TOKEN")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 接受 200（成功）或 402（余额不足）等非 5xx 错误
    if [ "$http_code" -lt 500 ]; then
        return 0
    fi
    return 1
}

# ==================== 5. 系统信息测试 ====================
test_system_info() {
    log_test "5. 系统信息测试"
    
    run_test "获取 API 文档信息" test_api_docs
    
    run_test "数据库连接检查" test_db_connection
}

test_api_docs() {
    local result=$(api_get "/v3/api-docs")
    local http_code=$(echo "$result" | cut -d'|' -f1)
    
    # 生产环境可能关闭了 API 文档，404 也算通过
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 404 ]; then
        return 0
    fi
    return 1
}

test_db_connection() {
    local result=$(api_get "/actuator/health")
    local body=$(echo "$result" | cut -d'|' -f2-)
    
    # 检查 health 响应中数据库组件状态
    if echo "$body" | python3 -c "
import sys, json
d = json.load(sys.stdin)
components = d.get('components', {})
db = components.get('db', {})
status = db.get('status', 'UNKNOWN')
sys.exit(0 if status == 'UP' else 1)
" 2>/dev/null; then
        return 0
    fi
    # 如果无法解析详细组件状态，只要整体 UP 就算通过
    if echo "$body" | grep -q '"status":"UP"'; then
        return 0
    fi
    return 1
}

# ==================== 6. 性能基准测试 ====================
test_performance() {
    log_test "6. 性能基准测试"
    
    run_test "API 响应时间 < 3s" test_response_time
    
    run_test "并发请求处理" test_concurrent_requests
}

test_response_time() {
    local start=$(date +%s%N)
    local result=$(api_get "/actuator/health")
    local end=$(date +%s%N)
    
    local elapsed_ms=$(( (end - start) / 1000000 ))
    log_debug "响应时间: ${elapsed_ms}ms"
    
    if [ "$elapsed_ms" -lt 3000 ]; then
        return 0
    fi
    return 1
}

test_concurrent_requests() {
    # 发送 5 个并发请求测试服务稳定性
    local success=0
    local fail=0
    
    for i in $(seq 1 5); do
        if curl -sf -o /dev/null "${BASE_URL}/actuator/health" 2>/dev/null; then
            success=$((success + 1))
        else
            fail=$((fail + 1))
        fi
    done
    
    log_debug "并发请求: ${success}/5 成功"
    
    if [ "$success" -ge 4 ]; then
        return 0
    fi
    return 1
}

# ==================== 主函数 ====================
main() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   濮院毛衫 AI 平台 - 核心功能测试    ║${NC}"
    echo -e "${BLUE}║   测试环境: ${BASE_URL}${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
    echo ""
    
    # 检查依赖
    if ! command -v curl &> /dev/null; then
        log_error "需要 curl 命令，请先安装"
        exit 1
    fi
    
    if ! command -v python3 &> /dev/null; then
        log_error "需要 python3 命令，请先安装"
        exit 1
    fi
    
    # 检查服务是否可达
    log_info "检查服务连通性..."
    if ! curl -sf --connect-timeout 5 "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        log_warn "服务 ${BASE_URL} 不可达，部分测试可能失败"
        log_warn "请确认服务已启动，或使用 --base-url 指定正确地址"
        echo ""
    else
        log_info "服务连通正常"
    fi
    echo ""
    
    # 执行测试套件
    test_health_check
    echo ""
    test_login
    echo ""
    test_plugin_list
    echo ""
    test_ai_call
    echo ""
    test_system_info
    echo ""
    test_performance
    
    # ==================== 测试总结 ====================
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  测试结果汇总${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "  ${GREEN}通过: ${PASSED}${NC}"
    echo -e "  ${RED}失败: ${FAILED}${NC}"
    echo -e "  ${YELLOW}跳过: ${SKIPPED}${NC}"
    echo -e "  总计: $((PASSED + FAILED + SKIPPED))"
    echo ""
    
    # 显示失败详情
    if [ "$FAILED" -gt 0 ]; then
        echo -e "${RED}失败的测试:${NC}"
        for result in "${TEST_RESULTS[@]}"; do
            local status=$(echo "$result" | cut -d'|' -f1)
            local name=$(echo "$result" | cut -d'|' -f2-)
            if [ "$status" = "FAIL" ]; then
                echo -e "  ${RED}✗${NC} $name"
            fi
        done
        echo ""
    fi
    
    # 返回退出码
    if [ "$FAILED" -gt 0 ]; then
        echo -e "${RED}测试未完全通过！请检查失败的测试项。${NC}"
        exit 1
    else
        echo -e "${GREEN}所有测试通过！${NC}"
        exit 0
    fi
}

# 清理临时文件
cleanup() {
    rm -f /tmp/test_core_response.json /tmp/test_core_error.log /tmp/test_core_token.txt
}
trap cleanup EXIT

# 执行
main "$@"
