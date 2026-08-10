import { useRef, useEffect } from 'react'
import { useFrame, useThree } from '@react-three/fiber'
import * as THREE from 'three'
import {
  lerpKeyframes,
  lerpKeyframeScalar,
  type Keyframe,
} from '../../hooks/useScrollTimeline'

/* ─── Camera position keyframes ─── */
const cameraPositionKeyframes: Keyframe<[number, number, number]>[] = [
  { progress: 0, value: [0, 2, 8] },          // Hero: front view
  { progress: 0.15, value: [6, 3, 5] },        // About: orbit around
  { progress: 0.3, value: [-5, 4, 9] },        // Fleet: pulled back, showing scale
  { progress: 0.45, value: [2, 1.5, 4] },      // Cabin: close up
  { progress: 0.6, value: [-3, 2, -4] },       // Destinations: chase angle
  { progress: 0.75, value: [0, 4, 14] },       // Testimonials: far back, plane recedes
  { progress: 0.875, value: [3, 3, 11] },      // Booking: dramatic angle
  { progress: 1, value: [0, 6, 18] },          // Footer: very far
 ]
 
 /* ─── LookAt target keyframes ─── */
const lookAtKeyframes: Keyframe<[number, number, number]>[] = [
  { progress: 0, value: [0, 0, 0] },
  { progress: 0.15, value: [0, 0, 0] },
  { progress: 0.3, value: [0, 0, 0] },
  { progress: 0.45, value: [0, 0.5, 0] },
  { progress: 0.6, value: [0, 0, -2] },
  { progress: 0.75, value: [0, 0, 0] },
  { progress: 0.875, value: [0, 0, 0] },
  { progress: 1, value: [0, 0, 0] },
 ]
 
 /* ─── FOV keyframes (scalar) ─── */
const fovKeyframes: Keyframe<number>[] = [
  { progress: 0, value: 45 },
  { progress: 0.3, value: 50 },
  { progress: 0.45, value: 35 },
  { progress: 0.6, value: 50 },
  { progress: 0.75, value: 45 },
  { progress: 1, value: 45 },
]

interface CameraRigProps {
  progressRef: React.MutableRefObject<number>
}

export default function CameraRig({ progressRef }: CameraRigProps) {
  const { camera } = useThree()

  // Reusable Vector3 for lookAt — avoids GC pressure every frame
  const lookAtTarget = useRef(new THREE.Vector3())

  // Mouse position for hero parallax (normalised -1 → 1)
  const mouse = useRef({ x: 0, y: 0 })
  // Smoothed mouse offset to prevent jerky movement
  const smoothMouse = useRef({ x: 0, y: 0 })

  /* ── Track mouse position for parallax ── */
  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      mouse.current.x = (e.clientX / window.innerWidth) * 2 - 1
      mouse.current.y = (e.clientY / window.innerHeight) * 2 - 1
    }
    window.addEventListener('mousemove', onMouseMove, { passive: true })
    return () => window.removeEventListener('mousemove', onMouseMove)
  }, [])

  /* ── Per-frame camera update ── */
  useFrame(() => {
    const t = progressRef.current
    const perspCamera = camera as THREE.PerspectiveCamera

    // ── Position ──
    const pos = lerpKeyframes(cameraPositionKeyframes, t) as [number, number, number]

    // Subtle sine-wave "breathing" drift layered on camera Y
    const elapsed = performance.now() * 0.001
    const breathY = Math.sin(elapsed * 0.4) * 0.05

    let finalX = pos[0]
    let finalY = pos[1] + breathY
    let finalZ = pos[2]

    // Mouse parallax — hero section only (progress < 0.12)
    if (t < 0.12) {
      const factor = 0.3
      smoothMouse.current.x += (mouse.current.x - smoothMouse.current.x) * 0.05
      smoothMouse.current.y += (mouse.current.y - smoothMouse.current.y) * 0.05
      finalX += smoothMouse.current.x * factor
      finalY += -smoothMouse.current.y * factor * 0.5 // inverted Y feels natural
    }

    perspCamera.position.set(finalX, finalY, finalZ)

    // ── LookAt ──
    const look = lerpKeyframes(lookAtKeyframes, t) as [number, number, number]
    lookAtTarget.current.set(look[0], look[1], look[2])
    perspCamera.lookAt(lookAtTarget.current)

    // ── FOV ──
    const newFov = lerpKeyframeScalar(fovKeyframes, t)
    if (Math.abs(perspCamera.fov - newFov) > 0.01) {
      perspCamera.fov = newFov
      perspCamera.updateProjectionMatrix()
    }
  })

  // CameraRig is purely imperative — renders nothing to the scene graph
  return null
}
