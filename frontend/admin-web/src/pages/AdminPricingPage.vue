<template>
  <section>
    <header class="row-head">
      <h1>定价管理</h1>
      <button @click="loadConfig" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>

    <div class="info-box">
      <p><strong>说明：</strong></p>
      <ul>
        <li>此处管理全局 Token 定价配置，保存后即时生效</li>
        <li>充值兑换率 = 1 元可兑换的 Token 数量</li>
        <li>扣费记账单价由兑换率自动推导（1 / 兑换率），无需手动填写</li>
        <li>存储定价请前往 <strong>"对象存储"</strong> Tab 配置</li>
      </ul>
    </div>

    <form v-if="form" class="panel form-grid" @submit.prevent="saveConfig">
      <label>
        充值兑换率（1元 = N Token）
        <input v-model.number="form.token_ratio" type="number" step="1" min="1" />
        <span class="hint">当前：1 元 = {{ form.token_ratio }} Token</span>
      </label>

      <div class="derived-field">
        <span class="derived-label">扣费记账单价（自动计算）</span>
        <span class="derived-value">1 Token = {{ derivedCashPerToken }} 元</span>
        <span class="hint">公式：1 ÷ 兑换率 = 1 ÷ {{ form.token_ratio }} ≈ {{ derivedCashPerToken }} 元</span>
      </div>

      <div class="divider"></div>

      <label>
        新用户注册赠送 Token
        <input v-model.number="form.register_bonus_token" type="number" step="1" min="0" />
        <span class="hint">新用户注册时钱包初始 Token 数量</span>
      </label>

      <div class="preview-box">
        <p><strong>价格预览：</strong></p>
        <ul>
          <li>充值 50 元 → {{ 50 * (form.token_ratio || 0) }} Token</li>
          <li>充值 100 元 → {{ 100 * (form.token_ratio || 0) }} Token</li>
          <li>充值 200 元 → {{ 200 * (form.token_ratio || 0) }} Token</li>
          <li>消耗 10 Token → {{ (10 * derivedCashPerTokenNum).toFixed(2) }} 元</li>
        </ul>
      </div>

      <button type="submit" :disabled="saving">{{ saving ? "保存中..." : "保存" }}</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { adminRequest } from "../utils/http";

interface PricingConfig {
  register_bonus_token: number;
  token_ratio: number;
  cash_per_token: number;
}

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const form = ref<PricingConfig | null>(null);

// 扣费记账单价由充值兑换率自动推导：1 / token_ratio
const derivedCashPerToken = computed(() => {
  if (!form.value || !form.value.token_ratio || form.value.token_ratio <= 0) return "0.00";
  return (1 / form.value.token_ratio).toFixed(4);
});

const derivedCashPerTokenNum = computed(() => {
  if (!form.value || !form.value.token_ratio || form.value.token_ratio <= 0) return 0;
  return 1 / form.value.token_ratio;
});

async function loadConfig() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await adminRequest<PricingConfig>("/api/v1/admin/pricing");
    console.log("Pricing config loaded:", data);
    form.value = data;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
    console.error("Load pricing config error:", error);
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  if (!form.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    const payload = {
      register_bonus_token: form.value.register_bonus_token,
      token_ratio: form.value.token_ratio,
      cash_per_token: derivedCashPerTokenNum.value,
    };
    console.log("Saving pricing config:", payload);
    await adminRequest<PricingConfig>("/api/v1/admin/pricing", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    successMessage.value = "保存成功，新配置已生效";
    // 同步更新 form 中的 cash_per_token，确保数据一致性
    form.value.cash_per_token = derivedCashPerTokenNum.value;
    setTimeout(() => {
      successMessage.value = "";
    }, 3000);
  } catch (error) {
    console.error("Save pricing config error:", error);
    errorMessage.value = error instanceof Error ? error.message : "保存失败";
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  void loadConfig();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

h1 {
  color: hsl(var(--foreground));
  font-size: 24px;
  font-weight: 600;
}

.panel {
  margin-top: 16px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px;
  max-width: 600px;
}

.form-grid {
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--foreground));
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  font-weight: 400;
}

.derived-field {
  display: grid;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--foreground));
  padding: 10px 12px;
  background: hsl(var(--muted));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
}

.derived-label {
  font-weight: 500;
}

.derived-value {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--primary));
}

input {
  padding: 10px 12px;
  border: 1px solid hsl(var(--input));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 14px;
  transition: all 0.2s ease;
}

input:hover {
  border-color: hsl(var(--ring));
}

input:focus {
  outline: none;
  border-color: hsl(var(--ring));
  box-shadow: 0 0 0 3px hsl(var(--ring) / 0.1);
}

button {
  margin-top: 8px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 10px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:hover:not(:disabled) {
  background: hsl(240 8% 18%);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.success {
  color: hsl(142, 71%, 35%);
  background: hsl(142, 71%, 45% / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
  border: 1px solid hsl(142, 71%, 45% / 0.3);
}

.info-box {
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px;
  margin-bottom: 16px;
  max-width: 600px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}

.info-box p {
  margin: 0 0 8px;
  color: hsl(var(--foreground));
}

.info-box ul {
  margin: 0;
  padding-left: 20px;
}

.info-box li {
  margin-bottom: 4px;
}

.info-box strong {
  color: hsl(var(--primary));
}

.divider {
  height: 1px;
  background: hsl(var(--border));
  margin: 4px 0;
}

.preview-box {
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 12px 16px;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  line-height: 1.8;
}

.preview-box p {
  margin: 0 0 4px;
  color: hsl(var(--foreground));
  font-size: 14px;
}

.preview-box ul {
  margin: 0;
  padding-left: 20px;
}
</style>