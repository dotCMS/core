const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    main: 'libs/sdk/uve/src/index.ts',
    additionalEntryPoints: ['./src/internal.ts'],
    generateExportsField: true,
    outputPath: '../../../dist/libs/sdk/uve',
    tsConfig: './tsconfig.lib.json',
    project: './package.json',
    compiler: 'babel',
    format: ['esm', 'cjs'],
    extractCss: false,
    assets: [
        {
            input: 'libs/sdk/uve',
            output: '.',
            glob: '*.md'
        }
    ]
};

const config = withNx(options, {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
});

module.exports = config;
