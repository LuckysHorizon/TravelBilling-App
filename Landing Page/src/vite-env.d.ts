/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_TRAVELBILLING_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.glb' {
  const src: string
  export default src
}

declare module '*.gltf' {
  const src: string
  export default src
}
