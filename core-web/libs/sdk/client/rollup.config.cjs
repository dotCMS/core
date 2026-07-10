const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    format: ['esm', 'cjs'],
    compiler: 'tsc',
    additionalEntryPoints: ['./src/internal.ts'],
    generateExportsField: true,
    outputPath: '../../../dist/libs/sdk/client',
    assets: [
        {
            input: 'libs/sdk/client',
            output: '.',
            glob: '*.md'
        }
    ],
    main: './src/index.ts',
    tsConfig: './tsconfig.lib.json'
};

const config = withNx(options, {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
});

module.exports = config;
