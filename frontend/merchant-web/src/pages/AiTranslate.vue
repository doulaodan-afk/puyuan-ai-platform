<template>
  <section class="ai-tool">
    <header class="row-head">
      <h1>AI 跨境翻译</h1>
      <span class="balance-badge">Token 余额: {{ balance.toLocaleString() }}</span>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="form-card">
      <div class="form-group">
        <label for="text">待翻译文本</label>
        <textarea
          id="text"
          v-model="text"
          placeholder="输入需要翻译的中文文本..."
          rows="6"
          @input="updateTokenCost"
        ></textarea>
      </div>

      <div class="form-group">
        <label for="targetLang">目标语言</label>
        <select id="targetLang" v-model="targetLang">
          <option value="en">英语</option>
          <option value="th">泰语</option>
          <option value="vi">越南语</option>
          <option value="ms">马来语</option>
          <option value="id">印尼语</option>
        </select>
      </div>

      <div class="token-info">
        <span>本次消耗:</span>
        <span class="token-cost">{{ tokenCost }} Tokens</span>
      </div>

      <button @click="translate" :disabled="loading || !text.trim()">
        {{ loading ? "翻译中..." : "翻译" }}
      </button>
    </div>

    <div v-if="result" class="result-card">
      <div class="result-header">
        <h3>翻译结果</h3>
        <button class="copy-btn" @click="copyResult">复制</button>
      </div>
      <div class="result-content">
        <div class="translation-block">
          <span class="lang-tag">中文</span>
          <p>{{ text }}</p>
        </div>
        <div class="arrow-down">↓</div>
        <div class="translation-block">
          <span class="lang-tag">{{ getLangName(result.target_lang) }}</span>
          <p>{{ result.translated_text }}</p>
        </div>
      </div>
      <div class="result-meta">
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

const text = ref("");
const targetLang = ref("en");
const loading = ref(false);
const errorMessage = ref("");
const balance = ref(0);
const result = ref<{
  translated_text: string;
  target_lang: string;
  source_lang: string;
  token_used: number;
  balance_remaining: number;
} | null>(null);

const tokenCost = ref(0);

function getLangName(code: string): string {
  const langs: Record<string, string> = {
    en: "English",
    th: "泰语",
    vi: "越南语",
    ms: "马来语",
    id: "印尼语",
  };
  return langs[code] || code;
}

function updateTokenCost() {
  const textLength = text.value.length;
  tokenCost.value = Math.max(5, Math.ceil(textLength / 10));
}

async function loadBalance() {
  try {
    const data = await merchantRequest<{ token_balance: number }>("/api/v1/account/balance");
    balance.value = data.token_balance;
  } catch (error) {
    console.error("Failed to load balance:", error);
  }
}

async function translate() {
  if (!text.value.trim()) {
    errorMessage.value = "请输入待翻译文本";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  result.value = null;

  try {
    const data = await merchantRequest<typeof result.value>(
      `/api/plugin/invoke/ai_translate`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          text: text.value,
          target_lang: targetLang.value,
        }),
      }
    );
    result.value = data;
    if (data) balance.value = data.balance_remaining;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "翻译失败";
  } finally {
    loading.value = false;
  }
}

async function copyResult() {
  if (!result.value) return;
  await navigator.clipboard.writeText(result.value.translated_text);
}

onMounted(() => {
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

.form-group textarea:focus,
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

.result-content {
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 16px;
  margin-bottom: 16px;
}

.translation-block {
  margin-bottom: 12px;
}

.translation-block:last-child {
  margin-bottom: 0;
}

.lang-tag {
  display: inline-block;
  padding: 2px 8px;
  background: hsl(var(--border));
  color: hsl(var(--muted-foreground));
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 6px;
}

.translation-block p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

.arrow-down {
  text-align: center;
  color: hsl(var(--muted-foreground));
  font-size: 18px;
  margin: 8px 0;
}

.result-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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