import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: 'dist', // 构建输出目录
    rollupOptions: {
      output: {
        manualChunks: {
          // 将 three.js 单独打包，避免主包过大
          three: ['three']
        }
      }
    }
  },
  server: {
    port: 5173,
    open: true // 启动时自动打开浏览器
  }
})
