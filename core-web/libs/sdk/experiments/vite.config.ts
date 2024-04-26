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
        })
    ],

    // Configuration for building the DotExperiment as IIFE file to use in
    // plain HTML or other projects.
    build: {
        outDir: '../../../dist/libs/sdk/experiments',
        reportCompressedSize: true,
        emptyOutDir: true,
        commonjsOptions: {
            transformMixedEsModules: true
        },
        minify: 'terser',
        lib: {
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
