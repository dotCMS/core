/* eslint-disable */
export default {
    displayName: 'data-access',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {
        'ts-jest': {
            isolatedModules: true, // Prevent type checking in tests and deps
            tsconfig: '<rootDir>/tsconfig.spec.json',
            stringifyContentPathRegex: '\\.(html|svg)$'
        }
    },
    coverageDirectory: '../../coverage/libs/data-access',
    transform: {
        '^.+\\.(ts|mjs|js|html)$': 'jest-preset-angular'
    },
    transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
