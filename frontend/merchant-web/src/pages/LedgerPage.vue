<template>
  <section>
    <header class="row-head">
      <h1>消费明细</h1>
      <div class="filters">
        <select v-model="entryType" @change="reloadFirstPage">
          <option value="">全部</option>
          <option value="debit">扣费</option>
          <option value="recharge">充值</option>
          <option value="refund">退款</option>
          <option value="adjust">调账</option>
        </select>
        <button @click="loadLedger" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>流水号</th>
          <th>类型</th>
          <th>方向</th>
          <th>Token</th>
          <th>金额</th>
          <th>发生时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.biz_no">
          <td>{{ item.biz_no }}</td>
          <td>{{ item.entry_type }}</td>
          <td>{{ item.direction }}</td>
          <td>{{ item.token_amount }}</td>
          <td>{{ item.cash_amount }}</td>
          <td>{{ item.occurred_at }}</td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无数据</p>

    <footer class="pager">
      <button @click="prevPage" :disabled="page <= 1 || loading">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="page >= totalPages || loading">下一页</button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { merchantRequest } from "../utils/http";

interface LedgerItem {
  biz_no: string;
  entry_type: string;
  direction: string;
  token_amount: number;
  cash_amount: string;
  occurred_at: string;
}

interface LedgerPageData {
  list: LedgerItem[];
  page: number;
  page_size: number;
  total: number;
}

const loading = ref(false);
const errorMessage = ref("");
const entryType = ref("");
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const items = ref<LedgerItem[]>([]);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function loadLedger() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const query = new URLSearchParams({
      page: String(page.value),
      page_size: String(pageSize),
    });
    if (entryType.value) {
      query.set("entry_type", entryType.value);
    }

    const data = await merchantRequest<LedgerPageData>(`/api/v1/account/ledger?${query.toString()}`);
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
  void loadLedger();
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    void loadLedger();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    void loadLedger();
  }
}

onMounted(() => {
  void loadLedger();
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
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
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