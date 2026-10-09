/// <reference types='vitest' />
import { defineConfig } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';

import { resolve } from 'path';

import type { Plugin } from 'vite';

/** Fails the build if anything the script imports reaches React: traditional pages have none. */
const noReact = (): Plugin => ({
    name: 'dot-events-no-react',
    enforce: 'pre',
    resolveId(source, importer) {
        if (/^react(-dom)?(\/|$)/.test(source)) {
            this.error(
                `ca.min.js must not import React: ${source}, from ${importer ?? 'the entry'}`
            );
        }

        return null;
    }
});

/**
 * The IIFE dotCMS injects into traditional pages: src/standalone.ts with everything it imports,
 * Analytics.js included, in one file. Named ca.min.js because dotCMS serves and injects
 * /ext/analytics/ca.min.js (core-web/pom.xml copies the standalone folder there).
 */
export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/events-standalone',
    // Resolve @dotcms/uve from the workspace sources, pinned to the base tsconfig so the plugin
    // does not crawl every tsconfig in the monorepo (see @dotcms/analytics's standalone config)
    plugins: [
        noReact(),
        tsconfigPaths({
            root: resolve(__dirname, '../../../'),
            projects: ['tsconfig.base.json']
        })
    ],
    build: {
        outDir: resolve(__dirname, '../../../dist/libs/sdk/events-standalone'),
        emptyOutDir: true,
        reportCompressedSize: true,
        lib: {
            entry: resolve(__dirname, 'src/standalone.ts'),
            // Required by Vite for an IIFE; the entry exports nothing, so no global is created
            name: 'dotEventsStandalone',
            fileName: 'ca.min',
            formats: ['iife']
        },
        rollupOptions: {
            output: {
                entryFileNames: 'ca.min.js',
                inlineDynamicImports: true
            }
        }
    }
});
