/**
 * useResponsiveScene
 * ==================
 * Returns scale, position adjustments, and feature flags based on
 * viewport width. Used by canvas components to adapt for mobile.
 */

import { useEffect, useState } from 'react'

export interface ResponsiveConfig {
  /** Model scale multiplier */
  modelScale: number
  /** Model position offset [x, y, z] */
  modelOffset: [number, number, number]
  /** Particle count multiplier (0-1) */
  particleDensity: number
  /** Whether to enable post-processing */
  enablePostProcessing: boolean
  /** Whether device is mobile */
  isMobile: boolean
  /** Whether device is tablet */
  isTablet: boolean
  /** DPR cap for the canvas */
  dprMax: number
}

export function useResponsiveScene(): ResponsiveConfig {
  const [config, setConfig] = useState<ResponsiveConfig>(getConfig())

  useEffect(() => {
    let timeout: ReturnType<typeof setTimeout>

    const handleResize = () => {
      clearTimeout(timeout)
      timeout = setTimeout(() => {
        setConfig(getConfig())
      }, 150) // Debounced
    }

    window.addEventListener('resize', handleResize)
    return () => {
      clearTimeout(timeout)
      window.removeEventListener('resize', handleResize)
    }
  }, [])

  return config
}

function getConfig(): ResponsiveConfig {
  const w = typeof window !== 'undefined' ? window.innerWidth : 1920

  if (w < 640) {
    // Mobile
    return {
      modelScale: 0.6,
      modelOffset: [0, -0.5, 0],
      particleDensity: 0.3,
      enablePostProcessing: false,
      isMobile: true,
      isTablet: false,
      dprMax: 1.5,
    }
  }

  if (w < 1024) {
    // Tablet
    return {
      modelScale: 0.8,
      modelOffset: [0, -0.3, 0],
      particleDensity: 0.6,
      enablePostProcessing: true,
      isMobile: false,
      isTablet: true,
      dprMax: 1.5,
    }
  }

  // Desktop
  return {
    modelScale: 1,
    modelOffset: [0, 0, 0],
    particleDensity: 1,
    enablePostProcessing: true,
    isMobile: false,
    isTablet: false,
    dprMax: 2,
  }
}
