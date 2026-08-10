/**
 * GlassCard — Liquid glass card with hover effects
 */
interface GlassCardProps {
  children: React.ReactNode
  className?: string
  variant?: 'default' | 'strong' | 'subtle' | 'dark'
  hover?: boolean
}

export default function GlassCard({
  children,
  className = '',
  variant = 'default',
  hover = false,
}: GlassCardProps) {
  const variantClass = {
    default: 'liquid-glass',
    strong: 'liquid-glass-strong',
    subtle: 'liquid-glass-subtle',
    dark: 'liquid-glass-dark',
  }[variant]

  return (
    <div
      className={`${variantClass} rounded-2xl ${hover ? 'liquid-glass-hover' : ''} ${className}`}
    >
      {children}
    </div>
  )
}
