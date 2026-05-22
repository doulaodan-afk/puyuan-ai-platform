import * as React from "react"
import { Sun, Moon, Monitor, Check } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useTheme, type ThemeMode } from "@/lib/theme-context"
import { cn } from "@/lib/utils"

const options: { mode: ThemeMode; label: string; Icon: React.ComponentType<{ className?: string }> }[] = [
  { mode: "light", label: "白天", Icon: Sun },
  { mode: "dark", label: "黑夜", Icon: Moon },
  { mode: "auto", label: "跟随系统", Icon: Monitor },
]

export function ThemeToggle() {
  const { mode, resolved, setMode } = useTheme()
  const btnRef = React.useRef<HTMLButtonElement | null>(null)

  const handlePick = (next: ThemeMode) => (e: React.MouseEvent) => {
    const rect = btnRef.current?.getBoundingClientRect()
    const origin = rect
      ? { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
      : { x: e.clientX, y: e.clientY }
    setMode(next, origin)
  }

  const ActiveIcon = resolved === "dark" ? Moon : Sun

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          ref={btnRef}
          variant="ghost"
          size="icon"
          aria-label="切换主题"
          className="relative"
        >
          <ActiveIcon className="size-5 transition-transform" />
          {mode === "auto" && (
            <span className="border-background bg-primary absolute -right-0.5 -bottom-0.5 size-2 rounded-full border" />
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-44">
        <DropdownMenuLabel className="text-muted-foreground text-xs">
          外观主题
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        {options.map(({ mode: m, label, Icon }) => {
          const active = mode === m
          return (
            <DropdownMenuItem key={m} onClick={handlePick(m)} className="gap-2.5">
              <Icon className={cn("size-4", active && "text-primary")} />
              <span className={cn(active && "font-medium")}>{label}</span>
              {active && <Check className="text-primary ml-auto size-4" />}
            </DropdownMenuItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
