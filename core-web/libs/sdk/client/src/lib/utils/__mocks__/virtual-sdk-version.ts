// Jest stub for the `virtual:sdk-version` module that rollup generates at build time
// (see `sdkVersionPlugin` in rollup.config.cjs). Mapped in jest.config.ts's
// moduleNameMapper since ts-jest never runs the rollup build.
export const SDK_VERSION = '0.0.0-test';
