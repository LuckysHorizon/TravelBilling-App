/**
 * Stats — GSAP-powered counting animation, monochrome
 */
import { useRef, useEffect, useState } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'

interface StatItem {
  value: number
  suffix: string
  prefix?: string
  label: string
  description: string
}

function CountingStat({ value, suffix, prefix = '', label, description }: StatItem) {
  const [display, setDisplay] = useState(0)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    const obj = { val: 0 }
    const ctx = gsap.context(() => {
      ScrollTrigger.create({
        trigger: el,
        start: 'top 85%',
        once: true,
        onEnter: () => {
          gsap.to(obj, {
            val: value,
            duration: 2,
            ease: 'power2.out',
            onUpdate: () => setDisplay(Math.round(obj.val)),
          })
        },
      })
    })

    return () => ctx.revert()
  }, [value])

  return (
    <div ref={ref} className="text-center">
      <p className="text-5xl md:text-6xl font-bold text-text-primary tracking-tight">
        {prefix}{display.toLocaleString()}{suffix}
      </p>
      <p className="text-sm font-semibold text-text-primary mt-2">{label}</p>
      <p className="text-xs text-text-tertiary mt-1">{description}</p>
    </div>
  )
}

const STATS: StatItem[] = [
  { value: 80, suffix: '%', label: 'Time Saved', description: 'Reduction in manual billing effort' },
  { value: 2, suffix: 'M+', label: 'Invoices Processed', description: 'Across all organizations' },
  { value: 500, suffix: '+', label: 'Travel Agencies', description: 'Trust TravelBilling Pro' },
  { value: 99, suffix: '.9%', label: 'Uptime', description: 'Enterprise-grade reliability' },
]

export default function Stats() {
  return (
    <section className="section-padding bg-surface-secondary">
      <div className="section-container">
        <AnimatedSection className="text-center mb-16">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-text-tertiary mb-3">
            Impact
          </p>
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Numbers that speak.
          </AnimatedText>
        </AnimatedSection>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12">
          {STATS.map((stat, i) => (
            <AnimatedSection key={stat.label} delay={i * 0.1}>
              <CountingStat {...stat} />
            </AnimatedSection>
          ))}
        </div>
      </div>
    </section>
  )
}
