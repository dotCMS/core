/* eslint-disable */
export default {
    displayName: 'mcp-server',
    preset: '../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html', 'json'],
    testMatch: ['**/*.spec.ts', '**/*.test.ts'],
    coverageDirectory: '../../coverage/apps/mcp-server'
};
