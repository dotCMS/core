export default {
    displayName: 'dotcms-webcomponents',
    preset: '@stencil/core/testing',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {
        'ts-jest': {
            stringifyContentPathRegex: '\\.(html|svg)$',

            tsconfig: '<rootDir>/tsconfig.spec.json'
        }
    },
    coverageDirectory: '../../coverage/libs/dotcms-webcomponents',

    transformIgnorePatterns: ['node_modules/(?!.*.mjs$)']
};
