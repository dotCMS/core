// Jest stub for the `virtual:sdk-version` module that rollup generates at build time
// (see `sdkVersionPlugin` in rollup.config.cjs). Mapped through resolve.alias in
// vite.config.mts, since the test run never executes the rollup build.
export const SDK_VERSION = '0.0.0-test';
