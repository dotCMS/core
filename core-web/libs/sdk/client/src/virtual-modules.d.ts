/**
 * `virtual:sdk-version` is not a real file — it's generated at build time by the
 * `sdkVersionPlugin` rollup plugin in `rollup.config.cjs`, which reads this package's own
 * `package.json` version (already set to the exact dotCMS release version by the
 * deploy-javascript-sdk release pipeline before the build runs).
 *
 * Unit tests never go through rollup, so `jest.config.ts` maps this module id to a real
 * stub file (`src/lib/utils/__mocks__/virtual-sdk-version.ts`) instead.
 */
declare module 'virtual:sdk-version' {
    export const SDK_VERSION: string;
}
