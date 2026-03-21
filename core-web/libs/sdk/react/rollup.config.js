const preserveDirectives = require('rollup-plugin-preserve-directives').default;
const path = require('path');
const fs = require('fs');

/**
 * Rollup plugin that patches the output package.json to add the react-server
 * export condition, pointing to the server-safe entry that excludes TinyMCE
 * and other client-only modules.
 */
function patchPackageJsonPlugin(outputDir) {
    return {
        name: 'patch-package-json',
        writeBundle() {
            const pkgPath = path.join(outputDir, 'package.json');
            if (!fs.existsSync(pkgPath)) return;
            const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));
            const mainExport = pkg.exports && pkg.exports['.'];

            if (mainExport && typeof mainExport === 'object' && !Array.isArray(mainExport)) {
                pkg.exports['.'] = {
                    'react-server': './index.server.esm.js',
                    ...mainExport
                };
                fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
            } else if (typeof mainExport === 'string') {
                pkg.exports['.'] = {
                    'react-server': './index.server.esm.js',
                    import: mainExport
                };
                fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
            }
        }
    };
}

module.exports = (options) => {
    if (!options) return {};

    // Add server entry point alongside the default entry
    if (options.input && typeof options.input === 'object' && options.input.index) {
        const mainEntry = options.input.index;
        const parsed = path.parse(mainEntry);
        const serverFileName = `${parsed.name}.server${parsed.ext}`;
        const serverEntry = parsed.dir ? path.join(parsed.dir, serverFileName) : serverFileName;
        options.input['index.server'] = serverEntry;
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
        ...(Array.isArray(options.plugins) ? options.plugins : []),
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
