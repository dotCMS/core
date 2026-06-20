export default {
    displayName: 'portlets-dot-accessibility-studio-portlet',
    preset: '../../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    coverageDirectory: '../../../coverage/libs/portlets/dot-accessibility-studio',
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                tsconfig: '<rootDir>/tsconfig.spec.json',
                stringifyContentPathRegex: '\\.(html|svg)$'
            }
        ]
    },
    // The studio imports the page scanner from @dotcms/portlets/dot-ema/ui, whose
    // barrel transitively pulls ESM-only deps (lib0/y-protocols/@tiptap/etc.) that
    // must be transformed. Mirrors dot-content-drive/portlet's jest config.
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|.*(y-protocols|lib0|y-prosemirror|@tiptap|marked|lowlight|devlop))'
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ]
};
