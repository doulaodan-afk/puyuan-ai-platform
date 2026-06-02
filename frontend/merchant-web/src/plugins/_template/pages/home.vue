<template>
  <section class="home-page">
    <h2>{{PLUGIN_NAME}}</h2>
    <p>插件首页内容，请根据需求修改此页面。</p>
  </section>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-page {
  padding: 24px;
}

h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
}

p {
  color: hsl(var(--muted-foreground));
}
</style>