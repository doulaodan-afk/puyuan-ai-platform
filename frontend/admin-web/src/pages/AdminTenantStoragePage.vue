<template>
  <section class="page-container">
    <header class="row-head">
      <h1>对象存储管理</h1>
      <span class="subtitle">租户存储空间分配与计费管理（电表模式）</span>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>

    <!-- 凭证配置卡片 -->
    <div class="credential-card">
      <div class="credential-header" @click="showCredentialForm = !showCredentialForm">
        <div class="credential-status-line">
          <span class="credential-dot" :class="credentialStatus.configured ? 'dot-ok' : 'dot-none'"></span>
          <strong>七牛云凭证配置</strong>
          <span v-if="credentialStatus.configured" class="credential-sub">
            {{ credentialStatus.masked_access_key }}
            <template v-if="credentialStatus.last_updated_at"> · 上次更新：{{ credentialStatus.last_updated_at?.slice(0, 10) }}</template>
          </span>
          <span v-else class="credential-sub warn">未配置</span>
        </div>
        <span class="toggle-icon">{{ showCredentialForm ? '▲' : '▼' }}</span>
      </div>

      <div v-if="showCredentialForm" class="credential-body">
        <div class="form-grid">
          <div class="form-group">
            <label>AccessKey</label>
            <input v-model.trim="credForm.access_key" type="text" placeholder="填入七牛云 AccessKey" :disabled="credTesting" />
          </div>
          <div class="form-group">
            <label>SecretKey</label>
            <input v-model.trim="credForm.secret_key" type="password" placeholder="填入七牛云 SecretKey" :disabled="credTesting" />
          </div>
          <div class="form-group button-group">
            <button class="primary-btn" @click="handleTestCredentials" :disabled="credTesting || !credForm.access_key || !credForm.secret_key">
              {{ credTesting ? '测试中...' : '测试连接' }}
            </button>
            <button class="primary-btn save-btn" @click="handleSaveCredentials" :disabled="!credTestPassed || credSaving">
              {{ credSaving ? '保存中...' : '保存凭证' }}
            </button>
            <button class="secondary-btn" @click="loadCredentialStatus">刷新状态</button>
          </div>
        </div>

        <!-- 测试结果 -->
        <div v-if="credTestResult" class="test-result" :class="credTestResult.success ? 'test-ok' : 'test-fail'">
          <p class="test-msg">{{ credTestResult.success ? '✅' : '❌' }} {{ credTestResult.message }}</p>
          <p v-if="credTestResult.success" class="test-detail">
            延迟 {{ credTestResult.latency_ms }}ms · 共 {{ credTestResult.bucket_count }} 个 Bucket
          </p>
          <div v-if="credTestResult.buckets && credTestResult.buckets.length > 0" class="bucket-list">
            <span v-for="(b, i) in credTestResult.buckets" :key="i" class="bucket-tag">{{ b }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.value" :class="{ active: activeTab === tab.value }" @click="switchTab(tab.value)">
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab 1: 存储空间管理 -->
    <section v-if="activeTab === 'buckets'" class="panel">
      <div class="panel-header">
        <h2>存储空间列表（电表）</h2>
        <div class="panel-actions">
          <button @click="showCreateBucketForm = true" class="primary-btn">分配空间</button>
          <button @click="refreshBuckets" class="secondary-btn" :disabled="loading">刷新</button>
          <button @click="syncAllDomains" class="secondary-btn">同步域名</button>
        </div>
      </div>

      <!-- 创建空间表单 -->
      <div v-if="showCreateBucketForm" class="form-panel">
        <h3>创建存储空间</h3>
        <form class="form-grid" @submit.prevent="handleCreateBucket">
          <div class="form-group">
            <label>租户ID</label>
            <input v-model.number="bucketForm.tenant_id" type="number" placeholder="输入租户ID" required />
          </div>
          <div class="form-group">
            <label>Bucket名称（全局唯一）</label>
            <input v-model.trim="bucketForm.bucket_name" placeholder="如: tenant-123-store" required />
          </div>
          <div class="form-group">
            <label>存储区域</label>
            <select v-model="bucketForm.bucket_region">
              <option value="z0">华东 (z0)</option>
              <option value="z1">华北 (z1)</option>
              <option value="z2">华南 (z2)</option>
              <option value="na0">北美 (na0)</option>
              <option value="as0">东南亚 (as0)</option>
            </select>
          </div>
          <div class="form-group">
            <label>套餐</label>
            <select v-model.number="bucketForm.plan_id">
              <option :value="0">不绑定套餐</option>
              <option v-for="plan in plans" :key="plan.id" :value="plan.id">
                {{ plan.plan_name }} ({{ plan.storage_quota_gb }}GB / ¥{{ plan.base_price }}/月)
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>备注</label>
            <input v-model.trim="bucketForm.notes" placeholder="备注说明" />
          </div>
          <div class="form-group checkbox-group">
            <label>
              <input v-model="bucketForm.bucket_private" type="checkbox" />
              私有空间
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="primary-btn" :disabled="creating">创建</button>
            <button type="button" class="secondary-btn" @click="showCreateBucketForm = false">取消</button>
          </div>
        </form>
      </div>

      <!-- 空间列表 -->
      <div v-if="buckets.length > 0" class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>租户ID</th>
              <th>Bucket名称</th>
              <th>区域</th>
              <th>套餐</th>
              <th>用量/配额</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="bucket in buckets" :key="bucket.id">
              <td>{{ bucket.id }}</td>
              <td>{{ bucket.tenant_id }}</td>
              <td class="mono">{{ bucket.bucket_name }}</td>
              <td>{{ bucket.bucket_region_label }}</td>
              <td>
                <span v-if="bucket.plan_name" class="badge">{{ bucket.plan_name }}</span>
                <span v-else class="text-muted">未分配</span>
              </td>
              <td>
                <div class="usage-bar">
                  <div class="usage-label">{{ formatGb(bucket.storage_used_gb) }} / {{ formatGb(bucket.storage_quota_gb) }} GB</div>
                  <div class="bar">
                    <div class="bar-fill" :style="{ width: usagePercent(bucket) + '%' }" :class="usageClass(bucket)"></div>
                  </div>
                </div>
              </td>
              <td><span :class="'status-' + bucket.status">{{ statusLabel(bucket.status) }}</span></td>
              <td class="actions">
                <button @click="viewBucketDetail(bucket)" class="small-btn">详情</button>
                <button @click="showAssignPlan(bucket)" class="small-btn">套餐</button>
                <button @click="handleDeleteBucket(bucket)" class="small-btn danger">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">暂无存储空间，点击"分配空间"开始</p>
    </section>

    <!-- Tab 2: 套餐管理 -->
    <section v-if="activeTab === 'plans'" class="panel">
      <div class="panel-header">
        <h2>存储套餐</h2>
        <div class="panel-actions">
          <button @click="openPlanForm(null)" class="primary-btn">新增套餐</button>
        </div>
      </div>
      <div class="plan-grid">
        <article v-for="plan in plans" :key="plan.id" class="plan-card" :class="{ 'plan-highlight': plan.plan_level >= 3 }">
          <h3>{{ plan.plan_name }}</h3>
          <p class="plan-price">¥{{ plan.base_price }}<span class="unit">/月</span></p>
          <ul class="plan-features">
            <li v-for="(feat, i) in plan.features" :key="i">{{ feat }}</li>
            <li><strong>存储</strong>: {{ plan.storage_quota_gb }} GB</li>
            <li><strong>月流量</strong>: {{ plan.monthly_traffic_gb }} GB</li>
            <li><strong>GET请求</strong>: {{ formatNumber(plan.monthly_get_requests) }}次/月</li>
            <li><strong>超额存储</strong>: ¥{{ plan.storage_price_per_gb }}/GB/月</li>
            <li><strong>超额流量</strong>: ¥{{ plan.traffic_price_per_gb }}/GB</li>
          </ul>
          <div class="plan-meta">
            <span class="text-muted">{{ plan.tenant_count }} 个租户使用中</span>
          </div>
          <div class="plan-actions-row">
            <button @click="openPlanForm(plan)" class="small-btn">编辑</button>
            <button @click="handleDeletePlan(plan)" class="small-btn danger">删除</button>
          </div>
        </article>
      </div>
    </section>

    <!-- Tab 3: 计费管理 -->
    <section v-if="activeTab === 'billing'" class="panel">
      <div class="panel-header">
        <h2>计费管理</h2>
        <div class="panel-actions">
          <input v-model="billPeriod" type="month" class="date-input" />
          <button @click="loadBilling" class="primary-btn">查询</button>
          <button @click="handleCalculateAllBills" class="primary-btn" :disabled="billingLoading">批量出账</button>
        </div>
      </div>

      <div v-if="billingRecords.length > 0" class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>租户</th>
              <th>空间</th>
              <th>账单周期</th>
              <th>存储用量</th>
              <th>流量用量</th>
              <th>基础费</th>
              <th>超额费</th>
              <th>总费用</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in billingRecords" :key="record.id">
              <td>{{ record.tenant_name }}</td>
              <td class="mono">{{ record.bucket_name }}</td>
              <td>{{ record.bill_period }}</td>
              <td>{{ formatNum(record.standard_storage_gb) }} GB</td>
              <td>{{ formatNum(record.external_traffic_gb) }} GB</td>
              <td>¥{{ formatNum(record.base_fee) }}</td>
              <td>¥{{ formatNum(record.total_fee - record.base_fee) }}</td>
              <td class="fee-total">¥{{ formatNum(record.total_fee) }}</td>
              <td><span :class="'status-' + record.bill_status">{{ billStatusLabel(record.bill_status) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">暂无计费记录，选择月份后查询或点击"批量出账"</p>
    </section>

    <!-- Tab 4: 用量快照 -->
    <section v-if="activeTab === 'usage'" class="panel">
      <div class="panel-header">
        <h2>用量快照（抄表记录）</h2>
        <div class="panel-actions">
          <select v-model="snapshotBucketId" class="select-input">
            <option :value="0">选择空间</option>
            <option v-for="b in buckets" :key="b.id" :value="b.id">{{ b.bucket_name }}</option>
          </select>
          <input v-model="snapshotBegin" type="date" class="date-input" />
          <input v-model="snapshotEnd" type="date" class="date-input" />
          <button @click="loadSnapshots" class="primary-btn">查询</button>
          <button @click="handleSnapshotAll" class="secondary-btn" :disabled="snapshotLoading">批量抄表</button>
        </div>
      </div>

      <div v-if="snapshots.length > 0" class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>日期</th>
              <th>标准存储</th>
              <th>低频存储</th>
              <th>归档存储</th>
              <th>外网流量</th>
              <th>CDN流量</th>
              <th>GET请求</th>
              <th>PUT请求</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="snap in snapshots" :key="snap.id">
              <td>{{ snap.snapshot_date }}</td>
              <td>{{ formatGb(snap.standard_storage_gb) }} GB</td>
              <td>{{ formatGb(snap.line_storage_gb) }} GB</td>
              <td>{{ formatGb(snap.archive_storage_gb) }} GB</td>
              <td>{{ formatGb(snap.external_traffic_gb) }} GB</td>
              <td>{{ formatGb(snap.cdn_traffic_gb) }} GB</td>
              <td>{{ formatNumber(snap.get_requests) }}</td>
              <td>{{ formatNumber(snap.put_requests) }}</td>
              <td><span :class="'status-' + snap.fetch_status">{{ snap.fetch_status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">选择存储空间后查询用量快照数据</p>
    </section>

    <!-- 详情弹窗 -->
    <div v-if="selectedBucket" class="modal-overlay" @click="selectedBucket = null">
      <div class="modal large" @click.stop>
        <h3>{{ selectedBucket.bucket_name }} 详情</h3>
        <div class="detail-grid">
          <div class="detail-item"><span class="label">ID</span><span>{{ selectedBucket.id }}</span></div>
          <div class="detail-item"><span class="label">租户ID</span><span>{{ selectedBucket.tenant_id }}</span></div>
          <div class="detail-item"><span class="label">Bucket名称</span><span class="mono">{{ selectedBucket.bucket_name }}</span></div>
          <div class="detail-item"><span class="label">存储区域</span><span>{{ selectedBucket.bucket_region_label }} ({{ selectedBucket.bucket_region }})</span></div>
          <div class="detail-item"><span class="label">CDN域名</span><span>{{ selectedBucket.bucket_domain || '未配置' }}</span></div>
          <div class="detail-item"><span class="label">访问权限</span><span>{{ selectedBucket.bucket_private ? '私有' : '公开' }}</span></div>
          <div class="detail-item"><span class="label">套餐</span><span>{{ selectedBucket.plan_name || '无' }}</span></div>
          <div class="detail-item"><span class="label">配额</span><span>{{ selectedBucket.storage_quota_gb }} GB</span></div>
          <div class="detail-item"><span class="label">状态</span><span>{{ statusLabel(selectedBucket.status) }}</span></div>
          <div class="detail-item"><span class="label">备注</span><span>{{ selectedBucket.notes || '无' }}</span></div>
          <div class="detail-item"><span class="label">创建时间</span><span>{{ selectedBucket.created_at }}</span></div>
        </div>
        <button @click="selectedBucket = null" class="secondary-btn">关闭</button>
      </div>
    </div>

    <!-- 分配套餐弹窗 -->
    <div v-if="assignTarget" class="modal-overlay" @click="assignTarget = null">
      <div class="modal" @click.stop>
        <h3>为 {{ assignTarget.bucket_name }} 分配套餐</h3>
        <div class="plan-selector">
          <div v-for="plan in plans" :key="plan.id" class="plan-option" :class="{ selected: assignPlanId === plan.id }" @click="assignPlanId = plan.id">
            <strong>{{ plan.plan_name }}</strong>
            <span>¥{{ plan.base_price }}/月 · {{ plan.storage_quota_gb }}GB</span>
            <span class="text-muted">{{ plan.description }}</span>
          </div>
        </div>
        <div class="form-actions">
          <button @click="handleAssignPlan" class="primary-btn" :disabled="assigning">确认分配</button>
          <button @click="assignTarget = null" class="secondary-btn">取消</button>
        </div>
      </div>
    </div>

    <!-- 套餐新增/编辑弹窗 -->
    <div v-if="planFormVisible" class="modal-overlay" @click="planFormVisible = false">
      <div class="modal large" @click.stop>
        <h3>{{ editingPlan ? '编辑套餐: ' + editingPlan.plan_name : '新增套餐' }}</h3>
        <form class="plan-form" @submit.prevent="handleSavePlan">
          <div class="form-grid">
            <div class="form-group">
              <label>套餐名称 *</label>
              <input v-model.trim="planForm.plan_name" required placeholder="如: 免费版" />
            </div>
            <div class="form-group">
              <label>套餐编码 *</label>
              <input v-model.trim="planForm.plan_code" required placeholder="如: free" />
            </div>
            <div class="form-group">
              <label>套餐等级 *</label>
              <input v-model.number="planForm.plan_level" type="number" min="1" max="10" required />
            </div>
            <div class="form-group">
              <label>排序权重</label>
              <input v-model.number="planForm.sort_order" type="number" min="0" />
            </div>
            <div class="form-group">
              <label>存储配额(GB) *</label>
              <input v-model.number="planForm.storage_quota_gb" type="number" min="0" step="1" required />
            </div>
            <div class="form-group">
              <label>最大文件数</label>
              <input v-model.number="planForm.max_file_count" type="number" min="0" placeholder="留空不限" />
            </div>
            <div class="form-group">
              <label>最大文件大小(MB)</label>
              <input v-model.number="planForm.max_file_size_mb" type="number" min="0" placeholder="留空不限" />
            </div>
            <div class="form-group">
              <label>月流量配额(GB) *</label>
              <input v-model.number="planForm.monthly_traffic_gb" type="number" min="0" required />
            </div>
            <div class="form-group">
              <label>CDN回源流量(GB)</label>
              <input v-model.number="planForm.monthly_cdn_traffic_gb" type="number" min="0" />
            </div>
            <div class="form-group">
              <label>月GET请求次数</label>
              <input v-model.number="planForm.monthly_get_requests" type="number" min="0" />
            </div>
            <div class="form-group">
              <label>月PUT请求次数</label>
              <input v-model.number="planForm.monthly_put_requests" type="number" min="0" />
            </div>
            <div class="form-group">
              <label>基础月费(元) *</label>
              <input v-model.number="planForm.base_price" type="number" min="0" step="0.01" required />
            </div>
            <div class="form-group">
              <label>超额存储单价(元/GB)</label>
              <input v-model.number="planForm.storage_price_per_gb" type="number" min="0" step="0.0001" />
            </div>
            <div class="form-group">
              <label>超额流量单价(元/GB)</label>
              <input v-model.number="planForm.traffic_price_per_gb" type="number" min="0" step="0.0001" />
            </div>
            <div class="form-group">
              <label>超额请求单价(元/万次)</label>
              <input v-model.number="planForm.request_price_per_10k" type="number" min="0" step="0.0001" />
            </div>
            <div class="form-group">
              <label>免费试用天数</label>
              <input v-model.number="planForm.free_trial_days" type="number" min="0" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <input v-model.trim="planForm.description" placeholder="套餐说明" />
            </div>
          </div>
          <div class="form-group full-width">
            <label>特性列表（每行一个）</label>
            <textarea v-model="featuresText" rows="5" placeholder="例如：&#10;10GB 存储空间&#10;50GB 月流量&#10;标准存储"></textarea>
          </div>
          <div class="form-group checkbox-group">
            <label>
              <input v-model="planForm.status" type="checkbox" />
              启用
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="primary-btn" :disabled="planSaving">
              {{ planSaving ? '保存中...' : '保存' }}
            </button>
            <button type="button" class="secondary-btn" @click="planFormVisible = false">取消</button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  getTenantBuckets,
  getStoragePlans,
  createTenantBucket,
  deleteTenantBucket,
  assignStoragePlan,
  syncBucketDomains,
  getBillingByPeriod,
  calculateAllBills,
  getUsageSnapshots,
  snapshotAllDailyUsage,
  getPlatformStorageOverview,
  getCredentialsStatus,
  testCredentials,
  saveCredentials,
  createStoragePlan,
  updateStoragePlan,
  deleteStoragePlan,
  type TenantBucket,
  type StoragePlan,
  type BillingRecord,
  type UsageSnapshot,
  type CredentialsStatus,
  type CredentialsTestResult,
  type SavePlanRequest,
} from "../api/tenantStorage";

// 标签页
const tabs = [
  { value: "buckets", label: "空间管理" },
  { value: "plans", label: "套餐方案" },
  { value: "billing", label: "计费管理" },
  { value: "usage", label: "用量快照" },
];
const activeTab = ref("buckets");

// 通用状态
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

// 空间列表
const buckets = ref<TenantBucket[]>([]);
const showCreateBucketForm = ref(false);
const creating = ref(false);
const bucketForm = ref({
  tenant_id: 0,
  bucket_name: "",
  bucket_region: "z0",
  bucket_private: false,
  plan_id: 0,
  notes: "",
});

// 套餐
const plans = ref<StoragePlan[]>([]);

// 计费
const billingRecords = ref<BillingRecord[]>([]);
const billingLoading = ref(false);
const billPeriod = ref("");

// 用量快照
const snapshots = ref<UsageSnapshot[]>([]);
const snapshotLoading = ref(false);
const snapshotBucketId = ref(0);
const snapshotBegin = ref("");
const snapshotEnd = ref("");

// 弹窗
const selectedBucket = ref<TenantBucket | null>(null);
const assignTarget = ref<TenantBucket | null>(null);
const assignPlanId = ref(0);
const assigning = ref(false);

// 凭证管理
const showCredentialForm = ref(false);
const credentialStatus = ref<CredentialsStatus>({
  configured: false,
  has_access_key: false,
  has_secret_key: false,
  masked_access_key: "",
  last_updated_at: null,
});
const credForm = ref({ access_key: "", secret_key: "" });
const credTesting = ref(false);
const credSaving = ref(false);
const credTestPassed = ref(false);
const credTestResult = ref<CredentialsTestResult | null>(null);

// 套餐表单
const planFormVisible = ref(false);
const editingPlan = ref<StoragePlan | null>(null);
const planSaving = ref(false);
const planForm = ref<SavePlanRequest>({
  plan_name: "",
  plan_code: "",
  plan_level: 1,
  storage_quota_gb: 10,
  max_file_count: null,
  max_file_size_mb: null,
  monthly_traffic_gb: 100,
  monthly_cdn_traffic_gb: 50,
  monthly_get_requests: 100000,
  monthly_put_requests: 10000,
  base_price: 0,
  storage_price_per_gb: 0.1,
  traffic_price_per_gb: 0.5,
  request_price_per_10k: 0.01,
  free_trial_days: 0,
  status: true,
  sort_order: 0,
  description: "",
  features: [],
});
const featuresText = ref("");

// 工具函数
function formatGb(v: number): string {
  if (!v || v < 0.001) return "0";
  return v < 100 ? v.toFixed(2) : v.toFixed(1);
}
function formatNum(v: number): string {
  if (v === null || v === undefined) return "0";
  return Number(v).toFixed(2);
}
function formatNumber(v: number): string {
  if (!v) return "0";
  return v >= 10000 ? (v / 10000).toFixed(1) + "万" : String(v);
}
function usagePercent(b: TenantBucket): number {
  if (!b.storage_quota_gb || b.storage_quota_gb === 0) return 0;
  return Math.min(100, ((b.storage_used_gb || 0) / b.storage_quota_gb) * 100);
}
function usageClass(b: TenantBucket): string {
  const p = usagePercent(b);
  if (p >= 90) return "danger";
  if (p >= 70) return "warning";
  return "normal";
}
function statusLabel(s: string): string {
  const map: Record<string, string> = { active: "活跃", creating: "创建中", suspended: "已暂停", deleting: "删除中", deleted: "已删除" };
  return map[s] || s;
}
function billStatusLabel(s: string): string {
  const map: Record<string, string> = { pending: "待计算", calculated: "已出账", paid: "已支付", overdue: "已逾期", cancelled: "已取消" };
  return map[s] || s;
}

async function showMessage(msg: string, type: "success" | "error" = "success") {
  if (type === "success") {
    successMessage.value = msg;
    errorMessage.value = "";
  } else {
    errorMessage.value = msg;
    successMessage.value = "";
  }
  setTimeout(() => { successMessage.value = ""; errorMessage.value = ""; }, 5000);
}

function switchTab(tab: string) {
  activeTab.value = tab;
  errorMessage.value = "";
  successMessage.value = "";
}

// 加载数据
async function refreshBuckets() {
  loading.value = true;
  try {
    buckets.value = await getTenantBuckets();
    plans.value = await getStoragePlans();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "加载失败", "error");
  } finally {
    loading.value = false;
  }
}

async function handleCreateBucket() {
  creating.value = true;
  try {
    const request = {
      tenant_id: bucketForm.value.tenant_id,
      bucket_name: bucketForm.value.bucket_name,
      bucket_region: bucketForm.value.bucket_region,
      bucket_private: bucketForm.value.bucket_private,
      plan_id: bucketForm.value.plan_id || 0,
      notes: bucketForm.value.notes,
    };
    await createTenantBucket(request);
    showMessage("空间创建成功");
    showCreateBucketForm.value = false;
    bucketForm.value = { tenant_id: 0, bucket_name: "", bucket_region: "z0", bucket_private: false, plan_id: 0, notes: "" };
    await refreshBuckets();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "创建失败", "error");
  } finally {
    creating.value = false;
  }
}

async function handleDeleteBucket(bucket: TenantBucket) {
  if (!confirm(`确定要删除空间 "${bucket.bucket_name}" 吗？此操作不可逆。`)) return;
  try {
    await deleteTenantBucket(bucket.id);
    showMessage("空间已删除");
    await refreshBuckets();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "删除失败", "error");
  }
}

function viewBucketDetail(bucket: TenantBucket) {
  selectedBucket.value = bucket;
}

function showAssignPlan(bucket: TenantBucket) {
  assignTarget.value = bucket;
  assignPlanId.value = 0;
}

async function handleAssignPlan() {
  if (!assignTarget.value || !assignPlanId.value) return;
  assigning.value = true;
  try {
    await assignStoragePlan({
      tenant_id: assignTarget.value.tenant_id,
      tenant_bucket_id: assignTarget.value.id,
      plan_id: assignPlanId.value,
      auto_renew: true,
    });
    showMessage("套餐分配成功");
    assignTarget.value = null;
    await refreshBuckets();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "分配失败", "error");
  } finally {
    assigning.value = false;
  }
}

// 套餐 CRUD
function openPlanForm(plan: StoragePlan | null) {
  editingPlan.value = plan;
  if (plan) {
    planForm.value = {
      plan_name: plan.plan_name,
      plan_code: plan.plan_code,
      plan_level: plan.plan_level,
      storage_quota_gb: plan.storage_quota_gb,
      max_file_count: plan.max_file_count,
      max_file_size_mb: plan.max_file_size_mb,
      monthly_traffic_gb: plan.monthly_traffic_gb,
      monthly_cdn_traffic_gb: plan.monthly_cdn_traffic_gb,
      monthly_get_requests: plan.monthly_get_requests,
      monthly_put_requests: plan.monthly_put_requests,
      base_price: plan.base_price,
      storage_price_per_gb: plan.storage_price_per_gb,
      traffic_price_per_gb: plan.traffic_price_per_gb,
      request_price_per_10k: plan.request_price_per_10k,
      free_trial_days: plan.free_trial_days,
      status: plan.status,
      sort_order: plan.sort_order,
      description: plan.description,
      features: plan.features,
    };
    featuresText.value = plan.features.join("\n");
  } else {
    planForm.value = {
      plan_name: "", plan_code: "", plan_level: 1, storage_quota_gb: 10,
      max_file_count: null, max_file_size_mb: null, monthly_traffic_gb: 100,
      monthly_cdn_traffic_gb: 50, monthly_get_requests: 100000, monthly_put_requests: 10000,
      base_price: 0, storage_price_per_gb: 0.1, traffic_price_per_gb: 0.5,
      request_price_per_10k: 0.01, free_trial_days: 0, status: true, sort_order: 0,
      description: "", features: [],
    };
    featuresText.value = "";
  }
  planFormVisible.value = true;
}

async function handleSavePlan() {
  planSaving.value = true;
  try {
    const features = featuresText.value
      .split("\n")
      .map(s => s.trim())
      .filter(s => s.length > 0);
    // 构造干净对象，避免 Vue Proxy 序列化问题
    const payload: SavePlanRequest = {
      plan_name: planForm.value.plan_name,
      plan_code: planForm.value.plan_code,
      plan_level: planForm.value.plan_level,
      storage_quota_gb: planForm.value.storage_quota_gb,
      max_file_count: planForm.value.max_file_count ?? null,
      max_file_size_mb: planForm.value.max_file_size_mb ?? null,
      monthly_traffic_gb: planForm.value.monthly_traffic_gb,
      monthly_cdn_traffic_gb: planForm.value.monthly_cdn_traffic_gb ?? 0,
      monthly_get_requests: planForm.value.monthly_get_requests ?? 0,
      monthly_put_requests: planForm.value.monthly_put_requests ?? 0,
      base_price: planForm.value.base_price,
      storage_price_per_gb: planForm.value.storage_price_per_gb ?? 0,
      traffic_price_per_gb: planForm.value.traffic_price_per_gb ?? 0,
      request_price_per_10k: planForm.value.request_price_per_10k ?? 0,
      free_trial_days: planForm.value.free_trial_days ?? 0,
      status: planForm.value.status ?? true,
      sort_order: planForm.value.sort_order ?? 0,
      description: planForm.value.description ?? "",
      features,
    };
    if (editingPlan.value) {
      await updateStoragePlan(editingPlan.value.id, payload);
      showMessage("套餐更新成功");
    } else {
      await createStoragePlan(payload);
      showMessage("套餐创建成功");
    }
    planFormVisible.value = false;
    plans.value = await getStoragePlans();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "保存失败", "error");
  } finally {
    planSaving.value = false;
  }
}

async function handleDeletePlan(plan: StoragePlan) {
  if (!confirm(`确定要删除套餐 "${plan.plan_name}" 吗？\n当前有 ${plan.tenant_count} 个租户正在使用此套餐。`)) return;
  try {
    await deleteStoragePlan(plan.id);
    showMessage("套餐已删除");
    plans.value = await getStoragePlans();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "删除失败", "error");
  }
}

async function syncAllDomains() {
  loading.value = true;
  let synced = 0;
  for (const bucket of buckets.value) {
    try {
      await syncBucketDomains(bucket.id);
      synced++;
    } catch { /* skip */ }
  }
  showMessage(`已同步 ${synced} 个空间的域名`);
  await refreshBuckets();
  loading.value = false;
}

// 计费
async function loadBilling() {
  if (!billPeriod.value) {
    const now = new Date();
    billPeriod.value = `${now.getFullYear()}-${String(now.getMonth()).padStart(2, "0")}`;
  }
  billingLoading.value = true;
  try {
    billingRecords.value = await getBillingByPeriod(billPeriod.value);
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "加载失败", "error");
  } finally {
    billingLoading.value = false;
  }
}

async function handleCalculateAllBills() {
  billingLoading.value = true;
  try {
    const result = await calculateAllBills(billPeriod.value || undefined);
    showMessage(`已为 ${result.calculated} 个空间生成 ${result.period} 账单`);
    await loadBilling();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "出账失败", "error");
  } finally {
    billingLoading.value = false;
  }
}

// 用量快照
async function loadSnapshots() {
  if (!snapshotBucketId.value) return;
  snapshotLoading.value = true;
  try {
    snapshots.value = await getUsageSnapshots(snapshotBucketId.value, snapshotBegin.value, snapshotEnd.value);
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "加载失败", "error");
  } finally {
    snapshotLoading.value = false;
  }
}

async function handleSnapshotAll() {
  snapshotLoading.value = true;
  try {
    const result = await snapshotAllDailyUsage();
    showMessage(`已抓取 ${result.snapshotted} 个空间的当日用量`);
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "抄表失败", "error");
  } finally {
    snapshotLoading.value = false;
  }
}

// 凭证管理
async function loadCredentialStatus() {
  try {
    credentialStatus.value = await getCredentialsStatus();
    // 自动展开表单如果未配置
    if (!credentialStatus.value.configured) {
      showCredentialForm.value = true;
    }
  } catch (e) {
    console.error("获取凭证状态失败", e);
  }
}

async function handleTestCredentials() {
  credTesting.value = true;
  credTestPassed.value = false;
  credTestResult.value = null;
  try {
    credTestResult.value = await testCredentials({
      access_key: credForm.value.access_key,
      secret_key: credForm.value.secret_key,
    });
    credTestPassed.value = credTestResult.value.success;
    if (credTestResult.value.success) {
      showMessage(`测试成功！连接七牛云，延迟 ${credTestResult.value.latency_ms}ms`);
    } else {
      showMessage(credTestResult.value.message, "error");
    }
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "测试请求失败", "error");
  } finally {
    credTesting.value = false;
  }
}

async function handleSaveCredentials() {
  if (!credTestPassed.value) {
    showMessage("请先测试通过再保存", "error");
    return;
  }
  credSaving.value = true;
  try {
    const result = await saveCredentials({
      access_key: credForm.value.access_key,
      secret_key: credForm.value.secret_key,
    });
    showMessage(result.message || "凭证保存成功");
    await loadCredentialStatus();
  } catch (e) {
    showMessage(e instanceof Error ? e.message : "保存失败", "error");
  } finally {
    credSaving.value = false;
  }
}

onMounted(async () => {
  await refreshBuckets();
  await loadCredentialStatus();
  // 设置默认账单月份
  const now = new Date();
  billPeriod.value = `${now.getFullYear()}-${String(now.getMonth()).padStart(2, "0")}`;
  // 设置默认快照日期范围
  const end = new Date();
  const begin = new Date();
  begin.setDate(begin.getDate() - 30);
  snapshotBegin.value = begin.toISOString().slice(0, 10);
  snapshotEnd.value = end.toISOString().slice(0, 10);
});
</script>

<style scoped>
.page-container { max-width: 1400px; margin: 0 auto; }

.row-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}
.row-head h1 { font-size: 28px; font-weight: 700; color: hsl(var(--foreground)); margin: 0; }
.subtitle { color: hsl(var(--muted-foreground)); font-size: 14px; }

.error { background: hsl(var(--destructive) / 0.1); color: hsl(var(--destructive)); padding: 12px 16px; border-radius: calc(var(--radius) - 4px); margin-bottom: 16px; border: 1px solid hsl(var(--destructive) / 0.2); font-size: 14px; }
.success { background: hsl(142 71% 45% / 0.1); color: hsl(142 71% 45%); padding: 12px 16px; border-radius: calc(var(--radius) - 4px); margin-bottom: 16px; border: 1px solid hsl(142 71% 45% / 0.2); font-size: 14px; }

/* Tabs */
.tabs { display: flex; gap: 4px; margin-bottom: 20px; background: hsl(var(--muted)); padding: 4px; border-radius: var(--radius); width: fit-content; }
.tabs button { padding: 8px 20px; border: none; border-radius: calc(var(--radius) - 4px); background: transparent; color: hsl(var(--muted-foreground)); cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.2s; }
.tabs button.active { background: hsl(var(--background)); color: hsl(var(--foreground)); box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

/* Panel */
.panel { background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: var(--radius); padding: 24px; margin-top: 16px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.panel-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: hsl(var(--foreground)); }
.panel-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

/* Buttons */
button { border: none; border-radius: calc(var(--radius) - 4px); padding: 8px 16px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.primary-btn { background: hsl(var(--primary)); color: hsl(var(--primary-foreground)); }
.primary-btn:hover:not(:disabled) { opacity: 0.9; }
.secondary-btn { background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); }
.secondary-btn:hover:not(:disabled) { background: hsl(var(--accent)); }
.small-btn { padding: 4px 10px; font-size: 12px; background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); }
.small-btn.danger { background: hsl(var(--destructive) / 0.15); color: hsl(var(--destructive)); }
.small-btn:hover:not(:disabled) { opacity: 0.8; }

/* Forms */
.form-panel { background: hsl(var(--muted)); padding: 20px; border-radius: var(--radius); margin-bottom: 20px; border: 1px solid hsl(var(--border)); }
.form-panel h3 { margin: 0 0 16px 0; font-size: 16px; font-weight: 600; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; font-weight: 500; color: hsl(var(--muted-foreground)); }
.form-group input, .form-group select { padding: 10px 12px; border: 1px solid hsl(var(--border)); border-radius: calc(var(--radius) - 4px); background: hsl(var(--background)); color: hsl(var(--foreground)); font-size: 14px; }
.checkbox-group { justify-content: flex-end; }
.checkbox-group label { display: flex; align-items: center; gap: 8px; font-size: 14px; cursor: pointer; }
.form-actions { grid-column: 1 / -1; display: flex; gap: 8px; }

.date-input, .select-input { padding: 8px 12px; border: 1px solid hsl(var(--border)); border-radius: calc(var(--radius) - 4px); background: hsl(var(--background)); color: hsl(var(--foreground)); font-size: 14px; }

/* Table */
.table-wrapper { overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; }
.table thead { background: hsl(var(--secondary)); }
.table th { padding: 12px 14px; text-align: left; font-size: 13px; font-weight: 600; color: hsl(var(--foreground)); border-bottom: 2px solid hsl(var(--border)); white-space: nowrap; }
.table td { padding: 12px 14px; font-size: 13px; color: hsl(var(--foreground)); border-bottom: 1px solid hsl(var(--border) / 0.5); }
.table tbody tr:hover { background: hsl(var(--accent)); }
.mono { font-family: 'Consolas', monospace; font-size: 12px; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.fee-total { font-weight: 700; color: hsl(var(--primary)); }

/* Status */
.status-active, .status-success { color: hsl(142 71% 45%); font-weight: 500; }
.status-suspended, .status-overdue { color: hsl(35 92% 50%); font-weight: 500; }
.status-deleted, .status-cancelled, .status-failed { color: hsl(var(--destructive)); font-weight: 500; }
.status-calculated, .status-creating { color: hsl(217 91% 60%); font-weight: 500; }
.status-pending { color: hsl(var(--muted-foreground)); font-weight: 500; }
.status-paid { color: hsl(142 71% 45%); font-weight: 500; }

.badge { display: inline-block; padding: 2px 10px; border-radius: 12px; background: hsl(var(--primary) / 0.1); color: hsl(var(--primary)); font-size: 12px; font-weight: 500; }
.text-muted { color: hsl(var(--muted-foreground)); font-size: 12px; }

/* Usage Bar */
.usage-bar { min-width: 120px; }
.usage-label { font-size: 11px; color: hsl(var(--muted-foreground)); margin-bottom: 4px; }
.bar { height: 6px; background: hsl(var(--secondary)); border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 3px; transition: width 0.3s; }
.bar-fill.normal { background: hsl(142 71% 45%); }
.bar-fill.warning { background: hsl(35 92% 50%); }
.bar-fill.danger { background: hsl(var(--destructive)); }

/* Plan Grid */
.plan-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
.plan-card { background: hsl(var(--muted)); border: 1px solid hsl(var(--border)); border-radius: var(--radius); padding: 24px; transition: border-color 0.2s; }
.plan-card:hover { border-color: hsl(var(--primary)); }
.plan-highlight { border-color: hsl(var(--primary) / 0.4); }
.plan-card h3 { margin: 0 0 8px 0; font-size: 18px; }
.plan-price { font-size: 32px; font-weight: 700; color: hsl(var(--foreground)); margin: 0 0 16px 0; }
.plan-price .unit { font-size: 14px; font-weight: 400; color: hsl(var(--muted-foreground)); }
.plan-features { list-style: none; padding: 0; margin: 0 0 16px 0; }
.plan-features li { padding: 6px 0; font-size: 13px; color: hsl(var(--foreground)); border-bottom: 1px solid hsl(var(--border) / 0.3); }
.plan-features li:last-child { border-bottom: none; }
.plan-meta { font-size: 12px; }
.plan-actions-row { display: flex; gap: 6px; margin-top: 12px; }
.plan-form { max-height: 60vh; overflow-y: auto; padding-right: 8px; }
.plan-form .form-grid { margin-bottom: 16px; }
.full-width { grid-column: 1 / -1; }
.full-width textarea { width: 100%; padding: 10px 12px; border: 1px solid hsl(var(--border)); border-radius: calc(var(--radius) - 4px); background: hsl(var(--background)); color: hsl(var(--foreground)); font-size: 14px; font-family: monospace; resize: vertical; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: hsl(var(--card)); padding: 24px; border-radius: var(--radius); max-width: 480px; width: 90%; border: 1px solid hsl(var(--border)); max-height: 80vh; overflow-y: auto; }
.modal.large { max-width: 640px; }
.modal h3 { margin: 0 0 20px 0; font-size: 18px; font-weight: 600; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item .label { font-size: 12px; color: hsl(var(--muted-foreground)); }
.detail-item span:last-child { font-size: 14px; color: hsl(var(--foreground)); }

/* Plan Selector */
.plan-selector { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.plan-option { padding: 12px; border: 2px solid hsl(var(--border)); border-radius: var(--radius); cursor: pointer; display: flex; flex-direction: column; gap: 4px; transition: border-color 0.2s; }
.plan-option:hover { border-color: hsl(var(--primary) / 0.5); }
.plan-option.selected { border-color: hsl(var(--primary)); background: hsl(var(--primary) / 0.05); }
.plan-option strong { font-size: 15px; }

.empty { color: hsl(var(--muted-foreground)); text-align: center; padding: 40px 0; font-size: 14px; }

/* Credential Card */
.credential-card { background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: var(--radius); margin-bottom: 20px; overflow: hidden; }
.credential-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; cursor: pointer; background: hsl(var(--muted) / 0.5); user-select: none; }
.credential-header:hover { background: hsl(var(--muted)); }
.credential-status-line { display: flex; align-items: center; gap: 8px; }
.credential-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.dot-ok { background: hsl(142 71% 45%); box-shadow: 0 0 6px hsl(142 71% 45% / 0.4); }
.dot-none { background: hsl(var(--muted-foreground) / 0.4); }
.credential-sub { font-size: 12px; color: hsl(var(--muted-foreground)); }
.credential-sub.warn { color: hsl(35 92% 50%); font-weight: 500; }
.toggle-icon { font-size: 12px; color: hsl(var(--muted-foreground)); }
.credential-body { padding: 20px; border-top: 1px solid hsl(var(--border)); }
.button-group { display: flex; align-items: flex-end; gap: 8px; padding-top: 4px; }
.save-btn { background: hsl(142 71% 45%); color: #fff; }
.save-btn:hover:not(:disabled) { background: hsl(142 71% 40%); }

/* Test Result */
.test-result { margin-top: 16px; padding: 14px 18px; border-radius: calc(var(--radius) - 4px); }
.test-ok { background: hsl(142 71% 45% / 0.08); border: 1px solid hsl(142 71% 45% / 0.2); }
.test-fail { background: hsl(var(--destructive) / 0.08); border: 1px solid hsl(var(--destructive) / 0.2); }
.test-msg { font-size: 14px; font-weight: 500; margin: 0 0 6px 0; }
.test-detail { font-size: 12px; color: hsl(var(--muted-foreground)); margin: 0 0 8px 0; }
.bucket-list { display: flex; flex-wrap: wrap; gap: 6px; }
.bucket-tag { display: inline-block; padding: 2px 10px; border-radius: 12px; background: hsl(var(--secondary)); color: hsl(var(--foreground)); font-size: 12px; font-family: monospace; }

</style>
