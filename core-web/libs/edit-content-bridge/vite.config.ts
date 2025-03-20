import { defineConfig } from 'vite';

import { resolve } from 'path';

export default defineConfig({
    build: {
        lib: {
            entry: resolve(__dirname, 'src/iife.ts'),
            name: 'DotCustomFieldApi',
            formats: ['iife'],
            fileName: () => 'edit-content-bridge.js'
        },
        minify: true
    }
});
