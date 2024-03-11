import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

import * as path from 'path';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/experiments',

    plugins: [
        nxViteTsPaths(),
        dts({
            entryRoot: 'src',
            tsConfigFilePath: path.join(__dirname, 'tsconfig.lib.json'),
            skipDiagnostics: true
        }),
        dts({
            entryRoot: 'src',
            tsConfigFilePath: path.join(__dirname, 'tsconfig.lib.json'),
            skipDiagnostics: true
        })
    ],

    // Configuration for building your library.
    // See: https://vitejs.dev/guide/build.html#library-mode
    build: {
        outDir: '../../../dist/libs/sdk/experiments',
        reportCompressedSize: true,
        emptyOutDir: true,
        commonjsOptions: {
            transformMixedEsModules: true
        },
        minify: 'terser',
        lib: {
            // Could also be a dictionary or array of multiple entry points.
            entry: 'src/lib/standalone.ts',
            name: 'DotExperiment',
            fileName: 'dot-experiments.min',
            formats: ['iife']
        },
        rollupOptions: {
            // External packages that should not be bundled into your library.
            external: []
        }
    }
});
