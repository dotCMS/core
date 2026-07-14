/// <reference types='vitest' />
import { defineConfig } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';

import { resolve } from 'path';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/analytics',
    // `root` points to the core-web workspace root so tsconfig path aliases
    // (e.g. @dotcms/types) resolve in bundled sibling sources like @dotcms/uve,
    // which are compiled from source into this build.
    plugins: [tsconfigPaths({ root: resolve(__dirname, '../../../') })],
    build: {
        // Explicitly resolve outDir to prevent output from going to external dist folders
        // This ensures reproducible builds regardless of current working directory
        outDir: resolve(__dirname, '../../../dist/libs/sdk/analytics'),
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
