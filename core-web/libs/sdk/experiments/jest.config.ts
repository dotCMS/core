/* eslint-disable */
export default {
    displayName: 'sdk-experiments',
    preset: '../../../jest.preset.js',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../../coverage/libs/sdk/experiments',
    testEnvironment: 'jsdom'
};
