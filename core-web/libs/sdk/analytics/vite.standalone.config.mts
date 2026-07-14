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
    // `projects` pins resolution to the base tsconfig (which holds every
    // @dotcms/* alias) so the plugin does NOT crawl every tsconfig in the
    // monorepo. That crawl runs inside @nx/vite's project-graph inference
    // (resolveConfig) and segfaults the native resolver on CI.
    plugins: [
        tsconfigPaths({
            root: resolve(__dirname, '../../../'),
            projects: ['tsconfig.base.json']
        })
    ],
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
