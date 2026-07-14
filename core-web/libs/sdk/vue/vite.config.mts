/// <reference types='vitest' />
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import dts from 'vite-plugin-dts';
import * as path from 'path';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig(() => ({
    root: import.meta.dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/vue',
    plugins: [
        vue(),
        // `root` points to the core-web workspace root so tsconfig path aliases
        // (e.g. @dotcms/types) resolve in bundled sibling sources like @dotcms/uve,
        // which are compiled from source into this build/test run.
        // `projects` pins resolution to the base tsconfig (which holds every
        // @dotcms/* alias) so the plugin does NOT crawl every tsconfig in the
        // monorepo. That crawl runs inside @nx/vite's project-graph inference
        // (resolveConfig) and segfaults the native resolver on CI.
        tsconfigPaths({
            root: path.resolve(import.meta.dirname, '../../../'),
            projects: ['tsconfig.base.json']
        }),
        viteStaticCopy({ targets: [{ src: '*.md', dest: '.' }] }),
        dts({
            entryRoot: 'src',
            tsconfigPath: path.join(import.meta.dirname, 'tsconfig.lib.json'),
            pathsToAliases: false
        })
    ],
    // Uncomment this if you are using workers.
    // worker: {
    //   plugins: () => [ tsconfigPaths() ],
    // },
    // Configuration for building your library.
    // See: https://vite.dev/guide/build.html#library-mode
    build: {
        outDir: '../../../dist/libs/sdk/vue',
        emptyOutDir: true,
        reportCompressedSize: true,
        commonjsOptions: {
            transformMixedEsModules: true
        },
        lib: {
            // Could also be a dictionary or array of multiple entry points.
            entry: 'src/index.ts',
            name: 'sdk-vue',
            fileName: 'index',
            // Change this to the formats you want to support.
            // Don't forget to update your package.json as well.
            formats: ['es' as const]
        },
        // `vue` MUST be external (a peer dependency). If it is bundled, the SDK
        // ships its own copy of Vue's reactivity, which is a *different* instance
        // from the host app's — so refs created inside the SDK never trigger the
        // app's watchers/computeds (live UVE updates silently do nothing).
        rollupOptions: {
            external: [
                'vue',
                '@tinymce/tinymce-vue',
                '@dotcms/client',
                '@dotcms/uve',
                '@dotcms/uve/internal',
                '@dotcms/types',
                '@dotcms/types/internal'
            ],
            output: {
                globals: { vue: 'Vue' }
            }
        }
    },
    test: {
        name: 'sdk-vue',
        watch: false,
        globals: true,
        environment: 'jsdom',
        include: ['{src,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
        setupFiles: ['./src/test-setup.ts'],
        reporters: ['default'],
        coverage: {
            reportsDirectory: '../../../coverage/libs/sdk/vue',
            provider: 'v8' as const
        }
    }
}));
