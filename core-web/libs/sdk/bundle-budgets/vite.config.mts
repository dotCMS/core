/// <reference types='vitest' />
import { defineConfig } from 'vite';

/**
 * Hand-written rather than produced by tools/generate-vite-configs.mjs: that generator emits
 * the jsdom + vmForks shape every other SDK project needs, and none of it applies here. This
 * suite runs esbuild in a Node environment against the built packages in dist/, touches no
 * DOM, and needs a longer timeout than a unit test because each probe is a real bundle.
 */
export default defineConfig(() => ({
    root: __dirname,
    cacheDir: '../../../node_modules/.vite/libs/sdk/bundle-budgets',
    test: {
        name: 'sdk-bundle-budgets',
        watch: false,
        globals: true,
        environment: 'node',
        include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts}'],
        // Each probe shells out to esbuild and bundles the real packages.
        testTimeout: 60_000,
        hookTimeout: 120_000,
        reporters: process.env.GITHUB_ACTIONS
            ? [
                  'default',
                  'github-actions',
                  [
                      'junit',
                      { outputFile: '../../../target/core-web-reports/sdk-bundle-budgets.xml' }
                  ]
              ]
            : [
                  'default',
                  [
                      'junit',
                      { outputFile: '../../../target/core-web-reports/sdk-bundle-budgets.xml' }
                  ]
              ]
    }
}));
