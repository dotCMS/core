/* eslint-disable */
export default {
    displayName: 'agentic-tools',
    preset: '../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html', 'json'],
    testMatch: ['**/*.spec.ts', '**/*.test.ts'],
    coverageDirectory: '../../coverage/libs/agentic-tools'
};
