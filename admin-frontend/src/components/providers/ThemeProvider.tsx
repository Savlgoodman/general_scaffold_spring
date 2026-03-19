import { useEffect, type ReactNode } from 'react'
import { useThemeStore } from '@/store/theme'

interface ThemeProviderProps {
  children: ReactNode
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const { theme, setTheme, resolvedTheme } = useThemeStore()

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    const handleChange = () => {
      if (theme === 'system') {
        const systemTheme = mediaQuery.matches ? 'dark' : 'light'
        useThemeStore.setState({ resolvedTheme: systemTheme })
        document.documentElement.classList.remove('light', 'dark')
        document.documentElement.classList.add(systemTheme)
      }
    }

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [theme])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (
        event.altKey &&
        event.shiftKey &&
        event.key.toLowerCase() === 'd'
      ) {
        event.preventDefault()
        const newTheme = resolvedTheme === 'dark' ? 'light' : 'dark'
        setTheme(newTheme)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [resolvedTheme, setTheme])

  return <>{children}</>
}
