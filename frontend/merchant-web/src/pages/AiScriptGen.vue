<template>
  <section class="ai-tool">
    <header class="row-head">
      <h1>AI 视频脚本生成</h1>
      <span class="balance-badge">Token 余额: {{ balance.toLocaleString() }}</span>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="form-card">
      <div class="form-group">
        <label for="productDesc">商品描述</label>
        <textarea
          id="productDesc"
          v-model="productDesc"
          placeholder="详细描述您的商品..."
          rows="6"
        ></textarea>
      </div>

      <div class="form-group">
        <label for="productUrl">商品链接（可选）</label>
        <input id="productUrl" v-model="productUrl" type="url" placeholder="https://..." />
      </div>

      <div class="form-group">
        <label for="scriptType">脚本类型</label>
        <select id="scriptType" v-model="scriptType">
          <option value="video">短视频脚本</option>
          <option value="live">直播话术</option>
          <option value="detail">详情页文案</option>
        </select>
      </div>

      <div class="token-info">
        <span>本次消耗:</span>
        <span class="token-cost">{{ tokenCost }} Tokens</span>
      </div>

      <button @click="generateScript" :disabled="loading || !productDesc.trim()">
        {{ loading ? "生成中..." : "生成脚本" }}
      </button>
    </div>

    <div v-if="result" class="result-card">
      <div class="result-header">
        <h3>生成结果</h3>
        <button class="copy-btn" @click="copyScript">复制</button>
      </div>
      <pre class="script-content">{{ result.script }}</pre>
      <div class="result-meta">
        <div class="meta-item">
          <span class="label">类型:</span>
          <span class="value">{{ getScriptTypeName(result.script_type) }}</span>
        </div>
        <div class="meta-item">
          <span class="label">消耗:</span>
          <span class="value">{{ result.token_used }} Tokens</span>
        </div>
        <div class="meta-item">
          <span class="label">剩余:</span>
          <span class="value">{{ result.balance_remaining.toLocaleString() }} Tokens</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { merchantRequest } from "../utils/http";

const productDesc = ref("");
const productUrl = ref("");
const scriptType = ref("video");
const loading = ref(false);
const errorMessage = ref("");
const balance = ref(0);
const result = ref<{
  script: string;
  script_type: string;
  token_used: number;
  balance_remaining: number;
} | null>(null);

const tokenCost = 20;

function getScriptTypeName(type: string): string {
  const types: Record<string, string> = {
    video: "短视频脚本",
    live: "直播话术",
    detail: "详情页文案",
  };
  return types[type] || type;
}

async function loadBalance() {
  try {
    const data = await merchantRequest<{ token_balance: number }>("/api/v1/account/balance");
    balance.value = data.token_balance;
  } catch (error) {
    console.error("Failed to load balance:", error);
  }
}

async function generateScript() {
  if (!productDesc.value.trim()) {
    errorMessage.value = "请输入商品描述";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  result.value = null;

  try {
    const data = await merchantRequest<typeof result.value>(
      `/api/plugin/invoke/ai_script_gen`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          product_desc: productDesc.value,
          product_url: productUrl.value || undefined,
          script_type: scriptType.value,
        }),
      }
    );
    result.value = data;
    if (data) balance.value = data.balance_remaining;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "生成失败";
  } finally {
    loading.value = false;
  }
}

async function copyScript() {
  if (!result.value) return;
  await navigator.clipboard.writeText(result.value.script);
}

onMounted(() => {
  console.log("[AiScriptGen] mounted");
  void loadBalance();
});
</script>

<style scoped>
.ai-tool {
  padding: 0;
}

.row-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

@media (min-width: 640px) {
  .row-head {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.row-head h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.balance-badge {
  display: inline-block;
  padding: 8px 16px;
  background: hsl(var(--success) / 0.2);
  color: hsl(var(--success));
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.error {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.form-card,
.result-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.form-group textarea,
.form-group input,
.form-group select {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  font-size: 15px;
  resize: vertical;
  box-sizing: border-box;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
}

.form-group input {
  resize: none;
}

.form-group textarea:focus,
.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: hsl(var(--primary));
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.2);
}

.token-info {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  border-top: 1px solid hsl(var(--border));
  border-bottom: 1px solid hsl(var(--border));
  margin-bottom: 16px;
}

.token-info span {
  font-size: 14px;
}

.token-cost {
  font-weight: 600;
}

.form-card button {
  width: 100%;
  padding: 14px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 2px);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
}

.form-card button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-header h3 {
  font-size: 16px;
  margin: 0;
}

.copy-btn {
  padding: 6px 12px;
  background: hsl(var(--muted-foreground));
  color: hsl(var(--background));
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.script-content {
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 16px;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
}

.result-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid hsl(var(--border));
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item .label {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

.meta-item .value {
  font-size: 14px;
  font-weight: 500;
}
</style>