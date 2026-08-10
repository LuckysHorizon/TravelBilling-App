/**
 * useSmoothScroll — GSAP-powered smooth scroll for anchor links
 *
 * Intercepts clicks on <a href="#..."> links and animates the scroll
 * position using GSAP ScrollToPlugin.
 *
 * Kills any in-flight tween on rapid clicks so animations never overlap.
 */
import { useCallback, useRef } from 'react'
import { gsap, EASE } from '../lib/gsapConfig'

interface SmoothScrollOptions {
  /** Scroll duration in seconds (default: 0.9) */
  duration?: number
  /** Offset from top of target in pixels — accounts for floating navbar (default: -80) */
  offsetY?: number
}

export function useSmoothScroll(options: SmoothScrollOptions = {}) {
  const {
    duration = 0.9,
    offsetY = -80,
  } = options

  const activeTween = useRef<gsap.core.Tween | null>(null)

  const scrollTo = useCallback(
    (target: string | HTMLElement, e?: React.MouseEvent) => {
      if (e) e.preventDefault()

      // Resolve target element
      const el =
        typeof target === 'string'
          ? document.querySelector(target)
          : target

      if (!el) return

      // Kill any in-flight scroll animation to handle rapid clicks
      if (activeTween.current) {
        activeTween.current.kill()
        activeTween.current = null
      }

      // Animate scroll
      activeTween.current = gsap.to(window, {
        scrollTo: {
          y: el,
          offsetY: Math.abs(offsetY),
          autoKill: true,
        },
        duration,
        ease: EASE.smooth,
        onComplete: () => {
          activeTween.current = null
        },
      })

      // Update URL hash without jumping
      if (typeof target === 'string') {
        history.pushState(null, '', target)
      }
    },
    [duration, offsetY]
  )

  /**
   * Click handler factory for anchor elements.
   * Usage: <a href="#features" onClick={handleAnchorClick}>
   */
  const handleAnchorClick = useCallback(
    (e: React.MouseEvent<HTMLAnchorElement>) => {
      const href = e.currentTarget.getAttribute('href')
      if (href && href.startsWith('#') && href.length > 1) {
        scrollTo(href, e)
      }
    },
    [scrollTo]
  )

  return { scrollTo, handleAnchorClick }
}
