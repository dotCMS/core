import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';

import { resolve } from 'path';

// These options were migrated by @nx/vite:convert-to-inferred from the project.json file.
const configValues = { default: { buildLibsFromSource: true } };

// Determine the correct configValue to use based on the configuration
const nxConfiguration = process.env.NX_TASK_TARGET_CONFIGURATION ?? 'default';

const options = {
    ...configValues.default,
    ...(configValues[nxConfiguration] ?? {})
};

export default defineConfig(() => {
    // Explicitly resolve outDir relative to this config file's location
    // This ensures output always goes to core-web/dist/libs/edit-content-bridge
    // regardless of current working directory or presence of external dist folders
    // __dirname points to core-web/libs/edit-content-bridge
    // So ../../dist/libs/edit-content-bridge resolves to core-web/dist/libs/edit-content-bridge
    const outDir = resolve(__dirname, '../../dist/libs/edit-content-bridge');

    return {
        root: __dirname,
        plugins: [nxViteTsPaths({ buildLibsFromSource: options.buildLibsFromSource })],
        build: {
            // Explicitly set outDir to prevent Vite from resolving paths incorrectly
            // This is critical for reproducible builds, especially when dist folders
            // exist outside the repository root (e.g., at repo root level)
            outDir: outDir,
            lib: {
                entry: resolve(__dirname, 'src/iife.ts'),
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
