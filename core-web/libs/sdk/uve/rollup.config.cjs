const { withNx } = require('@nx/rollup/with-nx');

const { patchExportsPlugin } = require('../rollup-shared/patch-exports.cjs');

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

// Rewrite the generated exports map so standard ESM `import` resolves to the real ESM
// artifact instead of Nx's `*.cjs.mjs` interop bridge (which is backed by a single
// non-analysable CommonJS file and defeats tree-shaking). `require` keeps pointing at the
// CommonJS build, so existing CJS consumers are unaffected.
// Read the directory off the resolved rollup output rather than `options.outputPath`:
// withNx() rewrites that field in place, so it no longer points where the bundle lands.
const outputDir = Array.isArray(config.output) ? config.output[0]?.dir : config.output?.dir;

config.plugins = [
    ...(Array.isArray(config.plugins) ? config.plugins : []),
    patchExportsPlugin({ outputDir, sideEffects: false })
];

module.exports = config;
