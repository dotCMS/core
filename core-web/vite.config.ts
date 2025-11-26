import { defineConfig } from 'vite';

export default defineConfig({
    cacheDir: '.vite-cache',
    optimizeDeps: {
        disabled: true
    },
    build: {
        write: true,
        sourcemap: false
    }
});
