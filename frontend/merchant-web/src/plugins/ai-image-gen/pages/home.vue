<template>
  <section class="home-page">
    <h2>AI 商品图生成</h2>
    <p>上传商品图片，AI 自动生成多场景、多风格的高质量商品图。</p>
    <div class="placeholder">
      <p>🚧 功能开发中，敬请期待</p>
    </div>
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

.placeholder {
  margin-top: 32px;
  padding: 48px 24px;
  text-align: center;
  background: hsl(var(--card));
  border: 1px dashed hsl(var(--border));
  border-radius: var(--radius);
}

.placeholder p {
  font-size: 16px;
}
</style>
