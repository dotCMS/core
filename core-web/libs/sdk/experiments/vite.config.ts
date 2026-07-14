import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';
import tsconfigPaths from 'vite-tsconfig-paths';

import * as path from 'path';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/experiments',

    plugins: [
        // `root` points to the core-web workspace root so tsconfig path aliases
        // (e.g. @dotcms/types) resolve in bundled sibling sources like @dotcms/uve,
        // which are compiled from source into this build.
        tsconfigPaths({ root: path.resolve(__dirname, '../../../') }),
        dts({
            entryRoot: 'src',
            tsConfigFilePath: path.join(__dirname, 'tsconfig.lib.json'),
            skipDiagnostics: true
        })
    ],

    // Configuration for building the DotExperiment as IIFE file to use in
    // plain HTML or other projects.
    build: {
        // Explicitly resolve outDir to prevent output from going to external dist folders
        // This ensures reproducible builds regardless of current working directory
        outDir: path.resolve(__dirname, '../../../dist/libs/sdk/experiments'),
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
