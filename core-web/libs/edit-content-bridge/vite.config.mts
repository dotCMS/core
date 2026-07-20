import { defineConfig } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';

import { resolve } from 'path';

export default defineConfig(() => {
    // Explicitly resolve outDir relative to this config file's location
    // This ensures output always goes to core-web/dist/libs/edit-content-bridge
    // regardless of current working directory or presence of external dist folders
    // import.meta.dirname points to core-web/libs/edit-content-bridge
    // So ../../dist/libs/edit-content-bridge resolves to core-web/dist/libs/edit-content-bridge
    const outDir = resolve(import.meta.dirname, '../../dist/libs/edit-content-bridge');

    return {
        root: import.meta.dirname,
        // `root` points to the core-web workspace root so tsconfig path aliases
        // (e.g. @dotcms/dotcms-models, @dotcms/ui) resolve consistently with the
        // other SDK vite configs, even though today's bundle graph doesn't need it.
        // `projects` pins resolution to the base tsconfig (which holds every
        // @dotcms/* alias) so the plugin does NOT crawl every tsconfig in the
        // monorepo. That crawl runs inside @nx/vite's project-graph inference
        // (resolveConfig) and segfaults the native resolver on CI.
        plugins: [
            tsconfigPaths({
                root: resolve(import.meta.dirname, '../../'),
                projects: ['tsconfig.base.json']
            })
        ],
        build: {
            // Explicitly set outDir to prevent Vite from resolving paths incorrectly
            // This is critical for reproducible builds, especially when dist folders
            // exist outside the repository root (e.g., at repo root level)
            outDir: outDir,
            lib: {
                entry: resolve(import.meta.dirname, 'src/iife.ts'),
                name: 'DotCustomFieldApi',
                formats: ['iife'],
                fileName: () => 'edit-content-bridge.js'
            },
            minify: true,
            rollupOptions: {
                // Externalize Angular and UI dependencies since they're not needed in the Dojo IIFE build
                // The IIFE only uses DojoFormBridge, not AngularFormBridge
                external: ['@angular/core', '@angular/forms', 'primeng/dynamicdialog', '@dotcms/ui']
            }
        }
    };
});
