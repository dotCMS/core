const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    format: ['esm', 'cjs'],
    compiler: 'tsc',
    generateExportsField: true,
    additionalEntryPoints: [
        './src/sandbox/index.ts',
        './src/adapter/index.ts',
        './src/spec/index.ts'
    ],
    outputPath: '../../../dist/libs/sdk/ai',
    assets: [
        {
            input: 'libs/sdk/ai',
            output: '.',
            glob: '*.md'
        }
    ],
    main: './src/runtime.ts',
    tsConfig: './tsconfig.lib.json'
};

const config = withNx(options, {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
});

module.exports = config;
