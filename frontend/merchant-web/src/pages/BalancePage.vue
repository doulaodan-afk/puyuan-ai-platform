<template>
  <section>
    <header class="row-head">
      <h1>账户余额</h1>
      <button @click="loadAll" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <article class="card" v-if="balance">
      <p><strong>Token 余额：</strong>{{ balance.token_balance }}</p>
      <p><strong>存储已用：</strong>{{ balance.storage_used_gb }} GB</p>
      <p><strong>免费额度：</strong>{{ balance.storage_free_quota_gb }} GB</p>
      <p><strong>到期日期：</strong>{{ balance.expire_date }}</p>
      <div class="actions">
        <RouterLink to="/account/recharge">去充值</RouterLink>
        <RouterLink to="/account/ledger">查看流水</RouterLink>
      </div>
    </article>

    <!-- 存储统计概览 -->
    <section class="storage-section" v-if="storageOverview">
      <h2>存储统计</h2>
      <div class="storage-cards">
        <article class="storage-card">
          <h3>标准存储</h3>
          <p class="value">{{ formatSize(storageOverview.standard_space_gb) }} GB</p>
          <p class="sub">文件数: {{ storageOverview.standard_count }}</p>
        </article>
        <article class="storage-card">
          <h3>低频存储</h3>
          <p class="value">{{ formatSize(storageOverview.line_space_gb) }} GB</p>
        </article>
        <article class="storage-card">
          <h3>归档存储</h3>
          <p class="value">{{ formatSize(storageOverview.archive_space_gb) }} GB</p>
        </article>
        <article class="storage-card">
          <h3>外网流出流量</h3>
          <p class="value">{{ formatSize(storageOverview.blob_io_flux_gb) }} GB</p>
        </article>
      </div>
      <p class="storage-meta">Bucket: {{ storageOverview.bucket }} | 查询范围: {{ storageOverview.query_range }}</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { merchantRequest } from "../utils/http";
import { getMerchantStorageOverview, type MerchantStorageOverview } from "../api/ossStatistics";

interface BalanceData {
  token_balance: number;
  storage_used_gb: number;
  storage_free_quota_gb: number;
  expire_date: string;
}

const loading = ref(false);
const errorMessage = ref("");
const balance = ref<BalanceData | null>(null);
const storageOverview = ref<MerchantStorageOverview | null>(null);

function formatSize(gb: number): string {
  if (gb < 0.001) return "0";
  if (gb < 1) return gb.toFixed(3);
  if (gb < 100) return gb.toFixed(2);
  return gb.toFixed(1);
}

async function loadAll() {
  loading.value = true;
  errorMessage.value = "";
  try {
    balance.value = await merchantRequest<BalanceData>("/api/v1/account/balance");
    storageOverview.value = await getMerchantStorageOverview();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadAll();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card {
  margin-top: 12px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 14px;
  line-height: 1.8;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 12px;
}

.actions a {
  color: hsl(var(--primary));
  text-decoration: none;
  padding: 6px 10px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s ease;
}

.actions a:hover {
  background: hsl(var(--accent));
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.error {
  color: hsl(var(--destructive));
}

.storage-section {
  margin-top: 20px;
}

.storage-section h2 {
  color: hsl(var(--foreground));
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.storage-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
}

.storage-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 12px;
}

.storage-card h3 {
  margin: 0 0 6px 0;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
  font-weight: 500;
}

.storage-card .value {
  font-size: 20px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
}

.storage-card .sub {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  margin: 4px 0 0 0;
}

.storage-meta {
  margin-top: 8px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}
</style>