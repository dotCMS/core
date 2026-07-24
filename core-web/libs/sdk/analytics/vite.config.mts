/// <reference types='vitest' />
import react from '@vitejs/plugin-react';
import * as path from 'path';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import tsconfigPaths from 'vite-tsconfig-paths';

const distDir = path.resolve(__dirname, '../../../dist/libs/sdk/analytics');

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/analytics',

    plugins: [
        react(),
        // `root` points to the core-web workspace root so tsconfig path aliases
        // (e.g. @dotcms/types) resolve in bundled sibling sources like @dotcms/uve,
        // which are compiled from source into this build.
        // `projects` pins resolution to the base tsconfig (which holds every
        // @dotcms/* alias) so the plugin does NOT crawl every tsconfig in the
        // monorepo. That crawl runs inside @nx/vite's project-graph inference
        // (resolveConfig) and segfaults the native resolver on CI.
        tsconfigPaths({
            root: path.resolve(__dirname, '../../../'),
            projects: ['tsconfig.base.json']
        }),
        viteStaticCopy({
            targets: [
                { src: '*.md', dest: '.' },
                { src: 'package.json', dest: '.' }
            ]
        }),
        dts({ entryRoot: 'src', tsconfigPath: path.join(__dirname, 'tsconfig.lib.json') }),
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
        outDir: distDir,
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
