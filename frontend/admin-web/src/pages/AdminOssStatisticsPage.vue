<template>
  <section>
    <header class="row-head">
      <h1>存储统计</h1>
      <div class="filters">
        <input v-model="beginDate" type="date" placeholder="起始日期" />
        <input v-model="endDate" type="date" placeholder="结束日期" />
        <select v-model="granularity">
          <option value="day">按天</option>
          <option value="month">按月</option>
        </select>
        <button @click="loadAll" :disabled="loading">{{ loading ? "加载中..." : "查询" }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <!-- 存储概览卡片 -->
    <section class="cards" v-if="overview">
      <article class="card">
        <h3>标准存储量</h3>
        <p class="value">{{ formatSize(overview.standard_space_gb) }} GB</p>
        <p class="sub">文件数: {{ overview.standard_count }}</p>
      </article>
      <article class="card">
        <h3>低频存储量</h3>
        <p class="value">{{ formatSize(overview.line_space_gb) }} GB</p>
        <p class="sub">文件数: {{ overview.line_count }}</p>
      </article>
      <article class="card">
        <h3>归档存储量</h3>
        <p class="value">{{ formatSize(overview.archive_space_gb) }} GB</p>
        <p class="sub">文件数: {{ overview.archive_count }}</p>
      </article>
      <article class="card">
        <h3>外网流出流量</h3>
        <p class="value">{{ formatSize(overview.blob_io_flux_gb) }} GB</p>
        <p class="sub">GET请求: {{ overview.get_count }}</p>
      </article>
      <article class="card">
        <h3>CDN回源流量</h3>
        <p class="value">{{ formatSize(overview.cdn_flux_gb) }} GB</p>
      </article>
      <article class="card">
        <h3>PUT请求次数</h3>
        <p class="value">{{ overview.put_count }}</p>
      </article>
    </section>

    <!-- 存储量趋势 -->
    <section class="panel" v-if="spaceData.datas.length > 0">
      <h2>标准存储量趋势</h2>
      <table class="table">
        <thead>
          <tr>
            <th>日期</th>
            <th>存储量 (bytes)</th>
            <th>存储量 (GB)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in spaceData.datas" :key="item.time">
            <td>{{ item.time }}</td>
            <td>{{ item.value }}</td>
            <td>{{ formatSize(item.value / (1024 * 1024 * 1024)) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 文件数趋势 -->
    <section class="panel" v-if="countData.datas.length > 0">
      <h2>标准文件数趋势</h2>
      <table class="table">
        <thead>
          <tr>
            <th>日期</th>
            <th>文件数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in countData.datas" :key="item.time">
            <td>{{ item.time }}</td>
            <td>{{ item.value }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 流量趋势 -->
    <section class="panel" v-if="blobIoData.datas.length > 0">
      <h2>流量趋势 (blob_io)</h2>
      <table class="table">
        <thead>
          <tr>
            <th>日期</th>
            <th>外网流出 (GB)</th>
            <th>CDN回源 (GB)</th>
            <th>数据读取 (GB)</th>
            <th>GET请求次数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in blobIoData.datas" :key="item.time">
            <td>{{ item.time }}</td>
            <td>{{ formatSize(item.flux / (1024 * 1024 * 1024)) }}</td>
            <td>{{ formatSize(item.cdn_flux / (1024 * 1024 * 1024)) }}</td>
            <td>{{ formatSize(item.read_bytes / (1024 * 1024 * 1024)) }}</td>
            <td>{{ item.get_count }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- PUT请求趋势 -->
    <section class="panel" v-if="putData.datas.length > 0">
      <h2>PUT请求次数趋势</h2>
      <table class="table">
        <thead>
          <tr>
            <th>日期</th>
            <th>PUT请求次数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in putData.datas" :key="item.time">
            <td>{{ item.time }}</td>
            <td>{{ item.value }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 支持的统计类型 -->
    <section class="panel" v-if="statTypes.length > 0">
      <h2>支持的统计类型</h2>
      <table class="table">
        <thead>
          <tr>
            <th>类型</th>
            <th>说明</th>
            <th>单位</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in statTypes" :key="item.stat_type">
            <td>{{ item.stat_type }}</td>
            <td>{{ item.stat_label }}</td>
            <td>{{ item.unit }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  getOssStatisticsOverview,
  getOssStatistic,
  getOssBlobIoStatistic,
  getOssStatTypes,
  type StorageOverviewResponse,
  type StatisticTypeResponse,
  type BlobIoStatisticResponse,
  type StatTypeInfo,
} from "../api/ossStatistics";

const loading = ref(false);
const errorMessage = ref("");

// 日期筛选
const beginDate = ref("");
const endDate = ref("");
const granularity = ref("day");

// 数据
const overview = ref<StorageOverviewResponse | null>(null);
const spaceData = ref<StatisticTypeResponse>({ stat_type: "space", stat_label: "", unit: "", datas: [] });
const countData = ref<StatisticTypeResponse>({ stat_type: "count", stat_label: "", unit: "", datas: [] });
const putData = ref<StatisticTypeResponse>({ stat_type: "rs_put", stat_label: "", unit: "", datas: [] });
const blobIoData = ref<BlobIoStatisticResponse>({ datas: [], bucket: "", query_range: "" });
const statTypes = ref<StatTypeInfo[]>([]);

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
    overview.value = await getOssStatisticsOverview(beginDate.value, endDate.value);
    spaceData.value = await getOssStatistic("space", beginDate.value, endDate.value, granularity.value);
    countData.value = await getOssStatistic("count", beginDate.value, endDate.value, granularity.value);
    putData.value = await getOssStatistic("rs_put", beginDate.value, endDate.value, granularity.value);
    blobIoData.value = await getOssBlobIoStatistic(beginDate.value, endDate.value, granularity.value);
    statTypes.value = await getOssStatTypes();
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
  margin-bottom: 24px;
}

h1 {
  color: hsl(var(--foreground));
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filters input,
.filters select {
  padding: 6px 10px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--muted));
  color: hsl(var(--foreground));
  font-size: 14px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.card,
.panel {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 16px;
}

.panel {
  margin-bottom: 16px;
}

.card h3,
.panel h2 {
  margin: 0 0 8px 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  font-weight: 500;
}

.value {
  font-size: 24px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
}

.sub {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  margin: 4px 0 0 0;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
  background: hsl(var(--card));
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid hsl(var(--border));
}

thead {
  background: hsl(var(--secondary));
}

th {
  padding: 10px 14px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-bottom: 2px solid hsl(var(--border));
}

td {
  padding: 10px 14px;
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

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:hover:not(:disabled) {
  opacity: 0.9;
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