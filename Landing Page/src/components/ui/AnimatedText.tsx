/**
 * AnimatedText — GSAP-powered split-text word reveal
 *
 * Splits children text into words, each wrapped in an overflow-hidden
 * container. GSAP ScrollTrigger animates each word from below with
 * stagger for a premium cascading reveal effect.
 */
import { useRef, useEffect } from 'react'
import { gsap, ScrollTrigger } from '../../lib/gsapConfig'

interface AnimatedTextProps {
  children: string
  className?: string
  as?: 'h1' | 'h2' | 'h3' | 'p' | 'span'
  /** Stagger delay between words */
  stagger?: number
  /** Start trigger position */
  start?: string
}

export default function AnimatedText({
  children,
  className = '',
  as: Tag = 'h2',
  stagger = 0.04,
  start = 'top 85%',
}: AnimatedTextProps) {
  const containerRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const el = containerRef.current
    if (!el) return

    const wordEls = el.querySelectorAll('.word-inner')

    const ctx = gsap.context(() => {
      gsap.set(wordEls, { y: '110%', opacity: 0 })

      ScrollTrigger.create({
        trigger: el,
        start,
        once: true,
        onEnter: () => {
          gsap.to(wordEls, {
            y: '0%',
            opacity: 1,
            duration: 0.8,
            stagger,
            ease: 'power4.out',
          })
        },
      })
    }, el)

    return () => ctx.revert()
  }, [children, stagger, start])

  const words = children.split(' ')

  return (
    <Tag ref={containerRef as React.Ref<HTMLHeadingElement>} className={className}>
      {words.map((word, i) => (
        <span
          key={`${word}-${i}`}
          className="inline-block overflow-hidden mr-[0.25em] last:mr-0"
          style={{ verticalAlign: 'top' }}
        >
          <span className="word-inner inline-block">{word}</span>
        </span>
      ))}
    </Tag>
  )
}
