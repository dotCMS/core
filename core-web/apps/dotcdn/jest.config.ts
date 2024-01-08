/* eslint-disable */
export default {
    displayName: 'dotcdn',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    coverageReporters: [['lcovonly', { file: 'TEST-dotcdn.lcov' }]],
    reporters: [
        'default',
        ['github-actions', { silent: false }],
        [
            'jest-junit',
            {
                outputDirectory: '../target/core-web-reports',
                outputName: 'TEST-dotcdn.xml'
            }
        ]
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ],
    transform: {
        '^.+.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                stringifyContentPathRegex: '\\.(html|svg)$',
                tsconfig: '<rootDir>/tsconfig.spec.json'
            }
        ]
    },
    transformIgnorePatterns: ['node_modules/(?!.*.mjs$)']
};
