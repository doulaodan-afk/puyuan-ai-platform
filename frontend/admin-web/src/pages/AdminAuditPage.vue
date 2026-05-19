<template>
  <section>
    <header class="row-head">
      <h1>审计日志</h1>
      <div class="filters">
        <input v-model="tenantId" placeholder="tenant_id" />
        <input v-model="action" placeholder="action" />
        <button @click="reloadFirstPage" :disabled="loading">{{ loading ? "加载中..." : "查询" }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>tenant_id</th>
          <th>action</th>
          <th>target</th>
          <th>detail_json</th>
          <th>created_at</th>
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

.filters {
  display: flex;
  gap: 8px;
}

.table {
  margin-top: 12px;
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th,
td {
  border: 1px solid #e5ebf8;
  padding: 8px;
  text-align: left;
  font-size: 13px;
}

.mono {
  font-family: "Consolas", monospace;
  max-width: 360px;
  word-break: break-all;
}

.pager {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
}

button {
  border: none;
  border-radius: 6px;
  padding: 6px 10px;
  background: #2e5fd7;
  color: #fff;
}

button:disabled {
  opacity: 0.6;
}

.error {
  color: #c83a28;
}

.empty {
  color: #5c6a82;
  margin-top: 12px;
}
</style>