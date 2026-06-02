<template>
  <section>
    <header class="row-head">
      <h1>租户管理</h1>
      <div class="filters">
        <input v-model.trim="keyword" placeholder="租户编码/名称" @keyup.enter="reloadFirstPage" />
        <button @click="reloadFirstPage" :disabled="loading">{{ loading ? "加载中..." : "查询" }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>编码</th>
          <th>名称</th>
          <th>状态</th>
          <th>套餐</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.tenant_id">
          <td>{{ item.tenant_id }}</td>
          <td>{{ item.tenant_code }}</td>
          <td>{{ item.tenant_name }}</td>
          <td>{{ item.status === 1 ? "正常" : "冻结" }}</td>
          <td>
            <select :value="item.level" @change="updateLevel(item.tenant_id, ($event.target as HTMLSelectElement).value)">
              <option value="basic">basic</option>
              <option value="vip">vip</option>
              <option value="enterprise">enterprise</option>
            </select>
          </td>
          <td>
            <button v-if="item.status === 1" @click="freezeTenant(item.tenant_id)">冻结</button>
            <button v-else @click="unfreezeTenant(item.tenant_id)">解冻</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无租户</p>

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

interface TenantItem {
  tenant_id: number;
  tenant_code: string;
  tenant_name: string;
  status: number;
  level: string;
}

interface TenantPageData {
  list: TenantItem[];
  page: number;
  page_size: number;
  total: number;
}

const loading = ref(false);
const errorMessage = ref("");
const keyword = ref("");
const items = ref<TenantItem[]>([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function loadTenants() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const query = new URLSearchParams({
      page: String(page.value),
      page_size: String(pageSize),
    });
    if (keyword.value) {
      query.set("keyword", keyword.value);
    }

    const data = await adminRequest<TenantPageData>(`/api/v1/admin/tenants?${query.toString()}`);
    items.value = data.list;
    total.value = data.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function freezeTenant(tenantId: number) {
  try {
    await adminRequest(`/api/v1/admin/tenants/${tenantId}/freeze`, { method: "POST" });
    await loadTenants();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "冻结失败";
  }
}

async function unfreezeTenant(tenantId: number) {
  try {
    await adminRequest(`/api/v1/admin/tenants/${tenantId}/unfreeze`, { method: "POST" });
    await loadTenants();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "解冻失败";
  }
}

async function updateLevel(tenantId: number, level: string) {
  try {
    await adminRequest(`/api/v1/admin/tenants/${tenantId}/level`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ level }),
    });
    await loadTenants();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "更新套餐失败";
  }
}

function reloadFirstPage() {
  page.value = 1;
  void loadTenants();
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    void loadTenants();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    void loadTenants();
  }
}

onMounted(() => {
  void loadTenants();
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