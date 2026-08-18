export default {
    displayName: 'sdk-client',
    preset: '../../../jest.preset.js',
    transform: {
        '^.+\\.[tj]s$': [
            'ts-jest',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json'
            }
        ]
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../../coverage/libs/sdk/client',
    moduleNameMapper: {
        // 'virtual:sdk-version' only exists as a rollup-generated virtual module
        // (see sdkVersionPlugin in rollup.config.cjs) — ts-jest never runs the rollup
        // build, so point it at a real stub file instead.
        '^virtual:sdk-version$': '<rootDir>/src/lib/utils/__mocks__/virtual-sdk-version.ts'
    }
};
