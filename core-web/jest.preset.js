const nxPreset = require('@nx/jest/preset').default;

// Coverage and verbose output are opt-in: pass `--coverage` / `--verbose` on the
// CLI, or rely on the `ci` configuration in nx.json which sets `codeCoverage: true`.
// Enabling them globally instruments every transform and slows the suite ~2x.
const isCi = process.env.CI === 'true' || process.env.CI === '1';

module.exports = {
    ...nxPreset,
    coverageDirectory: '../../../target/core-web-reports/',
    collectCoverageFrom: [
        'src/**/*.ts',
        'src/**/*.tsx',
        '!src/**/*.stories.ts',
        '!src/**/*.module.ts',
        '!src/index.ts'
    ],
    coverageReporters: ['html', 'lcov', 'text'],
    reporters: [
        'default',
        ...(isCi ? [['github-actions', { silent: false }]] : []),
        [
            'jest-junit',
            {
                outputDirectory: '../../../target/core-web-reports',
                outputName: 'TEST-results.xml'
            }
        ]
    ],
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
