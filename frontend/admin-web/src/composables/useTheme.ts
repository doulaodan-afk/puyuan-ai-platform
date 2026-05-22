import { ref, watch, onMounted } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'
type Resolved = 'light' | 'dark'

const STORAGE_KEY = 'admin-puyuan-theme'

const mode = ref<ThemeMode>('dark')
const resolved = ref<Resolved>('dark')

function getSystemPref(): Resolved {
  if (typeof window === 'undefined') return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyResolved(r: Resolved) {
  const root = document.documentElement
  root.setAttribute('data-theme', r)
  root.style.colorScheme = r
}

function resolveMode(m: ThemeMode): Resolved {
  return m === 'auto' ? getSystemPref() : m
}

export function useTheme() {
  const updateResolved = () => {
    resolved.value = resolveMode(mode.value)
  }

  onMounted(() => {
    const saved = (localStorage.getItem(STORAGE_KEY) as ThemeMode | null) ?? 'dark'
    mode.value = saved
    updateResolved()
    applyResolved(resolved.value)

    if (mode.value === 'auto') {
      const mql = window.matchMedia('(prefers-color-scheme: dark)')
      const onChange = () => {
        updateResolved()
        applyResolved(resolved.value)
      }
      mql.addEventListener?.('change', onChange)
    }
  })

  watch(mode, (newMode) => {
    updateResolved()
    applyResolved(resolved.value)
    localStorage.setItem(STORAGE_KEY, newMode)
  })

  const setMode = (next: ThemeMode, origin?: { x: number; y: number }) => {
    const nextResolved: Resolved = resolveMode(next)
    const current = resolved.value
    const changed = nextResolved !== current

    const commit = () => {
      mode.value = next
      updateResolved()
      applyResolved(resolved.value)
      localStorage.setItem(STORAGE_KEY, next)
    }

    if (!changed) {
      commit()
      return
    }

    // Create overlay element
    const overlay = document.createElement('div')
    overlay.style.cssText = `
      position: fixed;
      inset: 0;
      z-index: 99999;
      pointer-events: none;
      opacity: 0;
      transition: opacity 0s;
      will-change: opacity;
    `

    // Set overlay color - going to dark: overlay is dark, going to light: overlay is light
    overlay.style.background = nextResolved === 'dark'
      ? 'hsl(240, 10%, 4%)'
      : 'hsl(0, 0%, 100%)'

    document.body.appendChild(overlay)

    // Get origin point
    const originX = origin ? `${origin.x}px` : '50%'
    const originY = origin ? `${origin.y}px` : '50%'

    // Force reflow
    overlay.offsetHeight

    // Start animation: fade in overlay
    requestAnimationFrame(() => {
      overlay.style.opacity = '1'

      // After overlay fully covers screen, switch theme
      setTimeout(() => {
        commit()
      }, 150)

      // Then fade out overlay to reveal new theme
      setTimeout(() => {
        overlay.style.opacity = '0'
        overlay.style.transition = 'opacity 0.3s ease-out'
      }, 200)

      // Remove overlay after animation
      setTimeout(() => {
        overlay.remove()
      }, 500)
    })
  }

  const toggleWithAnimation = (origin?: { x: number; y: number }) => {
    const next: ThemeMode = resolved.value === 'dark' ? 'light' : 'dark'
    setMode(next, origin)
  }

  return {
    mode,
    resolved,
    setMode,
    toggleWithAnimation,
  }
}