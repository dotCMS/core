/// <reference types='vitest' />
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vite';

import { resolve } from 'path';

import { stubScss } from '../../../tools/vitest-stub-scss.mts';

/**
 * Test config, written to match what tools/generate-vite-configs.mjs emits for a
 * framework-free SDK (compare libs/sdk/client/vite.config.mts). The generator only handles
 * projects migrated from Jest, so it skips this one.
 *
 * One deviation from client's config: pool 'forks'. These specs came from sdk-analytics,
 * whose SSR cases set `global.window = undefined`; on the 'vmForks' pool `window` is a
 * getter-only property and the assignment throws. The generator keeps sdk-analytics on
 * 'forks' for the same measured reason (POOL_FORKS).
 */
export default defineConfig(() => ({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/events',
    plugins: [
        stubScss(),
        tsconfigPaths({ root: resolve(__dirname, '../../..'), projects: ['tsconfig.base.json'] })
    ],
    test: {
        name: 'sdk-events',
        watch: false,
        globals: true,
        css: { include: [], modules: { classNameStrategy: 'non-scoped' } },
        isolate: true,
        pool: 'forks',
        environment: 'jsdom',
        environmentOptions: { jsdom: { url: 'http://localhost/' } },
        include: ['{src,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
        server: {
            deps: {
                inline: [/[\\/](libs|apps)[\\/]/]
            }
        },
        reporters: process.env['GITHUB_ACTIONS']
            ? [
                  'default',
                  'github-actions',
                  ['junit', { outputFile: '../../../target/core-web-reports/sdk-events.xml' }]
              ]
            : [
                  'default',
                  ['junit', { outputFile: '../../../target/core-web-reports/sdk-events.xml' }]
              ],
        coverage: {
            reportsDirectory: '../../../coverage/libs/sdk/events',
            reporter: ['html', 'lcov', 'text'],
            provider: 'v8' as const
        }
    }
}));
