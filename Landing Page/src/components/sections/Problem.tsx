/**
 * Problem — Old Way vs New Way with visual hierarchy and staggered GSAP entrances
 */
import { useEffect, useRef } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import GlassCard from '../ui/GlassCard'
import {
  SpreadsheetIcon, PenEditIcon, PhoneIcon, ClipboardIcon, RefreshIcon, ClockIcon,
  BrainIcon, ZapIcon, CloudIcon, TrendUpIcon, BuildingIcon, ShieldIcon,
} from '../ui/Icons'

const OLD_WAY = [
  { Icon: SpreadsheetIcon, text: 'Excel spreadsheets for invoicing' },
  { Icon: PenEditIcon, text: 'Manual data entry from PDFs' },
  { Icon: PhoneIcon, text: 'WhatsApp document sharing' },
  { Icon: ClipboardIcon, text: 'Paper-based accounting' },
  { Icon: RefreshIcon, text: 'Duplicate customer records' },
  { Icon: ClockIcon, text: 'Hours spent on billing' },
]

const NEW_WAY = [
  { Icon: BrainIcon, text: 'AI-powered document extraction' },
  { Icon: ZapIcon, text: 'Automated invoice generation' },
  { Icon: CloudIcon, text: 'Cloud-based, access anywhere' },
  { Icon: TrendUpIcon, text: 'Real-time financial analytics' },
  { Icon: BuildingIcon, text: 'Multi-tenant org management' },
  { Icon: ShieldIcon, text: 'Secure, role-based access' },
]

export default function Problem() {
  const oldCardRef = useRef<HTMLDivElement>(null)
  const newCardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const oldEl = oldCardRef.current
    const newEl = newCardRef.current
    if (!oldEl || !newEl) return

    // GSAP Staggered reveals with opacity ceiling contrast
    const ctx = gsap.context(() => {
      // Set initial states
      gsap.set(oldEl, { opacity: 0, y: 30 })
      gsap.set(newEl, { opacity: 0, y: 30 })

      ScrollTrigger.create({
        trigger: oldEl,
        start: 'top 85%',
        once: true,
        onEnter: () => {
          // Old way card fades in first, lower opacity ceiling ~85%
          gsap.to(oldEl, {
            opacity: 0.85,
            y: 0,
            duration: 0.8,
            ease: 'power3.out',
          })

          // TravelBilling Way card follows, sharper, fully-opaque
          gsap.to(newEl, {
            opacity: 1,
            y: 0,
            duration: 0.8,
            delay: 0.25,
            ease: 'power2.out',
          })
        }
      })
    })

    return () => ctx.revert()
  }, [])

  return (
    <section className="py-16 md:py-20 bg-white" id="problem">
      <div className="section-container max-w-5xl">
        <AnimatedSection className="text-center mb-12">
          <AnimatedText as="h2" className="headline-lg font-display text-text-primary">
            Travel billing is broken.
          </AnimatedText>
          <p className="body-large mt-4 max-w-xl mx-auto">
            Most travel agencies still rely on manual processes that waste time, create errors, and limit growth.
          </p>
        </AnimatedSection>

        <div className="grid md:grid-cols-2 gap-6 max-w-4xl mx-auto">
          {/* Old Way Card - Desaturated & Low Contrast */}
          <div ref={oldCardRef} className="h-full">
            <GlassCard className="p-8 h-full bg-[#FAFAFA]/65 border-border-light" variant="subtle">
              <span className="inline-block text-[10px] font-bold uppercase tracking-widest text-text-tertiary bg-black/[0.03] px-3 py-1 rounded-full mb-6 border border-border-light select-none">
                The Old Way
              </span>
              <ul className="space-y-4">
                {OLD_WAY.map(({ Icon, text }) => (
                  <li key={text} className="flex items-start gap-3 opacity-60">
                    <Icon size={16} className="text-text-tertiary shrink-0 mt-0.5" aria-hidden="true" />
                    <span className="text-[13px] text-text-secondary leading-relaxed line-through decoration-text-tertiary/45">
                      {text}
                    </span>
                  </li>
                ))}
              </ul>
            </GlassCard>
          </div>

          {/* TravelBilling Way Card - Elevated & Highly Prominent */}
          <div ref={newCardRef} className="h-full">
            <GlassCard className="p-8 h-full border border-black/10 shadow-[0_12px_30px_-10px_rgba(0,0,0,0.06)] ring-1 ring-black/[0.03]" variant="strong">
              <span className="inline-block text-[10px] font-bold uppercase tracking-widest text-white bg-black px-3 py-1 rounded-full mb-6 select-none">
                The TravelBilling Way
              </span>
              <ul className="space-y-4">
                {NEW_WAY.map(({ Icon, text }) => (
                  <li key={text} className="flex items-start gap-3">
                    {/* Dark prominent icon chip */}
                    <div className="w-5 h-5 rounded-full bg-black text-white flex items-center justify-center shrink-0 mt-0.5">
                      <Icon size={12} aria-hidden="true" />
                    </div>
                    <span className="text-[13.5px] text-text-primary leading-relaxed font-semibold">
                      {text}
                    </span>
                  </li>
                ))}
              </ul>
            </GlassCard>
          </div>
        </div>
      </div>
    </section>
  )
}
