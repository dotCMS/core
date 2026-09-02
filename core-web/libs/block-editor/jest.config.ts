/* eslint-disable */
export default {
    displayName: 'block-editor',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ],
    transform: {
        '^.+.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                stringifyContentPathRegex: '\\.(html|svg)$',

                tsconfig: '<rootDir>/tsconfig.spec.json'
            }
        ]
    },
    // These deps ship ESM-only entry points that Jest cannot parse untransformed.
    // `y-protocols` is reached via @tiptap/extension-drag-handle → extension-collaboration
    // and made this whole suite fail to run. Mirrors the new-block-editor config.
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|.*(y-protocols|lib0|y-prosemirror|@tiptap|marked|lowlight|devlop))'
    ]
};
