<template>
  <section>
    <header class="row-head">
      <h1>审计日志</h1>
    </header>

    <div class="filter-bar">
      <div class="filters">
        <input v-model="tenantId" placeholder="租户ID" />
        <input v-model="action" placeholder="操作类型" />
        <button @click="reloadFirstPage" :disabled="loading">{{ loading ? "加载中..." : "查询" }}</button>
      </div>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>租户ID</th>
          <th>操作类型</th>
          <th>操作对象</th>
          <th>变更明细</th>
          <th>操作时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.tenant_id }}</td>
          <td>{{ item.action }}</td>
          <td>{{ item.target_type }} / {{ item.target_id }}</td>
          <td class="mono">{{ item.detail_json }}</td>
          <td>{{ item.created_at }}</td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无日志</p>

    <footer class="pager">
      <button @click="prevPage" :disabled="page <= 1 || loading">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="page >= totalPages || loading">下一页</button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { adminRequest } from "../utils/http";

interface AuditItem {
  id: number;
  tenant_id: number | null;
  action: string;
  target_type: string | null;
  target_id: string | null;
  detail_json: string | null;
  created_at: string;
}

interface AuditPageData {
  list: AuditItem[];
  total: number;
}

const loading = ref(false);
const errorMessage = ref("");
const tenantId = ref("");
const action = ref("");
const items = ref<AuditItem[]>([]);
const page = ref(1);
const pageSize = 20;
const total = ref(0);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function loadAudit() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const query = new URLSearchParams({
      page: String(page.value),
      page_size: String(pageSize),
    });
    if (tenantId.value) {
      query.set("tenant_id", tenantId.value);
    }
    if (action.value) {
      query.set("action", action.value);
    }

    const data = await adminRequest<AuditPageData>(`/api/v1/admin/audit?${query.toString()}`);
    items.value = data.list;
    total.value = data.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

function reloadFirstPage() {
  page.value = 1;
  void loadAudit();
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    void loadAudit();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    void loadAudit();
  }
}

onMounted(() => {
  void loadAudit();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h1 {
  color: hsl(var(--foreground));
  font-size: 24px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 8px;
}

.filter-bar {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filters input {
  padding: 8px 12px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--muted));
  color: hsl(var(--foreground));
  font-size: 14px;
}

.table {
  margin-top: 12px;
  width: 100%;
  border-collapse: collapse;
  background: hsl(var(--card));
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid hsl(var(--border));
}

thead {
  background: hsl(var(--secondary));
}

th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-bottom: 2px solid hsl(var(--border));
}

td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  color: hsl(var(--foreground));
  border-bottom: 1px solid hsl(var(--border) / 0.5);
}

tbody tr:hover {
  background: hsl(var(--accent));
}

tbody tr:last-child td {
  border-bottom: none;
}

.mono {
  font-family: "Consolas", monospace;
  max-width: 360px;
  word-break: break-all;
}

.pager {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
}

.pager span {
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 12px;
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
}

.empty {
  color: hsl(var(--muted-foreground));
  text-align: center;
  padding: 48px 24px;
  font-size: 14px;
}
</style>