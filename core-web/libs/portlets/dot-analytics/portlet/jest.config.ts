export default {
    displayName: 'portlets-dot-analytics',
    preset: '../../../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    /* d3 ships ESM in .js entrypoints; internmap etc. nest under node_modules — allow Jest to transform.
       The `.*` before each package matches pnpm's nested `.pnpm/<pkg>@<ver>/node_modules/<pkg>` layout. */
    transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$|.*(d3(/|-)|internmap/))'],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ],
    testEnvironment: '@happy-dom/jest-environment'
};
