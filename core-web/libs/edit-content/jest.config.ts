/* eslint-disable */
export default {
    displayName: 'edit-content',
    preset: '../../jest.preset.js',
    setupFilesAfterEnv: ['<rootDir>/src/test-setup.ts'],
    globals: {},
    transform: {
        '^.+\\.(ts|mjs|js|html)$': [
            'jest-preset-angular',
            {
                stringifyContentPathRegex: '\\.(html|svg)$',
                tsconfig: '<rootDir>/tsconfig.spec.json'
            }
        ]
    },
    transformIgnorePatterns: [
        'node_modules/(?!.*\\.mjs$|.*(y-protocols|lib0|y-prosemirror|@tiptap|marked))'
    ],
    snapshotSerializers: [
        'jest-preset-angular/build/serializers/no-ng-attributes',
        'jest-preset-angular/build/serializers/ng-snapshot',
        'jest-preset-angular/build/serializers/html-comment'
    ],
    coveragePathIgnorePatterns: [
        'node_modules/',
        'src/lib/custom-languages/.*-monaco-language.*\\.ts$'
    ]
};
