const nxPreset = require('@nrwl/jest/preset').default;

module.exports = {
    ...nxPreset,
    coverageDirectory: '../../../target/core-web-reports/',
    collectCoverage: true,
    collectCoverageFrom: [
        'src/**/*.ts',
        '!src/**/*.stories.ts',
        '!src/**/*.module.ts',
        '!src/index.ts'
    ],
    verbose: true
};
