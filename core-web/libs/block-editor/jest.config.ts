/* eslint-disable */
export default {
    displayName: 'block-editor',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    coverageReporters: [['lcovonly', { file: 'TEST-block-editor.lcov' }]],
    reporters: [
        'default',
        ['github-actions', { silent: false }],
        [
            'jest-junit',
            { outputDirectory: '../target/core-web-reports', outputName: 'TEST-block-editor.xml' }
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
