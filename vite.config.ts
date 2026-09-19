import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { existsSync } from 'node:fs'
import { resolve } from 'node:path'

export default defineConfig({
  plugins: [react()],
  optimizeDeps: {
    esbuildOptions: {
      plugins: [{
        name: 'resolve-local-dependencies',
        setup(build) {
          build.onResolve({ filter: /^\.\.?\// }, (args) => {
            const path = resolve(args.resolveDir, args.path)
            return existsSync(path) ? { path } : undefined
          })
        },
      }],
    },
  },
})
