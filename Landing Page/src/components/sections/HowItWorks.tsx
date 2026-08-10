/**
 * HowItWorks — 4-step interactive flow with customized GSAP timeline sequencing,
 * SVG line drawing, prefers-reduced-motion compliance, and tactile node pulses.
 */
import { useRef, useEffect } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import { BuildingIcon, FileTextIcon, BrainIcon, TrendUpIcon } from '../ui/Icons'

const STEPS = [
  { step: '01', title: 'Create Your Organization', desc: 'Sign up in 30 seconds. Set up your agency profile, add branches, and invite your team.', Icon: BuildingIcon },
  { step: '02', title: 'Add Customers & Invoices', desc: 'Import existing data or create fresh. Our smart forms auto-calculate taxes and totals.', Icon: FileTextIcon },
  { step: '03', title: 'Upload Documents to AI', desc: 'Drop airline tickets, hotel invoices, and travel documents. AI extracts data instantly.', Icon: BrainIcon },
  { step: '04', title: 'Track & Grow', desc: 'Real-time dashboards show revenue, payments, and trends. Export reports anytime.', Icon: TrendUpIcon },
]

export default function HowItWorks() {
  const containerRef = useRef<HTMLDivElement>(null)
  const lineSvgRef = useRef<SVGLineElement>(null)
  const nodeRefs = useRef<(HTMLDivElement | null)[]>([])
  const textRefs = useRef<(HTMLDivElement | null)[]>([])
  const pulseTweens = useRef<(gsap.core.Tween | null)[]>([])

  useEffect(() => {
    const el = containerRef.current
    const line = lineSvgRef.current
    if (!el) return

    // Accessibility check: skips all custom animations if user prefers reduced motion
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduceMotion) {
      // Set initial states to fully visible
      gsap.set(nodeRefs.current, { opacity: 1, scale: 1 })
      gsap.set(textRefs.current, { opacity: 1, y: 0 })
      if (line) gsap.set(line, { strokeDashoffset: 0 })
      return
    }

    const ctx = gsap.context(() => {
      // Set initial hidden states
      gsap.set(nodeRefs.current, { opacity: 0, scale: 0.8 })
      gsap.set(textRefs.current, { opacity: 0, y: 15 })

      const tl = gsap.timeline({
        scrollTrigger: {
          trigger: el,
          start: 'top 80%',
          once: true,
        }
      })

      // Draw connection line stroke-dashoffset
      if (line) {
        // Approximate horizontal path length
        const length = 800 
        gsap.set(line, { strokeDasharray: length, strokeDashoffset: length })
        tl.to(line, { strokeDashoffset: 0, duration: 2, ease: 'none' }, 0)
      }

      // Step node scales in (staggered alongside connection line drawing)
      tl.to(nodeRefs.current[0], { opacity: 1, scale: 1, duration: 0.5, ease: 'back.out(1.2)' }, 0)
      tl.to(nodeRefs.current[1], { opacity: 1, scale: 1, duration: 0.5, ease: 'back.out(1.2)' }, 0.6)
      tl.to(nodeRefs.current[2], { opacity: 1, scale: 1, duration: 0.5, ease: 'back.out(1.2)' }, 1.2)
      tl.to(nodeRefs.current[3], { opacity: 1, scale: 1, duration: 0.5, ease: 'back.out(1.2)' }, 1.8)

      // Step text blocks fade and float up sequentially
      tl.to(textRefs.current[0], { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }, 0.2)
      tl.to(textRefs.current[1], { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }, 0.8)
      tl.to(textRefs.current[2], { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }, 1.4)
      tl.to(textRefs.current[3], { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }, 2.0)
    }, el)

    return () => {
      ctx.revert()
      pulseTweens.current.forEach(t => t?.kill())
    }
  }, [])

  // Tactile single-pulse micro-animation on node hover
  const handlePulse = (index: number) => {
    const el = nodeRefs.current[index]
    if (!el) return

    // Prevent overlap / stacking of hover tweens
    pulseTweens.current[index]?.kill()

    pulseTweens.current[index] = gsap.to(el, {
      scale: 1.08,
      duration: 0.2,
      yoyo: true,
      repeat: 1,
      ease: 'power2.out',
      onComplete: () => {
        gsap.to(el, { scale: 1, duration: 0.1, overwrite: 'auto' })
      }
    })
  }

  return (
    <section className="py-16 md:py-20 bg-white" id="how-it-works">
      <div className="section-container">
        {/* Eyebrow Restraint: omitted eyebrow here to avoid templated repetition */}
        <AnimatedSection className="text-center mb-16">
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Up and running in minutes.
          </AnimatedText>
          <p className="body-large mt-4 max-w-lg mx-auto">
            No complex setup. No training needed. Just sign up and start billing.
          </p>
        </AnimatedSection>

        <div ref={containerRef} className="grid md:grid-cols-4 gap-8 max-w-5xl mx-auto relative">
          {/* SVG Pen-stroke connection line */}
          <svg className="hidden md:block absolute top-[28px] left-[12%] right-[12%] w-[76%] h-px overflow-visible text-border-medium pointer-events-none" fill="none">
            <line
              ref={lineSvgRef}
              x1="0"
              y1="0"
              x2="100%"
              y2="0"
              stroke="currentColor"
              strokeWidth="1.2"
            />
          </svg>

          {STEPS.map((s, i) => (
            <div key={s.step} className="text-center relative">
              {/* Tactile node wrapper */}
              <div
                ref={(el) => { nodeRefs.current[i] = el }}
                onMouseEnter={() => handlePulse(i)}
                className="w-14 h-14 rounded-2xl bg-black flex items-center justify-center mx-auto relative z-10 text-white cursor-pointer shadow-sm select-none"
              >
                <s.Icon size={22} aria-hidden="true" />
              </div>

              {/* Text content */}
              <div
                ref={(el) => { textRefs.current[i] = el }}
                className="mt-5"
              >
                <span className="text-[11px] font-bold text-text-tertiary tracking-widest">{s.step}</span>
                <h3 className="text-[15px] font-semibold text-text-primary mt-1">
                  {s.title}
                </h3>
                <p className="text-[13px] text-text-secondary mt-2 leading-relaxed">
                  {s.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
