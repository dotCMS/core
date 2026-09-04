export default {
    displayName: 'sdk-create-app',
    preset: '../../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    /*
     * This package is `"type": "module"` and every one of its runtime dependencies is
     * ESM-only (inquirer, ora, chalk, execa, axios — plus ~50 transitive micro-packages
     * such as string-width, restore-cursor, npm-run-path...). Jest runs the specs as
     * CommonJS, so anything left untransformed inside node_modules blows up with
     * "SyntaxError: Cannot use import statement outside a module" the moment a spec
     * imports this CLI's own source.
     *
     * Other libs in this workspace solve the same problem with a named allow-list
     * (see libs/edit-content, apps/dotcms-ui, libs/portlets/dot-agents:
     * `node_modules/(?!...y-protocols|lib0|@tiptap...)`). Here the list would need to
     * enumerate ~51 packages and would silently rot on every dependency bump, so the
     * whole of node_modules is transformed instead. The dependency tree of this CLI is
     * small: a cold, uncached run of the full suite costs ~3s.
     */
    transformIgnorePatterns: [],
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../../coverage/libs/sdk/create-app',
    moduleNameMapper: {
        // This workspace maps aliases per-project; jest.preset.js does not read
        // tsconfig.base.json paths. Without this, `@dotcms/http` resolves at compile time
        // but not under Jest.
        '^@dotcms/http$': '<rootDir>/../../http/src/index.ts'
    }
};
