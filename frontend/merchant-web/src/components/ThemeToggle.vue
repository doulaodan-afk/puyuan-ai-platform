<template>
  <button
    ref="btnRef"
    @click="toggleTheme"
    class="theme-btn"
    aria-label="切换主题"
  >
    <Sun v-if="resolved === 'light'" class="icon" />
    <Moon v-else class="icon" />
  </button>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { useTheme } from '../composables/useTheme'

const Sun = {
  render() {
    return h('svg', {
      class: 'sun-icon',
      xmlns: 'http://www.w3.org/2000/svg',
      width: '20',
      height: '20',
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('circle', { cx: '12', cy: '12', r: '4' }),
      h('path', { d: 'M12 2v2' }),
      h('path', { d: 'M12 20v2' }),
      h('path', { d: 'm4.93 4.93 1.41 1.41' }),
      h('path', { d: 'm17.66 17.66 1.41 1.41' }),
      h('path', { d: 'M2 12h2' }),
      h('path', { d: 'M20 12h2' }),
      h('path', { d: 'm6.34 17.66-1.41 1.41' }),
      h('path', { d: 'm19.07 4.93-1.41 1.41' })
    ])
  }
}

const Moon = {
  render() {
    return h('svg', {
      class: 'moon-icon',
      xmlns: 'http://www.w3.org/2000/svg',
      width: '20',
      height: '20',
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }, [
      h('path', { d: 'M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z' })
    ])
  }
}

const { resolved, toggleWithAnimation } = useTheme()

const btnRef = ref<HTMLElement | null>(null)

const toggleTheme = () => {
  const rect = btnRef.value?.getBoundingClientRect()
  const origin = rect
    ? { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
    : { x: window.innerWidth / 2, y: window.innerHeight / 2 }
  toggleWithAnimation(origin)
}
</script>

<style scoped>
.theme-btn {
  position: relative;
  padding: 6px 10px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-btn:hover {
  background: hsl(var(--accent));
  transform: scale(1.05);
}

.theme-btn:focus {
  outline: none;
}

.theme-btn:focus-visible {
  outline: 2px solid hsl(var(--ring));
  outline-offset: 2px;
}

.icon {
  width: 20px;
  height: 20px;
  transition: transform 0.2s ease;
}

.sun-icon,
.moon-icon {
  color: inherit;
}
</style>