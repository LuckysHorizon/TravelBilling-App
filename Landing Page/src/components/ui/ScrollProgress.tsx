import { useRef, useEffect } from 'react'

/**
 * ScrollProgress — A subtle, fixed progress indicator at the very top
 * of the page that reflects overall scroll progress (0 → 1).
 *
 * Reads `progressRef.current` inside a rAF loop so it stays in sync
 * with the 3D scene without causing React re-renders.
 */

interface ScrollProgressProps {
  progressRef: React.MutableRefObject<number>
}

export default function ScrollProgress({ progressRef }: ScrollProgressProps) {
  const barRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let rafId: number

    const tick = () => {
      if (barRef.current) {
        const pct = (progressRef.current ?? 0) * 100
        barRef.current.style.width = `${pct}%`
      }
      rafId = requestAnimationFrame(tick)
    }

    rafId = requestAnimationFrame(tick)

    return () => cancelAnimationFrame(rafId)
  }, [progressRef])

  return (
    <div className="pointer-events-none fixed top-0 left-0 z-50 h-[3px] w-full">
      <div
        ref={barRef}
        className="h-full bg-brand-blue"
        style={{ width: '0%', willChange: 'width' }}
      />
    </div>
  )
}
