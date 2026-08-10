/**
 * AISpotlight — Dark section showcasing PDF extraction, monochrome
 */
import { useRef, useEffect } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'
import AnimatedText from '../ui/AnimatedText'
import { FileTextIcon } from '../ui/Icons'
import { Badge } from '../ui/badge'

const EXTRACT_FIELDS = [
  { label: 'Passenger Name', value: 'Rajesh Kumar', confidence: 99 },
  { label: 'Ticket Number', value: '098-2481937620', confidence: 97 },
  { label: 'Travel Date', value: '15 Mar 2025', confidence: 98 },
  { label: 'Route', value: 'DEL → BOM', confidence: 96 },
  { label: 'Airline', value: 'Air India', confidence: 99 },
  { label: 'Amount', value: '₹8,450.00', confidence: 98 },
]

export default function AISpotlight() {
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = cardRef.current
    if (!el) return

    const rows = el.querySelectorAll('.extract-row')
    const ctx = gsap.context(() => {
      gsap.set(rows, { opacity: 0, x: 30 })

      ScrollTrigger.create({
        trigger: el,
        start: 'top 70%',
        once: true,
        onEnter: () => {
          gsap.to(rows, {
            opacity: 1,
            x: 0,
            duration: 0.5,
            stagger: 0.08,
            ease: 'power3.out',
          })
        },
      })
    }, el)

    return () => ctx.revert()
  }, [])

  return (
    <section className="section-padding bg-black text-white relative overflow-hidden" id="ai-spotlight">
      <div className="gradient-mesh">
        <div className="gradient-orb w-[500px] h-[500px] bg-white/[0.04] top-[-100px] right-[10%]" />
        <div className="gradient-orb w-[400px] h-[400px] bg-white/[0.03] bottom-[-100px] left-[5%]" />
      </div>

      <div className="section-container relative z-10">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          {/* Copy */}
          <AnimatedSection direction="left">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/40 mb-3">
              AI Innovation
            </p>
            <AnimatedText as="h2" className="headline-lg font-display text-white">
              Upload a PDF. Get structured data.
            </AnimatedText>
            <p className="text-[17px] text-white/50 mt-5 leading-relaxed max-w-lg">
              Our AI-powered extraction engine reads airline tickets, hotel
              invoices, booking confirmations, and visa documents — pulling
              passenger names, amounts, dates, and references automatically.
            </p>
            <div className="flex flex-wrap gap-2 mt-8">
              {['Airline Tickets', 'Hotel Invoices', 'Visa Documents', 'Booking Confirmations'].map((tag) => (
                <Badge key={tag} variant="outline" className="text-white/60 bg-white/[0.06] border-white/[0.08] px-3 py-1 text-xs font-medium rounded-full">
                  {tag}
                </Badge>
              ))}
            </div>
            <div className="flex items-center gap-8 mt-8">
              {[
                { val: '98%', label: 'Accuracy' },
                { val: '<3s', label: 'Processing' },
                { val: '80%', label: 'Time Saved' },
              ].map((s) => (
                <div key={s.label}>
                  <p className="text-3xl font-bold text-white">{s.val}</p>
                  <p className="text-[11px] text-white/30 mt-0.5">{s.label}</p>
                </div>
              ))}
            </div>
          </AnimatedSection>

          {/* Extraction card */}
          <AnimatedSection delay={0.2} direction="right">
            <div ref={cardRef} className="liquid-glass-dark rounded-2xl p-6">
              <div className="flex items-center gap-3 mb-5 pb-4 border-b border-white/[0.06]">
                <div className="w-10 h-10 rounded-xl bg-white/[0.08] flex items-center justify-center">
                  <FileTextIcon size={18} className="text-white/70" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-white">e-ticket_AI123.pdf</p>
                  <p className="text-[10px] text-white/30">Processed in 2.1 seconds</p>
                </div>
                <Badge className="ml-auto text-[10px] bg-check/10 text-check hover:bg-check/20 border-transparent rounded-full px-2 py-0.5 pointer-events-none">
                  ✓ Extracted
                </Badge>
              </div>
              <div className="space-y-3">
                {EXTRACT_FIELDS.map((field) => (
                  <div key={field.label} className="extract-row flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-1 h-1 rounded-full bg-white/30" />
                      <span className="text-[11px] text-white/40 w-28">{field.label}</span>
                    </div>
                    <span className="text-[13px] font-medium text-white flex-1">{field.value}</span>
                    <span className="text-[10px] text-white/50 font-mono">{field.confidence}%</span>
                  </div>
                ))}
              </div>
            </div>
          </AnimatedSection>
        </div>
      </div>
    </section>
  )
}
