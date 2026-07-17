import { fileURLToPath, URL } from 'node:url';

import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue(), tailwindcss()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    optimizeDeps: {
        // Don't pre-bundle the dotCMS SDKs. During local development the SDK is
        // often re-linked from a fresh local build; pre-bundling would make Vite
        // serve a stale cached copy from node_modules/.vite. Excluding them means
        // Vite always reads the current linked files.
        exclude: ['@dotcms/vue', '@dotcms/client', '@dotcms/uve', '@dotcms/types']
    }
});
