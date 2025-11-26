/* eslint-disable */
export default {
    displayName: 'dotcms-ui',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    coverageDirectory: '../../coverage/apps/dotcms-ui',
    testEnvironmentOptions: {
        customExportConditions: [''],
        resources: 'usable',
        pretendToBeVisual: true,
        features: {
            FetchExternalResources: false,
            ProcessExternalResources: false
        }
    },
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|y-protocols|lib0|@tiptap|y-prosemirror|gridstack|uuid)'
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
