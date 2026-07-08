const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    main: 'libs/sdk/react/src/index.ts',
    generateExportsField: true,
    outputPath: '../../../dist/libs/sdk/react',
    tsConfig: './tsconfig.lib.json',
    project: './package.json',
    external: ['react/jsx-runtime'],
    compiler: 'babel',
    format: ['esm'],
    extractCss: false,
    assets: [
        {
            glob: 'libs/sdk/react/README.md',
            input: '.',
            output: '.'
        }
    ]
};

let config = withNx(options, {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
});

config = require('./rollup.migrated.config.js')(config, options);

module.exports = config;
