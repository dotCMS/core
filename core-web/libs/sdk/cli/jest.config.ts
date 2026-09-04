export default {
    displayName: 'sdk-cli',
    preset: '../../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    moduleFileExtensions: ['ts', 'js', 'html'],
    // chalk 5 and ora are ESM-only. Jest skips node_modules by default, so they arrive
    // untransformed and fail to parse. create-app solves it the same way.
    transformIgnorePatterns: [],
    coverageDirectory: '../../../coverage/libs/sdk/cli',
    moduleNameMapper: {
        // This workspace maps aliases per-project; jest.preset.js does not read
        // tsconfig.base.json paths. Without this, `@dotcms/http` type-checks but does not
        // resolve at test time, and every suite touching it fails on module resolution
        // rather than on the behaviour under test.
        '^@dotcms/http$': '<rootDir>/../../http/src/index.ts'
    }
};
