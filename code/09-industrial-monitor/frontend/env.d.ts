/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly BASE_URL: string
  // 可添加其他自定义 env 变量，如 VITE_API_URL
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
