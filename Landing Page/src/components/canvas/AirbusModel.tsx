import { useRef, useEffect } from 'react'
import { useFrame } from '@react-three/fiber'
import { useGLTF } from '@react-three/drei'
import * as THREE from 'three'
import { lerpKeyframes, type Keyframe } from '../../hooks/useScrollTimeline'
import { useResponsiveScene } from '../../hooks/useResponsiveScene'

/* ─── Scroll-driven position keyframes ─── */
const positionKeyframes: Keyframe<[number, number, number]>[] = [
  { progress: 0, value: [0, 0, 0] },          // Hero: centered
  { progress: 0.15, value: [0, 0, 0] },        // About: stays centered
  { progress: 0.3, value: [0.5, 0, 0] },       // Fleet: slight offset
  { progress: 0.45, value: [0, 0, 0] },         // Cabin
  { progress: 0.6, value: [0, -0.3, -2] },      // Destinations: appears to fly forward
  { progress: 0.75, value: [0, 0, 0] },         // Testimonials
  { progress: 0.875, value: [0, 0, 0] },        // Booking CTA
  { progress: 1, value: [0, -1, 0] },           // Footer: sinks down
]

/* ─── Scroll-driven rotation keyframes ─── */
const rotationKeyframes: Keyframe<[number, number, number]>[] = [
  { progress: 0, value: [0, 0, 0] },
  { progress: 0.15, value: [0, Math.PI * 0.4, 0] },        // About: rotated to show profile
  { progress: 0.3, value: [0.06, -Math.PI * 0.15, 0.12] },  // Fleet: banking tilt
  { progress: 0.45, value: [0, Math.PI * 0.1, 0] },         // Cabin
  { progress: 0.6, value: [0.03, Math.PI * 0.8, 0.06] },    // Destinations: rotated, flying
  { progress: 0.75, value: [0, 0, 0] },                     // Testimonials
  { progress: 0.875, value: [0, -Math.PI * 0.2, 0.03] },    // Booking: slight angle
  { progress: 1, value: [0, 0, 0] },                        // Footer
]

interface AirbusModelProps {
  progressRef: React.MutableRefObject<number>
}

export default function AirbusModel({ progressRef }: AirbusModelProps) {
  const { scene } = useGLTF('/models/airbus.glb')
  const groupRef = useRef<THREE.Group>(null!)
  const { modelScale, modelOffset } = useResponsiveScene()

  /* ── Enable castShadow on every mesh in the loaded model ── */
  useEffect(() => {
    scene.traverse((child) => {
      if ((child as THREE.Mesh).isMesh) {
        child.castShadow = true
      }
    })
  }, [scene])

  /* ── Per-frame scroll interpolation + idle animation ── */
  useFrame((_, delta) => {
    if (!groupRef.current) return

    const t = progressRef.current

    // Interpolate position from keyframes
    const pos = lerpKeyframes(positionKeyframes, t) as [number, number, number]
    // Interpolate rotation from keyframes
    const rot = lerpKeyframes(rotationKeyframes, t) as [number, number, number]

    // Apply position with responsive offset
    groupRef.current.position.set(
      pos[0] + modelOffset[0],
      pos[1] + modelOffset[1],
      pos[2] + modelOffset[2],
    )

    // Apply rotation
    groupRef.current.rotation.set(rot[0], rot[1], rot[2])

    // Subtle idle sinusoidal yaw — only active in the hero section (progress < 0.15)
    if (t < 0.15) {
      const elapsed = performance.now() * 0.001
      const idleYaw = Math.sin(elapsed * 0.6) * 0.02
      groupRef.current.rotation.y += idleYaw
    }

    // Apply responsive scale
    groupRef.current.scale.setScalar(modelScale)
  })

  return (
    <group ref={groupRef}>
      <primitive object={scene} />
    </group>
  )
}

/* ── Preload the model for faster initial render ── */
useGLTF.preload('/models/airbus.glb')
