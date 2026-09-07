export default {
    displayName: 'portlets-dot-query-tool-portlet',
    preset: '../../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    coverageDirectory: '../../../coverage/libs/portlets/dot-query-tool',
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    // The Edit Content side panel (@dotcms/edit-content) transitively pulls ESM-only deps
    // (lib0/@tiptap/etc.) that must be transformed rather than ignored.
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|.*(y-protocols|lib0|y-prosemirror|@tiptap|marked|lowlight|devlop))'
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
