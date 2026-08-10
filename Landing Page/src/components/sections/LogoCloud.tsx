/**
 * LogoCloud — Marquee of travel company logos
 * Infinite horizontal scroll using GSAP, pauses on hover, fades at edges.
 */
import { useEffect, useRef } from 'react'
import { gsap } from '../../lib/gsapConfig'
import AnimatedSection from '../ui/AnimatedSection'

// TODO: confirm brand usage rights
const logos = [
  { name: 'MakeMyTrip', src: '/logos/makemytrip.svg', width: 130 },
  { name: 'Yatra', src: '/logos/yatra.svg', width: 100 },
  { name: 'ClearTrip', src: '/logos/cleartrip.svg', width: 120 },
  { name: 'Amadeus', src: '/logos/amadeus.svg', width: 120 },
  { name: 'Sabre', src: '/logos/sabre.svg', width: 110 },
  { name: 'Travelport', src: '/logos/travelport.svg', width: 120 },
  { name: 'Booking.com', src: '/logos/booking.svg', width: 130 },
  { name: 'TripAdvisor', src: '/logos/tripadvisor.svg', width: 130 },
]

export default function LogoCloud() {
  const marqueeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = marqueeRef.current
    if (!el) return

    // GSAP infinite marquee loop
    const tween = gsap.to(el, {
      xPercent: -50,
      repeat: -1,
      duration: 30, // Slow, premium speed
      ease: 'none',
    })

    // Pause on hover
    const onMouseEnter = () => tween.pause()
    const onMouseLeave = () => tween.play()

    el.addEventListener('mouseenter', onMouseEnter)
    el.addEventListener('mouseleave', onMouseLeave)

    return () => {
      tween.kill()
      el.removeEventListener('mouseenter', onMouseEnter)
      el.removeEventListener('mouseleave', onMouseLeave)
    }
  }, [])

  // Duplicate logos for seamless loop
  const marqueeLogos = [...logos, ...logos]

  return (
    <section className="py-10 md:py-14 border-y border-border-light bg-surface-tertiary overflow-hidden w-full relative select-none">
      <AnimatedSection direction="none" className="section-container text-center px-6 mb-10">
        <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-text-tertiary">
          Trusted by leading travel companies
        </p>
      </AnimatedSection>

      {/* Fade mask for premium gradient clipping */}
      <div className="w-full overflow-hidden [mask-image:linear-gradient(to_right,transparent_0%,black_15%,black_85%,transparent_100%)]">
        <div 
          ref={marqueeRef}
          className="flex gap-16 w-max items-center will-change-transform cursor-pointer"
        >
          {marqueeLogos.map((logo, i) => (
            <div
              key={`${logo.name}-${i}`}
              className="flex items-center justify-center shrink-0"
              style={{ width: `${logo.width}px` }}
            >
              <img
                src={logo.src}
                alt={logo.name}
                className="h-7 w-auto opacity-80 hover:opacity-100 transition-all duration-300"
                draggable="false"
              />
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
