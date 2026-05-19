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
        <p>预计到账 Token：{{ amount * 2000 }}</p>
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
        "Idempotency-Key": `recharge-create-${crypto.randomUUID()}`,
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
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
}

.panel {
  margin-top: 12px;
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
  border: 1px dashed #d8e0f0;
  border-radius: 8px;
  padding: 10px;
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

.paid {
  color: #1f8b4c;
}

.error {
  color: #c83a28;
}

.empty {
  color: #5c6a82;
}
</style>