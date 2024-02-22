/* eslint-disable */
export default {
    displayName: 'test',
    preset: '../../../jest.preset.js',
    testEnvironment: 'jsdom',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../../coverage/libs/sdk/test'
};
