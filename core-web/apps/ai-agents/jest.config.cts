module.exports = {
    displayName: 'ai-agents',
    preset: '../../jest.preset.js',
    testEnvironment: 'node',
    transform: {
        '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }]
    },
    // css-attribution.ts pulls in pure-ESM deps (css-select, htmlparser2 and
    // their transitive packages). Jest ignores node_modules by default, so let
    // these specific packages through the ts-jest transform. The pnpm store
    // nests transitive deps, so match the package name anywhere in the path.
    transformIgnorePatterns: [
        'node_modules/(?!(?:.+/)?(?:css-select|css-what|boolbase|nth-check|domhandler|domutils|domelementtype|dom-serializer|htmlparser2|entities)/)'
    ],
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../coverage/apps/ai-agents'
};
