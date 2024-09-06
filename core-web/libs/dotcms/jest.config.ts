/* eslint-disable */
export default {
    displayName: 'dotcms',
    preset: '../../jest.preset.js',
    globals: {},
    transform: {
        '^.+\\.[tj]s$': [
            'ts-jest',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json'
            }
        ]
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../target/core-web-reports/',
    coverageReporters: [['lcovonly', { file: 'TEST-dotcms.lcov' }]],
    reporters: [
        [
            'jest-junit',
            { outputDirectory: 'target/core-web-reports', outputName: 'TEST-dotcms.xml' }
        ]
    ]
};
