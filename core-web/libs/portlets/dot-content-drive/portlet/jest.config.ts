/* eslint-disable */
export default {
    displayName: 'portlet',
    preset: '../../../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    coverageDirectory: '../../../../coverage/libs/portlets/dot-content-drive/portlet',
    testEnvironment: '@happy-dom/jest-environment',
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    // The content-type selector dialog reuses the UVE palette from @dotcms/portlets/dot-ema/ui,
    // whose barrel transitively pulls ESM-only deps (lib0/@tiptap/etc.) that must be transformed.
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|.*(y-protocols|lib0|y-prosemirror|@tiptap|marked|lowlight|devlop))'
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
