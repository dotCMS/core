/// <reference types='vitest' />
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import react from '@vitejs/plugin-react';
import * as path from 'path';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/sdk-analytics',

    plugins: [
        react(),
        nxViteTsPaths(),
        dts({ entryRoot: 'src', tsconfigPath: path.join(__dirname, 'tsconfig.lib.json') })
    ],

    // Uncomment this if you are using workers.
    // worker: {
    //  plugins: [ nxViteTsPaths() ],
    // },

    // Configuration for building your library.
    // See: https://vitejs.dev/guide/build.html#library-mode
    build: {
        outDir: '../../../dist/libs/sdk/sdk-analytics',
        emptyOutDir: true,
        reportCompressedSize: true,
        commonjsOptions: {
            transformMixedEsModules: true
        },
        lib: {
            entry: {
                index: 'src/index.ts',
                'react/index': 'src/lib/react/index.ts'
            },
            formats: ['es']
        },
        rollupOptions: {
            external: ['react', 'react-dom', 'react/jsx-runtime'],
            output: {
                exports: 'named',
                preserveModules: true,
                preserveModulesRoot: 'src'
            }
        }
    }
});
