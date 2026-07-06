export default {
    displayName: 'sdk-ai',
    preset: '../../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html', 'json'],
    testMatch: ['**/*.spec.ts', '**/*.test.ts'],
    coverageDirectory: '../../../coverage/libs/sdk/ai',
    // The generated spec is data, not code — keep it out of coverage instrumentation.
    collectCoverageFrom: ['src/**/*.ts', '!src/generated/**', '!src/**/*.spec.ts']
};
