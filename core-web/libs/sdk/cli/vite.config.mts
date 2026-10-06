import * as path from 'node:path';

// `vitest/config`, not `vite`: in Vitest 4 the triple-slash reference no longer augments
// Vite's `UserConfig`, so a `test` block against `vite`'s defineConfig does not type-check.
import { defineConfig } from 'vitest/config';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/cli',
    resolve: {
        alias: {
            // This workspace maps aliases per-project. A plain alias rather than
            // `vite-tsconfig-paths`: that plugin crawls every tsconfig in the monorepo during
            // @nx/vite's graph inference, which has segfaulted the native resolver on CI
            // (see the note in libs/sdk/analytics/vite.config.mts). One entry is all we need.
            '@dotcms/http': path.resolve(__dirname, '../../http/src/index.ts')
        }
    },
    test: {
        name: 'sdk-cli',
        watch: false,
        globals: true,
        // A Node CLI: no DOM. Note there is no `transformIgnorePatterns` equivalent to set —
        // chalk 5 and ora are ESM-only, which Jest needed configuring around and Vite serves
        // natively.
        environment: 'node',
        include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts}'],
        setupFiles: ['./src/test-setup.ts'],
        reporters: ['default'],
        coverage: {
            reportsDirectory: '../../../coverage/libs/sdk/cli',
            provider: 'v8' as const
        }
    }
});
