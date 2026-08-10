import { Suspense } from 'react'
import { Canvas } from '@react-three/fiber'
import { EffectComposer, Bloom } from '@react-three/postprocessing'
import AirbusModel from './AirbusModel'
import CameraRig from './CameraRig'
import Lighting from './Lighting'
import CloudsParticles from './CloudsParticles'
import { useResponsiveScene } from '../../hooks/useResponsiveScene'

interface ExperienceProps {
  progressRef: React.MutableRefObject<number>
}

/**
 * Experience
 * ----------
 * Top-level R3F `<Canvas>` wrapper that assembles the complete 3D scene.
 *
 * - Responsive DPR clamping via `useResponsiveScene`
 * - Daytime sky-gradient background painted directly on the GL canvas
 * - Conditional post-processing (Bloom only) for devices that can handle it
 * - The `<Loader>` component is **not** rendered here — it lives alongside
 *   the Canvas in App.tsx so it can overlay as plain HTML
 */
export default function Experience({ progressRef }: ExperienceProps) {
  const { dprMax, enablePostProcessing } = useResponsiveScene()

  return (
    <div className="canvas-container">
      <Canvas
        dpr={[1, dprMax]}
        camera={{ position: [0, 2, 8], fov: 45, near: 0.1, far: 100 }}
        style={{
          background:
            'linear-gradient(180deg, #f5f8fc 0%, #eaf2ff 50%, #dce8fb 100%)',
        }}
        gl={{ antialias: true, alpha: false }}
      >
        <Suspense fallback={null}>
          {/* ── Camera path driven by scroll progress ── */}
          <CameraRig progressRef={progressRef} />

          {/* ── 3D Airbus model ── */}
          <AirbusModel progressRef={progressRef} />

          {/* ── Three-point lighting + environment ── */}
          <Lighting />

          {/* ── Atmospheric particles ── */}
          <CloudsParticles progressRef={progressRef} />
        </Suspense>

        {/* ── Post-processing (conditional) ── */}
        {enablePostProcessing && (
          <EffectComposer>
            <Bloom
              luminanceThreshold={0.9}
              luminanceSmoothing={0.5}
              intensity={0.3}
            />
          </EffectComposer>
        )}
      </Canvas>
    </div>
  )
}
