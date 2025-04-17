/* eslint-disable */
export default {
    displayName: 'template-builder',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    coverageReporters: [['lcovonly', { file: 'TEST-template-builder.lcov' }]],
    reporters: [
        'default',
        ['github-actions', { silent: false }],
        [
            'jest-junit',
            {
                outputDirectory: '../target/core-web-reports',
                outputName: 'TEST-template-builder.xml'
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
    // https://github.com/nrwl/nx/issues/7844#issuecomment-1016624608
    // This is a bottleneck, it's taking way too long to start the tests
    // https://github.com/dotCMS/core/issues/31729
    transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$|gridstack)'],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
