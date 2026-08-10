/**
 * AnimatedSection — GSAP ScrollTrigger-powered reveal
 *
 * Replaces Framer Motion whileInView with GSAP for
 * buttery-smooth, hardware-accelerated scroll reveals.
 * Supports directional entry, stagger, parallax, and scale.
 */
import { useRef, useEffect } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'

interface AnimatedSectionProps {
  children: React.ReactNode
  className?: string
  delay?: number
  direction?: 'up' | 'down' | 'left' | 'right' | 'none'
  /** Scale from this value to 1 */
  scale?: number
  /** Parallax speed multiplier (0 = none, 0.2 = subtle) */
  parallax?: number
  /** Trigger start position */
  start?: string
}

const directionMap = {
  up: { y: 80, x: 0 },
  down: { y: -80, x: 0 },
  left: { x: 80, y: 0 },
  right: { x: -80, y: 0 },
  none: { x: 0, y: 0 },
}

export default function AnimatedSection({
  children,
  className = '',
  delay = 0,
  direction = 'up',
  scale,
  parallax,
  start = 'top 88%',
}: AnimatedSectionProps) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return

    const offset = directionMap[direction]
    const ctx = gsap.context(() => {
      // Initial state
      gsap.set(el, {
        opacity: 0,
        ...offset,
        scale: scale ?? 1,
      })

      // Reveal tween
      gsap.to(el, {
        opacity: 1,
        x: 0,
        y: 0,
        scale: 1,
        duration: 1,
        delay,
        ease: 'power3.out',
        scrollTrigger: {
          trigger: el,
          start,
          toggleActions: 'play none none none',
        },
      })

      // Optional parallax
      if (parallax) {
        gsap.to(el, {
          y: () => parallax * -100,
          ease: 'none',
          scrollTrigger: {
            trigger: el,
            start: 'top bottom',
            end: 'bottom top',
            scrub: 1.5,
          },
        })
      }
    }, el)

    return () => ctx.revert()
  }, [delay, direction, scale, parallax, start])

  return (
    <div ref={ref} className={`gsap-reveal ${className}`}>
      {children}
    </div>
  )
}
