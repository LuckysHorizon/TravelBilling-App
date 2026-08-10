import { useState, useEffect } from 'react'
import { useProgress } from '@react-three/drei'

/**
 * Loader
 * ------
 * Full-screen branded loading overlay rendered **outside** the R3F Canvas.
 *
 * - Displays the "RamnetSolutions" brand name in Space Grotesk with a pulse animation
 * - Shows a progress bar driven by drei's `useProgress` hook
 * - Fades out gracefully 500 ms after loading completes via the
 *   `.loader-screen.loaded` CSS transition defined in globals.css
 */
export default function Loader() {
  const { progress, active } = useProgress()
  const [visible, setVisible] = useState(true)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    // Once assets are fully loaded and useProgress reports inactive,
    // wait a short beat before triggering the fade-out transition
    if (progress >= 100 && !active) {
      const timer = setTimeout(() => setLoaded(true), 500)
      return () => clearTimeout(timer)
    }
  }, [progress, active])

  // After the fade-out animation completes, unmount entirely
  useEffect(() => {
    if (loaded) {
      const unmountTimer = setTimeout(() => setVisible(false), 800)
      return () => clearTimeout(unmountTimer)
    }
  }, [loaded])

  if (!visible) return null

  return (
    <div className={`loader-screen ${loaded ? 'loaded' : ''}`}>
      {/* ── Brand logo ── */}
      <h1
        style={{
          fontFamily: "'Space Grotesk', sans-serif",
          fontSize: '2rem',
          fontWeight: 700,
          color: '#1f4fd8',
          marginBottom: '2rem',
          letterSpacing: '-0.02em',
          animation: 'loaderPulse 2s ease-in-out infinite',
        }}
      >
        RamnetSolutions
      </h1>

      {/* ── Progress bar track ── */}
      <div
        style={{
          width: '260px',
          height: '4px',
          borderRadius: '9999px',
          backgroundColor: 'rgba(31, 79, 216, 0.12)',
          overflow: 'hidden',
          marginBottom: '1rem',
        }}
      >
        {/* ── Progress bar fill ── */}
        <div
          style={{
            width: `${progress}%`,
            height: '100%',
            borderRadius: '9999px',
            backgroundColor: '#1f4fd8',
            transition: 'width 0.3s ease-out',
          }}
        />
      </div>

      {/* ── Percentage text ── */}
      <p
        style={{
          fontFamily: "'Space Grotesk', sans-serif",
          fontSize: '0.875rem',
          color: '#6b7280',
          fontVariantNumeric: 'tabular-nums',
        }}
      >
        {Math.round(progress)}%
      </p>

      {/* ── Inline keyframes for the pulse animation ── */}
      <style>{`
        @keyframes loaderPulse {
          0%, 100% { opacity: 1; transform: scale(1); }
          50%      { opacity: 0.7; transform: scale(1.03); }
        }
      `}</style>
    </div>
  )
}
