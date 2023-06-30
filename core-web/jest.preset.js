const nxPreset = require('@nrwl/jest/preset').default;

module.exports = {
    ...nxPreset,

    collectCoverage: true,
    collectCoverageFrom: [
        'src/**/*.ts',
        '!src/**/*.stories.ts',
        '!src/**/*.module.ts',
        '!src/index.ts'
    ]
};
