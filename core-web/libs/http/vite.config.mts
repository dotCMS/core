// `vitest/config`, not `vite`: in Vitest 4 the triple-slash reference no longer
// augments Vite's `UserConfig`, so a `test` block against `vite`'s defineConfig does not
// type-check.
import { defineConfig } from 'vitest/config';

export default defineConfig({
    root: __dirname,
    cacheDir: '../../node_modules/.vite/libs/http',
    test: {
        name: 'http',
        watch: false,
        globals: true,
        // A plain Node library: no DOM, and `fetch` is the platform's own.
        environment: 'node',
        include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts}'],
        reporters: ['default'],
        coverage: {
            reportsDirectory: '../../coverage/libs/http',
            provider: 'v8' as const
        }
    }
});
