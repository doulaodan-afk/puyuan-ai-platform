<template>
  <div class="layout">
    <header class="topbar">
      <strong>濮院毛衫 AI 平台 - 管理端</strong>
      <nav>
        <RouterLink to="/admin/dashboard">看板</RouterLink>
        <RouterLink to="/admin/tenants">租户</RouterLink>
        <RouterLink to="/admin/plugins">插件</RouterLink>
        <RouterLink to="/admin/pricing">定价</RouterLink>
        <RouterLink to="/admin/billing">账单</RouterLink>
        <RouterLink to="/admin/audit">审计</RouterLink>
      </nav>
      <button class="logout" @click="logout">退出</button>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import { useAdminAuthStore } from "./stores/adminAuth";

const router = useRouter();
const auth = useAdminAuthStore();

async function logout() {
  auth.clearAuth();
  await router.replace("/admin/login");
}
</script>