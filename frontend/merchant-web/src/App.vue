<template>
  <div class="layout">
    <header class="topbar">
      <strong>濮院毛衫 AI 平台 - 商家端</strong>
      <nav>
        <RouterLink to="/dashboard">工作台</RouterLink>
        <RouterLink to="/plugins">插件</RouterLink>
        <RouterLink to="/account/balance">账户</RouterLink>
        <RouterLink to="/billing">账单</RouterLink>
      </nav>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>