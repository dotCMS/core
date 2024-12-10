import { defineConfig } from 'vite';

import { resolve } from 'path';

export default defineConfig({
    build: {
        lib: {
            entry: resolve(__dirname, 'src/lib/standalone.ts'),
            name: 'dotAnalytics',
            fileName: () => `ca.min.js`,
            formats: ['iife']
        }
    }
});
