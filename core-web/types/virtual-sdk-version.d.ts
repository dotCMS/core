/**
 * `virtual:sdk-version` is not a real file — it's generated at build time by the
 * `sdkVersionPlugin` rollup plugin in `libs/sdk/client/rollup.config.cjs`, which reads that
 * package's own `package.json` version (already set to the exact dotCMS release version by the
 * deploy-javascript-sdk release pipeline before the build runs).
 *
 * Unit tests never go through rollup, so `libs/sdk/client/jest.config.ts` maps this module id to
 * a real stub file (`src/lib/utils/__mocks__/virtual-sdk-version.ts`) instead.
 *
 * It lives here rather than beside the importer because an ambient declaration only covers the
 * project that includes it: `sdk-vue` and the other SDK consumers pull `@dotcms/client` sources
 * in through a path mapping, which does not bring that project's sibling `.d.ts` along.
 */
declare module 'virtual:sdk-version' {
    export const SDK_VERSION: string;
}
