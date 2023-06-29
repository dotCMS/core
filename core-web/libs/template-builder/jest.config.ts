/* eslint-disable */
export default {
    displayName: 'template-builder',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {
        'ts-jest': {
            tsconfig: '<rootDir>/tsconfig.spec.json',
            stringifyContentPathRegex: '\\.(html|svg)$'
        }
    },
    coverageDirectory: '../../target/core-web-reports/',
    collectCoverage: true,
    coverageReporters: [['lcovonly', { file: 'TEST-template-builder.lcov' }]],
    collectCoverageFrom: [
        'src/**/*.ts',
        '!src/**/*.stories.ts',
        '!src/**/*.module.ts',
        '!src/index.ts'
    ],
    reporters: [
        [
            'jest-junit',
            { outputDirectory: 'target/core-web-reports', outputName: 'TEST-template-builder.xml' }
        ]
    ],
    transform: {
        '^.+\\.(ts|mjs|js|html)$': 'jest-preset-angular'
    },
    // https://github.com/nrwl/nx/issues/7844#issuecomment-1016624608
    transformIgnorePatterns: ['<rootDir>/node_modules/(?!.*\\.mjs$)'],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
