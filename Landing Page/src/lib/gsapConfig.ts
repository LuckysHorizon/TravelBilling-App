/**
 * GSAP Plugin Registration & Ease Presets
 * ========================================
 * Import this module once at app root to register ScrollTrigger.
 * Also exports shared ease presets for consistent motion language.
 */

import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { ScrollToPlugin } from 'gsap/ScrollToPlugin'

// Register plugins once
gsap.registerPlugin(ScrollTrigger, ScrollToPlugin)

// ── Ease Presets ──────────────────────────────────────────────────
export const EASE = {
  smooth: 'power3.inOut',
  smoothOut: 'power3.out',
  smoothIn: 'power3.in',
  dramatic: 'power4.inOut',
  dramaticOut: 'power4.out',
  soft: 'power2.inOut',
  softOut: 'power2.out',
  bounce: 'back.out(1.4)',
} as const

// ── Defaults ──────────────────────────────────────────────────────
gsap.defaults({
  ease: EASE.smooth,
  duration: 1,
})

export { gsap, ScrollTrigger }
