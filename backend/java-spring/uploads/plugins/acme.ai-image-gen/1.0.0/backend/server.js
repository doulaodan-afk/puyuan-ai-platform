/**
 * AI 商品图生成插件 - 独立后端服务
 *
 * 遵循 PLUGIN_API_SPECIFICATION.md 规范：
 * - 统一响应格式 { code, data, token_used, balance_remaining, message }
 * - 请求头：X-Tenant-Id, Authorization: Bearer <token>
 * - /health 健康检查端点
 */

const http = require('http');
const url = require('url');

const PORT = process.env.PORT || 3001;
const MOCK_IMAGE_URL = process.env.MOCK_IMAGE_URL || 'https://picsum.photos/seed/puyuan';

// 模拟租户余额存储（生产环境中由平台网关管理）
const tenantBalances = {};

// Token 消耗计算
function calculateTokenCost(size) {
  const baseCost = 10;
  switch (size) {
    case '1024x1024': return baseCost * 2;
    case '1792x1024':
    case '1024x1792': return baseCost * 3;
    case '512x512': return baseCost;
    default: return baseCost * 2;
  }
}

// 生成模拟图片 URL
function generateMockImageUrl(prompt, size) {
  const seed = prompt ? prompt.charCodeAt(0) % 1000 : Math.floor(Math.random() * 1000);
  const [w, h] = size.split('x').map(Number);
  return `https://picsum.photos/seed/${seed}/${w}/${h}`;
}

// 统一错误响应
function errorResponse(res, code, httpStatus, message) {
  res.writeHead(httpStatus, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, X-Tenant-Id, Authorization',
  });

  const body = JSON.stringify({
    code,
    data: null,
    token_used: 0,
    balance_remaining: 0,
    message,
  });
  res.end(body);
}

// 统一成功响应
function okResponse(res, data, tokenUsed, balanceRemaining, message = 'success') {
  res.writeHead(200, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, X-Tenant-Id, Authorization',
  });

  const body = JSON.stringify({
    code: 0,
    data,
    token_used: tokenUsed,
    balance_remaining: balanceRemaining,
    message,
  });
  res.end(body);
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // CORS 预检
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, X-Tenant-Id, Authorization',
    });
    res.end();
    return;
  }

  // ---- 健康检查端点 (/health) ----
  if (pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', version: '1.0.0', uptime: Math.floor(process.uptime()) }));
    return;
  }

  // ---- 主调用接口 (/) ----
  if (pathname === '/' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      // 解析请求头
      const tenantId = req.headers['x-tenant-id'] || 'anonymous';
      const auth = req.headers['authorization'] || '';

      // 解析请求体
      let payload;
      try {
        payload = JSON.parse(body);
      } catch {
        return errorResponse(res, 40001, 400, 'Invalid JSON body');
      }

      const { prompt, image_size: imageSize = '1024x1024' } = payload;

      if (!prompt || typeof prompt !== 'string' || prompt.trim().length === 0) {
        return errorResponse(res, 40001, 400, 'prompt is required and must be a non-empty string');
      }

      // 计算 Token 消耗
      const tokenUsed = calculateTokenCost(imageSize);

      // 模拟余额扣减
      const currentBalance = tenantBalances[tenantId] || 10000;
      const balanceRemaining = currentBalance - tokenUsed;
      tenantBalances[tenantId] = balanceRemaining;

      // 生成图片
      const imageUrl = generateMockImageUrl(prompt, imageSize);

      const result = {
        image_url: imageUrl,
        image_size: imageSize,
        prompt_hash: Buffer.from(prompt).toString('base64').slice(0, 16),
      };

      console.log(`[ai-image-gen] tenant=${tenantId} prompt_len=${prompt.length} size=${imageSize} token=${tokenUsed} balance=${balanceRemaining}`);

      return okResponse(res, result, tokenUsed, balanceRemaining, 'Image generated successfully');
    });
    return;
  }

  // ---- 路径不存在 ----
  errorResponse(res, 40401, 404, 'Not Found: ' + pathname);
});

server.listen(PORT, () => {
  console.log(`AI Image Gen Plugin backend running on port ${PORT}`);
});