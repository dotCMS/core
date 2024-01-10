/* eslint-disable */
export default {
    displayName: 'DotExperiments Data Access',
    preset: '../../../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    coverageDirectory: '../../../../../target/core-web-reports/',
    coverageReporters: [['lcovonly', { file: 'TEST-data-access-experiment.lcov' }]],
    reporters: [
        'default',
        ['github-actions', { silent: false }],
        [
            'jest-junit',
            {
                outputDirectory: '../target/core-web-reports',
                outputName: 'TEST-data-access-experiment.xml'
            }
        ]
    ],
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
