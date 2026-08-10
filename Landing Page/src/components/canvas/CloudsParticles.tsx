import { useRef, useMemo } from 'react'
import { useFrame } from '@react-three/fiber'
import { Sparkles } from '@react-three/drei'
import * as THREE from 'three'
import { useResponsiveScene } from '../../hooks/useResponsiveScene'

interface CloudsParticlesProps {
  progressRef: React.MutableRefObject<number>
}

/**
 * CloudsParticles
 * ---------------
 * Atmospheric sparkle/particle layer that adds depth and motion to the scene.
 *
 * - Particle count scales with `particleDensity` from useResponsiveScene
 * - Gentle continuous rotation creates a slow drifting effect
 * - During the "destinations" scroll range (0.5–0.65) the rotation
 *   speed increases to reinforce the sense of flight
 */
export default function CloudsParticles({ progressRef }: CloudsParticlesProps) {
  const groupRef = useRef<THREE.Group>(null!)
  const { particleDensity } = useResponsiveScene()

  // Particle count derived from responsive density factor
  const count = useMemo(
    () => Math.round(80 * particleDensity),
    [particleDensity],
  )

  useFrame(() => {
    if (!groupRef.current) return

    const t = progressRef.current
    const elapsed = performance.now() * 0.001

    // Base rotation speed — slow, ambient drift
    let speed = 0.015

    // Increase drift during the destinations section (flying feel)
    if (t > 0.5 && t < 0.65) {
      speed = 0.04
    }

    groupRef.current.rotation.y = elapsed * speed
    groupRef.current.rotation.x = Math.sin(elapsed * 0.1) * 0.02
  })

  return (
    <group ref={groupRef}>
      <Sparkles
        count={count}
        size={2}
        color="#dce8fb"
        speed={0.3}
        opacity={0.4}
        scale={20}
      />
    </group>
  )
}
