/**
 * Hero — Full-viewport Apple-style hero
 * GSAP-powered entrance animations. Pure white/black.
 */
import { useRef, useEffect } from 'react'
import { gsap } from '../../lib/gsapConfig'
import DashboardMockup from '../ui/DashboardMockup'
import { Button } from '../ui/button'

export default function Hero() {
  const heroRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = heroRef.current
    if (!el) return

    const ctx = gsap.context(() => {
      const tl = gsap.timeline({ defaults: { ease: 'power4.out' } })

      tl.from('.hero-headline', { opacity: 0, y: 50, duration: 1, delay: 0.3 })
        .from('.hero-sub', { opacity: 0, y: 30, duration: 0.8 }, '-=0.6')
        .from('.hero-ctas', { opacity: 0, y: 30, duration: 0.8 }, '-=0.5')
        .from('.hero-proof', { opacity: 0, duration: 0.6 }, '-=0.4')
        .from('.hero-mockup', {
          opacity: 0,
          y: 100,
          scale: 0.96,
          duration: 1.2,
          ease: 'power3.out',
        }, '-=0.8')
    }, el)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={heroRef}
      className="relative min-h-screen flex flex-col items-center justify-center px-6 pt-24 pb-8 overflow-hidden"
    >
      {/* Soft radial gradient */}
      <div className="gradient-mesh">
        <div className="gradient-orb w-[700px] h-[700px] bg-black/[0.03] top-[-200px] left-[10%]" />
        <div className="gradient-orb w-[500px] h-[500px] bg-black/[0.02] bottom-[100px] right-[-50px]" />
      </div>

      <div className="relative z-10 text-center max-w-4xl mx-auto">
        {/* Headline */}
        <h1 className="hero-headline headline-xl font-display text-text-primary mt-6">
          AI-Powered Billing for
          <br />
          Modern Travel Agencies
        </h1>

        {/* Subtext */}
        <p className="hero-sub body-large mt-5 max-w-2xl mx-auto">
          Automate invoicing, extract data from travel documents with AI, and
          get real-time financial visibility. Built for agencies that move fast.
        </p>

        {/* CTAs */}
        <div className="hero-ctas flex flex-col sm:flex-row items-center justify-center gap-3 mt-8">
          <Button render={<a href="#cta" />} size="lg" className="rounded-full px-7 shadow-lg shadow-black/8 group">
            Start Free Trial
            <span className="inline-block ml-1.5 transition-transform duration-300 group-hover:translate-x-0.5">→</span>
          </Button>
          <Button render={<a href="#features" />} variant="outline" size="lg" className="rounded-full px-7 border-border-medium hover:border-text-tertiary">
            See How It Works
          </Button>
        </div>

        {/* Social proof */}
        <p className="hero-proof text-xs text-text-tertiary mt-5">
          Trusted by <span className="text-text-secondary font-medium">500+</span> travel agencies · No credit card required
        </p>
      </div>

      {/* Dashboard mockup */}
      <div className="hero-mockup relative z-10 mt-12 w-full max-w-5xl mx-auto px-4">
        <div className="liquid-glass-strong rounded-2xl p-2 shadow-2xl shadow-black/[0.06]">
          <DashboardMockup />
        </div>
        <div className="h-24 bg-gradient-to-b from-white/80 to-transparent mt-[-1px]" />
      </div>
    </section>
  )
}
