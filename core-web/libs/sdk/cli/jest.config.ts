export default {
    displayName: 'sdk-cli',
    preset: '../../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../../coverage/libs/sdk/cli',
    moduleNameMapper: {
        // This workspace maps aliases per-project; jest.preset.js does not read
        // tsconfig.base.json paths. Without this, `@dotcms/http` type-checks but does not
        // resolve at test time, and every suite touching it fails on module resolution
        // rather than on the behaviour under test.
        '^@dotcms/http$': '<rootDir>/../../http/src/index.ts'
    }
};
