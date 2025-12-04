/// <reference types='vitest' />
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import * as path from 'path';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

// Plugin simple para copiar README.md
const copyReadme = {
    name: 'copy-readme',
    writeBundle() {
        fs.copyFileSync(
            path.resolve(__dirname, 'README.md'),
            path.resolve(__dirname, '../../../dist/libs/sdk/analytics/README.md')
        );
    }
};

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/analytics',

    plugins: [
        react(),
        nxViteTsPaths(),
        dts({ entryRoot: 'src', tsconfigPath: path.join(__dirname, 'tsconfig.lib.json') }),
        copyReadme,
        // Ensure the React entry is tagged as a Client Component in the emitted bundle
        {
            name: 'preserve-use-client-react-entry',
            generateBundle(_options, bundle) {
                for (const [fileName, chunk] of Object.entries(bundle)) {
                    if (chunk.type !== 'chunk') continue;
                    const isReactEntry =
                        (chunk as any).facadeModuleId?.endsWith('src/lib/react/index.ts') ||
                        fileName === 'react/index.js';
                    if (isReactEntry && !chunk.code.startsWith('"use client"')) {
                        chunk.code = '"use client";\n' + chunk.code;
                    }
                }
            }
        }
    ],

    build: {
        // Explicitly resolve outDir to prevent output from going to external dist folders
        // This ensures reproducible builds regardless of current working directory
        outDir: path.resolve(__dirname, '../../../dist/libs/sdk/analytics'),
        emptyOutDir: true,
        reportCompressedSize: true,
        commonjsOptions: {
            transformMixedEsModules: true,
            requireReturnsDefault: 'auto'
        },
        lib: {
            entry: {
                index: 'src/index.ts',
                'react/index': 'src/lib/react/index.ts'
            },
            formats: ['es']
        },
        rollupOptions: {
            external: [
                'react',
                'react-dom',
                'react/jsx-runtime',
                'analytics',
                '@analytics/core',
                '@analytics/storage-utils',
                '@analytics/queue-utils',
                '@analytics/router-utils',
                /^next\//
            ],
            output: {
                exports: 'named',
                preserveModules: true,
                preserveModulesRoot: 'src',
                entryFileNames: '[name].js',
                chunkFileNames: 'chunks/[name].js'
            }
        }
    }
});
