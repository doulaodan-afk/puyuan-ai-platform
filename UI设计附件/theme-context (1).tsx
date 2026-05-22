import * as React from "react"

export type ThemeMode = "light" | "dark" | "auto"
type Resolved = "light" | "dark"

type Ctx = {
  mode: ThemeMode
  resolved: Resolved
  setMode: (mode: ThemeMode, origin?: { x: number; y: number }) => void
}

const ThemeContext = React.createContext<Ctx | null>(null)

const STORAGE_KEY = "puyuan-theme"

function getSystemPref(): Resolved {
  if (typeof window === "undefined") return "light"
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"
}

function applyResolved(r: Resolved) {
  const root = document.documentElement
  if (r === "dark") root.classList.add("dark")
  else root.classList.remove("dark")
  root.style.colorScheme = r
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = React.useState<ThemeMode>("auto")
  const [resolved, setResolved] = React.useState<Resolved>("light")

  // Load saved mode & apply ASAP on mount
  React.useEffect(() => {
    const saved = (localStorage.getItem(STORAGE_KEY) as ThemeMode | null) ?? "auto"
    setModeState(saved)
    const r: Resolved = saved === "auto" ? getSystemPref() : saved
    setResolved(r)
    applyResolved(r)
  }, [])

  // Follow system changes when in auto mode
  React.useEffect(() => {
    if (mode !== "auto") return
    const mql = window.matchMedia("(prefers-color-scheme: dark)")
    const onChange = () => {
      const r: Resolved = mql.matches ? "dark" : "light"
      setResolved(r)
      applyResolved(r)
    }
    mql.addEventListener?.("change", onChange)
    return () => mql.removeEventListener?.("change", onChange)
  }, [mode])

  const setMode = React.useCallback<Ctx["setMode"]>(
    (next, origin) => {
      const nextResolved: Resolved = next === "auto" ? getSystemPref() : next
      const changed = nextResolved !== resolved

      // Set origin CSS vars for the tide animation
      const root = document.documentElement
      if (origin) {
        root.style.setProperty("--tide-x", `${origin.x}px`)
        root.style.setProperty("--tide-y", `${origin.y}px`)
      } else {
        root.style.setProperty("--tide-x", "50%")
        root.style.setProperty("--tide-y", "50%")
      }

      const commit = () => {
        setModeState(next)
        setResolved(nextResolved)
        applyResolved(nextResolved)
        localStorage.setItem(STORAGE_KEY, next)
      }

      if (!changed) {
        commit()
        return
      }

      // Preferred: View Transitions API (Chrome 111+, Safari 18+)
      const doc = document as Document & {
        startViewTransition?: (cb: () => void) => { finished: Promise<void> }
      }
      if (typeof doc.startViewTransition === "function") {
        doc.startViewTransition(() => {
          commit()
        })
        return
      }

      // Fallback: overlay that expands from origin
      const overlay = document.createElement("div")
      overlay.className = "tide-overlay"
      // Tide color is the incoming theme's background tone — warm earthy in dark, cream in light
      overlay.style.setProperty(
        "--tide-color",
        nextResolved === "dark" ? "oklch(0.18 0.008 50)" : "oklch(0.985 0.008 75)"
      )
      document.body.appendChild(overlay)

      // Swap theme midway for a "through-water" effect
      window.setTimeout(commit, 600)
      window.setTimeout(() => overlay.remove(), 1500)
    },
    [resolved]
  )

  const value = React.useMemo<Ctx>(
    () => ({ mode, resolved, setMode }),
    [mode, resolved, setMode]
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const ctx = React.useContext(ThemeContext)
  if (!ctx) throw new Error("useTheme must be used within ThemeProvider")
  return ctx
}
