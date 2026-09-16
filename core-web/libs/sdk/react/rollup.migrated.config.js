const preserveDirectives = require('rollup-plugin-preserve-directives').default;
const postcss = require('rollup-plugin-postcss');
const path = require('path');

// A build-time require of workspace tooling, not a runtime dependency between projects. The
// sibling SDK rollup configs require the same file; they simply never trip this rule because
// their lint target covers *.ts alone.
// eslint-disable-next-line @nx/enforce-module-boundaries -- build tooling, not a project import
const { patchExportsPlugin } = require('../../../tools/rollup/patch-exports.cjs');

/**
 * The CSS in this package is injected by JavaScript (`extractCss: false`), so each stylesheet
 * is emitted as its own side-effectful module — e.g. `Row.module.css.esm.js`. A blanket
 * `"sideEffects": false` would let bundlers drop those imports as dead code and silently ship
 * the layout without its grid styles, so the flag is an allow-list instead of `false`.
 */
const SIDE_EFFECTFUL_FILES = ['**/*.css', '**/*.css.esm.js', '**/*.css.cjs.js'];

/**
 * Server-safe entry, excluding TinyMCE and other client-only modules. Declared ahead of
 * `import` so React Server Components resolve to it.
 */
const REACT_SERVER_ENTRY = { 'react-server': './index.server.esm.js' };

/**
 * Swap @nx/rollup's inlined postcss plugin for `rollup-plugin-postcss`.
 *
 * @nx/rollup 23 replaced the `createFilter` from `@rollup/pluginutils` with a hand-rolled
 * picomatch matcher, and `picomatch('**\/*.css')` returns false for the absolute module ids
 * rollup actually passes in. The result is that no CSS is ever transformed: rollup receives
 * the raw stylesheet, tries to parse it as JavaScript, and the build dies on the first line
 * of Column.module.css with "Expression expected". Without this, `nx build sdk-react` cannot
 * complete at all.
 *
 * `rollup-plugin-postcss` is the plugin Nx inlined in the first place and is already a
 * dependency of this workspace. Options mirror what withNx passes: styles are injected by
 * JS (extract: false) and `.module.css` files are treated as CSS modules.
 */
function replaceBrokenPostcssPlugin(plugins, options) {
    const index = plugins.findIndex((plugin) => plugin && plugin.name === 'postcss');

    if (index === -1) {
        console.warn(
            '[rollup.config.js] No postcss plugin found in the Nx plugin list — CSS handling ' +
                'was left untouched. If the build fails parsing a .css file, this is why.'
        );

        return plugins;
    }

    const patched = [...plugins];
    patched[index] = postcss({
        inject: injectStyleInline,
        extract: options.extractCss,
        autoModules: true
    });

    return patched;
}

/**
 * Emit the style-injection code inline instead of importing `style-inject`.
 *
 * With `preserveModules`, the default `inject: true` leaves each stylesheet chunk importing
 * `style-inject` from the workspace's node_modules. Rollup then copies that dependency to
 * `dist/libs/sdk/react/node_modules/.pnpm/...` and rewrites the import to point there — which
 * works locally but not once published, because npm always strips `node_modules` from the
 * tarball. Consumers would install a package whose CSS chunks import a file that does not
 * exist.
 *
 * Injecting inline keeps the stylesheet chunks self-contained. The guard makes it a no-op
 * during SSR (no `document`), and idempotent if two copies of the package end up on one page
 * — which the dual-package hazard makes possible now that `import` resolves to real ESM.
 *
 * The guard compares stylesheet *content*, not a derived key. An earlier version keyed on
 * `css.length`, which silently skipped injection for any second stylesheet that happened to
 * be the same byte length as one already on the page.
 *
 * @param {string} cssVariableName identifier holding the stylesheet text
 * @returns {string} code appended to the stylesheet module
 */
function injectStyleInline(cssVariableName) {
    return `
(function () {
    if (typeof document === 'undefined') return;
    var existing = document.head.querySelectorAll('style[data-dotcms-style]');
    for (var i = 0; i < existing.length; i++) {
        if (existing[i].textContent === ${cssVariableName}) return;
    }
    var style = document.createElement('style');
    style.setAttribute('data-dotcms-style', '');
    style.appendChild(document.createTextNode(${cssVariableName}));
    document.head.appendChild(style);
})();
`;
}

module.exports = (options, nxOptions) => {
    if (!options) return {};

    // Add server entry point alongside the default entry
    if (options.input && typeof options.input === 'object' && options.input.index) {
        const mainEntry = options.input.index;
        const parsed = path.parse(mainEntry);
        const serverFileName = `${parsed.name}.server${parsed.ext}`;
        const serverEntry = parsed.dir ? path.join(parsed.dir, serverFileName) : serverFileName;
        options.input['index.server'] = serverEntry;
    } else {
        console.warn(
            '[rollup.config.js] Could not find options.input.index — server entry (index.server.ts) was NOT registered. ' +
                'The react-server export condition will point to a missing file.'
        );
    }

    // Enable preserveModules on all outputs
    if (Array.isArray(options.output)) {
        options.output.forEach((output) => {
            output.preserveModules = true;
            output.preserveModulesRoot = 'src';
        });
    } else if (options.output) {
        options.output.preserveModules = true;
        options.output.preserveModulesRoot = 'src';
    }

    // Determine output directory for package.json patching
    const outputDir = Array.isArray(options.output) ? options.output[0]?.dir : options.output?.dir;

    // Append preserveDirectives and package.json patcher as the last plugins
    options.plugins = [
        ...replaceBrokenPostcssPlugin(
            Array.isArray(options.plugins) ? options.plugins : [],
            nxOptions ?? {}
        ),
        preserveDirectives(),
        patchExportsPlugin({
            outputDir,
            sideEffects: SIDE_EFFECTFUL_FILES,
            extraConditions: REACT_SERVER_ENTRY
        })
    ];

    // Suppress MODULE_LEVEL_DIRECTIVE warnings from rollup, composing with any existing handler
    const originalOnWarn = options.onwarn;
    options.onwarn = (warning, warn) => {
        if (warning.code === 'MODULE_LEVEL_DIRECTIVE') return;
        if (typeof originalOnWarn === 'function') return originalOnWarn(warning, warn);
        warn(warning);
    };

    return options;
};
