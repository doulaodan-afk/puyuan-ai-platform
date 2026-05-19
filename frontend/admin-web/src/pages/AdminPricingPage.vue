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
}

.panel {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
}

.form-grid {
  display: grid;
  gap: 10px;
  max-width: 520px;
}

label {
  display: grid;
  gap: 6px;
}

input {
  border: 1px solid #d7deea;
  border-radius: 6px;
  padding: 8px 10px;
}

button {
  border: none;
  border-radius: 6px;
  padding: 8px 10px;
  background: #2e5fd7;
  color: #fff;
}

.error {
  color: #c83a28;
}
</style>