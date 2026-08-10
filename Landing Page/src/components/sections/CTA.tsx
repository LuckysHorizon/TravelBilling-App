/**
 * CTA — Final call-to-action, jet black background
 */
import { useRef, useEffect } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import { Button } from '../ui/button'

export default function CTA() {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = containerRef.current
    if (!el) return

    const ctx = gsap.context(() => {
      // Parallax glow orbs
      gsap.to('.cta-orb-1', {
        y: -60,
        ease: 'none',
        scrollTrigger: { trigger: el, start: 'top bottom', end: 'bottom top', scrub: 2 },
      })
      gsap.to('.cta-orb-2', {
        y: 40,
        ease: 'none',
        scrollTrigger: { trigger: el, start: 'top bottom', end: 'bottom top', scrub: 2 },
      })
    })

    return () => ctx.revert()
  }, [])

  return (
    <section ref={containerRef} className="section-padding bg-black text-white relative overflow-hidden" id="cta">
      <div className="gradient-mesh">
        <div className="cta-orb-1 gradient-orb w-[500px] h-[500px] bg-white/[0.03] top-[-200px] left-[-100px]" />
        <div className="cta-orb-2 gradient-orb w-[400px] h-[400px] bg-white/[0.02] bottom-[-150px] right-[-100px]" />
      </div>

      <div className="section-container relative z-10 text-center">
        <AnimatedSection>
          <AnimatedText as="h2" className="headline-xl font-display text-white max-w-3xl mx-auto">
            Ready to transform your billing?
          </AnimatedText>
          <p className="text-lg text-white/40 mt-5 max-w-xl mx-auto leading-relaxed">
            Join 500+ travel agencies that have already modernized their financial
            operations with TravelBilling Pro.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mt-10">
            <Button render={<a href="#" />} size="lg" className="rounded-full px-8 shadow-lg shadow-white/5 bg-white text-black hover:bg-surface-secondary group">
              Start Free Trial
              <span className="inline-block ml-1.5 transition-transform duration-300 group-hover:translate-x-0.5">→</span>
            </Button>
            <Button render={<a href="#" />} variant="outline" size="lg" className="rounded-full px-8 border-white/10 text-white/50 hover:text-white hover:border-white/20 bg-transparent hover:bg-transparent">
              Schedule a Demo
            </Button>
          </div>

          <p className="text-xs text-white/25 mt-5">
            Free 14-day trial · No credit card required · Cancel anytime
          </p>
        </AnimatedSection>
      </div>
    </section>
  )
}
