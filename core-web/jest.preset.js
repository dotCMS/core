const nxPreset = require('@nx/jest/preset').default;

module.exports = {
    ...nxPreset,
    /*
     * Recycle a Jest worker once its heap passes this limit, instead of letting it grow
     * until V8 aborts the process (#37245: `ui:test` died at 3706 MB against V8's 4144 MB
     * default ceiling, with `current mu = 0.067` — 93% of the time spent in GC).
     *
     * Why 1536MB, measured with `--logHeapUsage` on `libs/ui` (105 specs):
     *   - Per-spec footprint in a fresh worker: median 169 MB, mean 193 MB.
     *   - One outlier: dot-folder-list-view.component.spec.ts at 1564 MB (206 tests in a
     *     single file), 4x the next-heaviest spec (365 MB).
     *   - Accumulated peak with no limit: 1995 MB (2 workers) / 2384 MB (apps/dotcms-ui).
     * The worst case after a recycle is `limit + largest single-spec footprint`, i.e.
     * 1536 + 1564 = ~3.1 GB, leaving ~1 GB under the 4144 MB ceiling. Raising this to
     * 2048 MB would put the worst case at ~3.6 GB — right where CI already aborted.
     * At a 169 MB median it also lets a worker run ~9 specs before recycling, so the
     * respawn cost stays negligible.
     *
     * Setting this also forces Jest to use worker processes rather than running in-band
     * (see shouldRunInBand in @jest/core): on a runner where `maxWorkers <= 1` Jest would
     * otherwise run every spec in the main process, where nothing can recycle it.
     */
    workerIdleMemoryLimit: '1536MB',
    coverageDirectory: '../../../target/core-web-reports/',
    collectCoverage: true,
    collectCoverageFrom: ['src/**/*.ts', 'src/**/*.tsx', '!src/**/*.module.ts', '!src/index.ts'],
    coverageReporters: ['html', 'lcov', 'text'],
    reporters: [
        'default',
        ['github-actions', { silent: false }],
        [
            'jest-junit',
            {
                outputDirectory: '../../../target/core-web-reports',
                outputName: 'TEST-results.xml'
            }
        ]
    ],
    verbose: true,
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
