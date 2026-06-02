<template>
  <section class="ai-tool">
    <header class="row-head">
      <h1>AI 图片生成</h1>
      <span class="balance-badge">Token 余额: {{ balance.toLocaleString() }}</span>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="form-card">
      <div class="form-group">
        <label for="prompt">提示词</label>
        <textarea
          id="prompt"
          v-model="prompt"
          placeholder="描述您想要生成的图片..."
          rows="4"
        ></textarea>
      </div>

      <div class="form-group">
        <label for="imageSize">图片尺寸</label>
        <select id="imageSize" v-model="imageSize">
          <option value="512x512">512x512 (10 Tokens)</option>
          <option value="1024x1024">1024x1024 (20 Tokens)</option>
          <option value="1792x1024">1792x1024 (30 Tokens)</option>
          <option value="1024x1792">1024x1792 (30 Tokens)</option>
        </select>
      </div>

      <div class="token-info">
        <span>本次消耗:</span>
        <span class="token-cost">{{ tokenCost }} Tokens</span>
      </div>

      <button @click="generateImage" :disabled="loading || !prompt.trim()">
        {{ loading ? "生成中..." : "生成图片" }}
      </button>
    </div>

    <div v-if="result" class="result-card">
      <h3>生成结果</h3>
      <img :src="result.image_url" alt="生成的图片" class="result-image" />
      <div class="result-meta">
        <div class="meta-item">
          <span class="label">尺寸:</span>
          <span class="value">{{ result.image_size }}</span>
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
import { ref, onMounted, computed } from "vue";
import { merchantRequest } from "../utils/http";

const prompt = ref("");
const imageSize = ref("1024x1024");
const loading = ref(false);
const errorMessage = ref("");
const balance = ref(0);
const result = ref<{
  image_url: string;
  image_size: string;
  token_used: number;
  balance_remaining: number;
} | null>(null);

const tokenCost = computed(() => {
  const costs: Record<string, number> = {
    "512x512": 10,
    "1024x1024": 20,
    "1792x1024": 30,
    "1024x1792": 30,
  };
  return costs[imageSize.value] || 20;
});

async function loadBalance() {
  try {
    const data = await merchantRequest<{ token_balance: number }>("/api/v1/account/balance");
    balance.value = data.token_balance;
  } catch (error) {
    console.error("Failed to load balance:", error);
  }
}

async function generateImage() {
  if (!prompt.value.trim()) {
    errorMessage.value = "请输入提示词";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  result.value = null;

  try {
    const data = await merchantRequest<typeof result.value>(
      `/api/plugin/invoke/ai_image_gen`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: prompt.value, image_size: imageSize.value }),
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

onMounted(() => {
  console.log("[AiImageGen] mounted");
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

.result-card h3 {
  font-size: 16px;
  margin: 0 0 16px 0;
}

.result-image {
  width: 100%;
  height: auto;
  border-radius: calc(var(--radius) - 2px);
  border: 1px solid hsl(var(--border));
  margin-bottom: 16px;
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