const nxPreset = require('@nx/jest/preset').default;

module.exports = {
    ...nxPreset,
    coverageDirectory: '../../../target/core-web-reports/',
    collectCoverage: true,
    collectCoverageFrom: [
        'src/**/*.ts',
        'src/**/*.tsx',
        '!src/**/*.stories.ts',
        '!src/**/*.module.ts',
        '!src/index.ts'
    ],
    verbose: true,
    // Prevent worker pool exhaustion and individual test timeouts
    testTimeout: 30000, // 30 seconds per test (default is 5s)
    maxWorkers: '50%', // Limit worker processes to prevent exhaustion
    /* TODO: Update to latest Jest snapshotFormat
     * By default Nx has kept the older style of Jest Snapshot formats
     * to prevent breaking of any existing tests with snapshots.
     * It's recommend you update to the latest format.
     * You can do this by removing snapshotFormat property
     * and running tests with --update-snapshot flag.
     * Example: "nx affected --targets=test --update-snapshot"
     * More info: https://jestjs.io/docs/upgrading-to-jest29#snapshot-format
     */
    snapshotFormat: { escapeString: true, printBasicPrototype: true }
};
