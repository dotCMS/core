/* eslint-disable */
export default {
    displayName: 'block-editor',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {
        'ts-jest': {
            stringifyContentPathRegex: '\\.(html|svg)$',

            tsconfig: '<rootDir>/tsconfig.spec.json'
        }
    },
    coverageDirectory: '../../target/core-web-reports/',
    coverageReporters: [['lcovonly', { file: 'TEST-block-editor.lcov' }]],
    reporters: [
        [
            'jest-junit',
            { outputDirectory: 'target/core-web-reports', outputName: 'TEST-block-editor.xml' }
        ]
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ],
    transform: {
        '^.+.(ts|mjs|js|html)$': 'jest-preset-angular'
    },
    transformIgnorePatterns: ['node_modules/(?!.*.mjs$)']
};
