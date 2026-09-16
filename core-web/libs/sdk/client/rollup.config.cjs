const fs = require('fs');
const path = require('path');

const { withNx } = require('@nx/rollup/with-nx');

const { patchExportsPlugin } = require('../rollup-shared/patch-exports.cjs');

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

// Injects this package's own version (already set to the exact dotCMS release version
// by the deploy-javascript-sdk release pipeline before this build runs — see
// .github/actions/core-cicd/deployment/deploy-javascript-sdk/action.yml) as a build-time
// constant, so @dotcms/client can compare itself against the X-DotCMS-Version /
// X-DotCMS-Min-SDK response headers at runtime (see lib/utils/sdk-compatibility.ts).
//
// Implemented as a plain Rollup virtual-module plugin (no @rollup/plugin-replace
// dependency needed) — withNx() concatenates any `plugins` passed in its second
// argument onto its own generated plugin list, so this doesn't replace Nx's plugins.
function sdkVersionPlugin() {
    const virtualModuleId = 'virtual:sdk-version';
    const resolvedVirtualModuleId = '\0' + virtualModuleId;

    return {
        name: 'sdk-version',
        resolveId(id) {
            if (id === virtualModuleId) {
                return resolvedVirtualModuleId;
            }

            return null;
        },
        load(id) {
            if (id !== resolvedVirtualModuleId) {
                return null;
            }

            const pkg = JSON.parse(
                fs.readFileSync(path.resolve(__dirname, 'package.json'), 'utf-8')
            );

            return `export const SDK_VERSION = ${JSON.stringify(pkg.version)};`;
        }
    };
}

const config = withNx(options, {
    // Provide additional rollup configuration here. See: https://rollupjs.org/configuration-options
    // e.g.
    // output: { sourcemap: true },
    plugins: [sdkVersionPlugin()]
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
