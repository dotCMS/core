/// <reference types='vitest' />
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';

import { resolve } from 'path';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/analytics',
    plugins: [nxViteTsPaths()],
    build: {
        outDir: '../../../dist/libs/sdk/analytics',
        emptyOutDir: false,
        reportCompressedSize: true,
        lib: {
            entry: resolve(__dirname, 'src/lib/standalone.ts'),
            name: 'dotAnalytics',
            fileName: 'ca.min',
            formats: ['iife']
        },
        rollupOptions: {
            external: ['react', 'react-dom', 'react/jsx-runtime', /^@dotcms\/analytics\/react/],
            output: {
                entryFileNames: 'ca.min.js',
                globals: {}
            }
        }
    }
});
