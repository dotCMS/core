const { withNx } = require('@nx/rollup/with-nx');

const { typesFirstPlugin } = require('../../../tools/rollup/types-first.cjs');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    main: 'libs/sdk/types/src/index.ts',
    additionalEntryPoints: ['./src/internal.ts'],
    generateExportsField: true,
    outputPath: '../../../dist/libs/sdk/types',
    tsConfig: './tsconfig.lib.json',
    project: './package.json',
    compiler: 'babel',
    format: ['esm', 'cjs'],
    extractCss: false,
    assets: [
        {
            input: 'libs/sdk/types',
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

// Nx writes `module` before `types`, and conditions are order-sensitive, so TypeScript never
// reaches the declarations. Reorders keys only — every target is left as Nx set it.
const outputDir = Array.isArray(config.output) ? config.output[0]?.dir : config.output?.dir;

config.plugins = [
    ...(Array.isArray(config.plugins) ? config.plugins : []),
    typesFirstPlugin(outputDir)
];

module.exports = config;
