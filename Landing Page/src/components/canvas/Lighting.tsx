import { Environment, ContactShadows } from '@react-three/drei'

/**
 * Lighting
 * --------
 * Premium three-point directional lighting rig for a bright daytime aesthetic.
 *
 * - Key light  → warm white, main illumination with shadows
 * - Fill light → cool sky-blue, softens shadow side
 * - Rim light  → warm gold, highlights edges and adds depth
 * - Ambient    → low-intensity base fill
 * - Environment preset provides realistic sky reflections on metallic surfaces
 * - ContactShadows ground the model without a visible ground plane
 */
export default function Lighting() {
  return (
    <>
      {/* ── Sky / Environment reflections ── */}
      <Environment preset="sunset" />

      {/* ── Key light (warm white) ── */}
      <directionalLight
        position={[10, 10, 5]}
        intensity={2}
        color="#ffffff"
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
        shadow-camera-far={50}
        shadow-camera-left={-10}
        shadow-camera-right={10}
        shadow-camera-top={10}
        shadow-camera-bottom={-10}
        shadow-bias={-0.0001}
      />

      {/* ── Fill light (cool sky-blue) ── */}
      <directionalLight
        position={[-5, 3, -5]}
        intensity={0.8}
        color="#a0c4ff"
      />

      {/* ── Rim / back light (warm gold) ── */}
      <directionalLight
        position={[0, 5, -10]}
        intensity={1.2}
        color="#ffe4a0"
      />

      {/* ── Ambient fill ── */}
      <ambientLight intensity={0.4} />

      {/* ── Contact shadows beneath the model ── */}
      <ContactShadows
        position={[0, -1.5, 0]}
        opacity={0.3}
        scale={20}
        blur={2.5}
        far={10}
      />
    </>
  )
}
