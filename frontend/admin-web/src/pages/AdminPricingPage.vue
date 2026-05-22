<template>
  <section>
    <header class="row-head">
      <h1>定价管理</h1>
      <button @click="loadConfig" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <form v-if="form" class="panel form-grid" @submit.prevent="saveConfig">
      <label>
        token_price_per_1k
        <input v-model.number="form.token_price_per_1k" type="number" step="0.0001" />
      </label>
      <label>
        storage_price_per_gb_month
        <input v-model.number="form.storage_price_per_gb_month" type="number" step="0.0001" />
      </label>
      <label>
        free_token_quota_month
        <input v-model.number="form.free_token_quota_month" type="number" step="1" />
      </label>
      <label>
        free_storage_quota_gb
        <input v-model.number="form.free_storage_quota_gb" type="number" step="0.1" />
      </label>
      <button type="submit" :disabled="saving">{{ saving ? "保存中..." : "保存" }}</button>
    </form>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminRequest } from "../utils/http";

interface PricingConfig {
  token_price_per_1k: number;
  storage_price_per_gb_month: number;
  free_token_quota_month: number;
  free_storage_quota_gb: number;
}

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const form = ref<PricingConfig | null>(null);

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
  try {
    await adminRequest<PricingConfig>("/api/v1/admin/pricing", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form.value),
    });
    await loadConfig();  // # MEMORY: Reload config after save to reflect changes
  } catch (error) {
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
</style>