/**
 * useScrollTimeline
 * =================
 * Central hook that creates ONE GSAP ScrollTrigger-driven progress value
 * scrubbed from 0 → 1 across the entire page scroll distance.
 *
 * Components (CameraRig, AirbusModel) read progressRef.current in useFrame
 * to interpolate their positions/rotations without fighting each other.
 */

import { useEffect, useRef } from 'react'
import { gsap, ScrollTrigger } from '../lib/gsapConfig'

export interface ScrollTimelineResult {
  /** Current scroll progress 0 → 1 (read in useFrame) */
  progressRef: React.MutableRefObject<number>
  /** Section label positions mapped to progress values */
  sections: Record<string, number>
}

/** Maps section names to their normalized progress midpoints */
const SECTION_MAP = {
  hero: 0,
  about: 0.125,
  fleet: 0.25,
  cabin: 0.375,
  destinations: 0.5,
  testimonials: 0.625,
  booking: 0.75,
  footer: 0.875,
} as const

export function useScrollTimeline(): ScrollTimelineResult {
  const progressRef = useRef(0)

  useEffect(() => {
    // The scroll-sections wrapper determines total scroll distance.
    // Each section is 100vh, total = 800vh.
    const scrollWrapper = document.querySelector('.scroll-sections')
    if (!scrollWrapper) return

    const obj = { value: 0 }

    const tween = gsap.to(obj, {
      value: 1,
      ease: 'none', // Linear mapping — easing happens in consumers
      scrollTrigger: {
        trigger: scrollWrapper,
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.5, // Smooth scrub with 1.5s lag for buttery feel
        onUpdate: (self) => {
          progressRef.current = self.progress
        },
      },
    })

    return () => {
      tween.kill()
      ScrollTrigger.getAll().forEach((st) => st.kill())
    }
  }, [])

  return {
    progressRef,
    sections: SECTION_MAP,
  }
}

// ── Utility: Interpolation helpers for keyframe arrays ───────────

export interface Keyframe<T> {
  progress: number
  value: T
}

/**
 * Linearly interpolate between keyframe values based on progress.
 * Returns the interpolated value at the given progress point.
 */
export function lerpKeyframes(
  keyframes: Keyframe<[number, number, number]>[],
  progress: number
): [number, number, number] {
  if (keyframes.length === 0) return [0, 0, 0]
  if (progress <= keyframes[0].progress) return [...keyframes[0].value]
  if (progress >= keyframes[keyframes.length - 1].progress)
    return [...keyframes[keyframes.length - 1].value]

  // Find the two keyframes we're between
  for (let i = 0; i < keyframes.length - 1; i++) {
    const a = keyframes[i]
    const b = keyframes[i + 1]
    if (progress >= a.progress && progress <= b.progress) {
      const t = (progress - a.progress) / (b.progress - a.progress)
      // Smooth-step for more organic feel
      const st = t * t * (3 - 2 * t)
      return [
        a.value[0] + (b.value[0] - a.value[0]) * st,
        a.value[1] + (b.value[1] - a.value[1]) * st,
        a.value[2] + (b.value[2] - a.value[2]) * st,
      ]
    }
  }

  return [...keyframes[keyframes.length - 1].value]
}

/**
 * Interpolate a single numeric value across keyframes.
 */
export function lerpKeyframeScalar(
  keyframes: Keyframe<number>[],
  progress: number
): number {
  if (keyframes.length === 0) return 0
  if (progress <= keyframes[0].progress) return keyframes[0].value
  if (progress >= keyframes[keyframes.length - 1].progress)
    return keyframes[keyframes.length - 1].value

  for (let i = 0; i < keyframes.length - 1; i++) {
    const a = keyframes[i]
    const b = keyframes[i + 1]
    if (progress >= a.progress && progress <= b.progress) {
      const t = (progress - a.progress) / (b.progress - a.progress)
      const st = t * t * (3 - 2 * t)
      return a.value + (b.value - a.value) * st
    }
  }

  return keyframes[keyframes.length - 1].value
}
