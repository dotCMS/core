const { withNx } = require('@nx/rollup/with-nx');

// These options were migrated by @nx/rollup:convert-to-inferred from project.json
const options = {
    outputPath: '../../../dist/libs/sdk/experiments',
    tsConfig: './tsconfig.lib.json',
    project: './package.json',
    main: 'libs/sdk/experiments/src/index.ts',
    external: [
        'react/jsx-runtime',
        '@dotcms/client',
        '@dotcms/react',
        '@dotcms/uve',
        '@dotcms/types'
    ],
    compiler: 'babel',
    assets: [
        {
            glob: 'libs/sdk/experiments/README.md',
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

config = require('@nx/react/plugins/bundle-rollup')(config, options);

module.exports = config;
