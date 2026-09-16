const preserveDirectives = require('rollup-plugin-preserve-directives').default;
const postcss = require('rollup-plugin-postcss');
const path = require('path');
const fs = require('fs');

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
 * during SSR (no `document`) and idempotent if the same chunk is evaluated twice.
 *
 * @param {string} cssVariableName identifier holding the stylesheet text
 * @returns {string} code appended to the stylesheet module
 */
function injectStyleInline(cssVariableName) {
    return `
(function () {
    if (typeof document === 'undefined') return;
    var key = '__dotcms_react_style_' + ${cssVariableName}.length;
    if (document.head.querySelector('style[data-dotcms-style="' + key + '"]')) return;
    var style = document.createElement('style');
    style.setAttribute('data-dotcms-style', key);
    style.appendChild(document.createTextNode(${cssVariableName}));
    document.head.appendChild(style);
})();
`;
}

/**
 * The CSS in this package is injected by JavaScript (`extractCss: false`), so each stylesheet
 * is emitted as its own side-effectful module — e.g. `Row.module.css.esm.js`. A blanket
 * `"sideEffects": false` would let bundlers drop those imports as dead code and silently ship
 * the layout without its grid styles, so the flag is an allow-list instead of `false`.
 */
const SIDE_EFFECTFUL_FILES = ['**/*.css', '**/*.css.esm.js', '**/*.css.cjs.js'];

/**
 * Rollup plugin that patches the output package.json:
 *
 * - adds the `react-server` condition, pointing at the server-safe entry that excludes
 *   TinyMCE and other client-only modules;
 * - moves `types` to the front, because conditions resolve in declaration order and an
 *   earlier `import` shadows it;
 * - declares which files carry real side effects, so everything else can be tree-shaken.
 */
function patchPackageJsonPlugin(outputDir) {
    return {
        name: 'patch-package-json',
        writeBundle() {
            const pkgPath = path.join(outputDir, 'package.json');
            if (!fs.existsSync(pkgPath)) return;
            const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));
            const mainExport = pkg.exports && pkg.exports['.'];

            pkg.sideEffects = SIDE_EFFECTFUL_FILES;

            if (mainExport && typeof mainExport === 'object' && !Array.isArray(mainExport)) {
                const { types, ...rest } = mainExport;
                pkg.exports['.'] = {
                    ...(types ? { types } : {}),
                    'react-server': './index.server.esm.js',
                    ...rest
                };
                fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
            } else if (typeof mainExport === 'string') {
                pkg.exports['.'] = {
                    'react-server': './index.server.esm.js',
                    import: mainExport
                };
                fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
            } else {
                console.warn(
                    '[patchPackageJsonPlugin] Unrecognized exports["."] shape — react-server condition was NOT added. ' +
                        'SSR builds may pull in TinyMCE. Check the generated package.json exports field.'
                );
            }
        }
    };
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
        ...(outputDir ? [patchPackageJsonPlugin(outputDir)] : [])
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
