<template>
  <section>
    <header class="row-head">
      <h1>充值中心</h1>
      <button @click="loadOrders" :disabled="loading">{{ loading ? "加载中..." : "刷新订单" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <section class="plan-grid">
      <article v-for="amount in amounts" :key="amount" class="plan-card">
        <h3>{{ amount }} 元</h3>
        <p>预计到账 Token：{{ amount * tokenRatio }}</p>
        <button @click="createOrder(amount)" :disabled="creating">{{ creating ? "创建中..." : "立即充值" }}</button>
      </article>
    </section>

    <section class="panel">
      <h2>充值订单</h2>
      <ul v-if="orders.length > 0" class="order-list">
        <li v-for="item in orders" :key="item.order_no">
          <div>
            <strong>{{ item.order_no }}</strong>
            <p>{{ item.amount }} 元 / {{ item.token_grant }} Token / {{ item.pay_status }}</p>
          </div>
          <button v-if="item.pay_status !== 'paid'" @click="confirmPaid(item.order_no)" :disabled="confirmingOrderNo === item.order_no">
            {{ confirmingOrderNo === item.order_no ? "确认中..." : "模拟支付成功" }}
          </button>
          <span v-else class="paid">已到账</span>
        </li>
      </ul>
      <p v-else class="empty">暂无订单</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { buildMerchantHeaders, merchantRequest } from "../utils/http";
import { generateUUID } from "../utils/uuid";

interface RechargeOrderItem {
  order_no: string;
  amount: number;
  token_grant: number;
  pay_status: string;
}

interface RechargeOrderData {
  order_no: string;
}

interface RechargeOrderListData {
  list: RechargeOrderItem[];
}

const amounts = [50, 100, 200, 500];
const loading = ref(false);
const creating = ref(false);
const confirmingOrderNo = ref("");
const errorMessage = ref("");
const orders = ref<RechargeOrderItem[]>([]);
const tokenRatio = ref(10); // 默认 1:10，从后端动态获取

async function loadPricing() {
  try {
    const data = await merchantRequest<{ token_ratio: number }>("/api/v1/account/pricing");
    if (data?.token_ratio && data.token_ratio > 0) {
      tokenRatio.value = data.token_ratio;
    }
  } catch (error) {
    console.warn("获取定价配置失败，使用默认兑换率 1:10:", error);
  }
}

async function loadOrders() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await merchantRequest<RechargeOrderListData>("/api/v1/account/recharge/orders?page=1&page_size=20");
    orders.value = data.list;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载订单失败";
  } finally {
    loading.value = false;
  }
}

async function createOrder(amount: number) {
  creating.value = true;
  errorMessage.value = "";
  try {
    const response = await fetch("/api/v1/account/recharge/orders", {
      method: "POST",
      headers: {
        ...buildMerchantHeaders({ "Content-Type": "application/json" }),
        "Idempotency-Key": `recharge-create-${generateUUID()}`,
      },
      body: JSON.stringify({ amount, pay_channel: "wechat_pay" }),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = (await response.json()) as { code: number; message: string; data: RechargeOrderData };
    if (payload.code !== 0) {
      throw new Error(payload.message || "创建订单失败");
    }

    await confirmPaid(payload.data.order_no);
    await loadOrders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "创建订单失败";
  } finally {
    creating.value = false;
  }
}

async function confirmPaid(orderNo: string) {
  confirmingOrderNo.value = orderNo;
  errorMessage.value = "";
  try {
    const response = await fetch(`/api/v1/account/recharge/orders/${orderNo}/confirm`, {
      method: "POST",
      headers: {
        ...buildMerchantHeaders({ "Content-Type": "application/json" }),
        "Idempotency-Key": `recharge-confirm-${orderNo}`,
      },
      body: JSON.stringify({
        pay_txn_no: `mvp-${Date.now()}`,
        pay_result: "paid",
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = (await response.json()) as { code: number; message: string };
    if (payload.code !== 0) {
      throw new Error(payload.message || "确认支付失败");
    }

    await loadOrders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "确认支付失败";
  } finally {
    confirmingOrderNo.value = "";
  }
}

onMounted(() => {
  void loadPricing();
  void loadOrders();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.plan-card,
.panel {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 12px;
}

.plan-card h3 {
  color: hsl(var(--foreground));
}

.plan-card p {
  color: hsl(var(--muted-foreground));
}

.panel {
  margin-top: 12px;
}

.panel h2 {
  color: hsl(var(--foreground));
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.order-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px dashed hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 10px;
}

.order-list li strong {
  color: hsl(var(--foreground));
}

.order-list li p {
  color: hsl(var(--muted-foreground));
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  cursor: pointer;
  transition: opacity 0.2s ease;
}

button:hover:not(:disabled) {
  opacity: 0.9;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.paid {
  color: hsl(142, 71%, 45%);
  font-weight: 500;
}

.error {
  color: hsl(var(--destructive));
}

.empty {
  color: hsl(var(--muted-foreground));
}
</style>