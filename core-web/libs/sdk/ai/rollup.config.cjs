const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    format: ['esm', 'cjs'],
    compiler: 'tsc',
    generateExportsField: true,
    // One entry per subpath, each with a UNIQUE basename: the output bundle is named after
    // the entry file, so two entries called `index.ts` silently collapse into one.
    additionalEntryPoints: [
        './src/sandbox.ts',
        './src/adapter.ts',
        './src/spec.ts',
        './src/tools.ts'
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
