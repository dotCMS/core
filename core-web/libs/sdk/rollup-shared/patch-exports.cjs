const fs = require('fs');
const path = require('path');

/**
 * `@nx/rollup`'s `generateExportsField` writes the conditions for a dual ESM/CJS build in
 * this order:
 *
 *     { "module": "./x.esm.js", "types": "./x.d.ts",
 *       "import": "./x.cjs.mjs", "default": "./x.cjs.js" }
 *
 * `x.cjs.mjs` is an ESM-to-CJS interop bridge: it imports the CommonJS build and re-exports
 * its named bindings. Nx picks it to dodge the dual-package hazard, but because every modern
 * bundler resolves `import` before `module`, real ESM consumers land on the bridge and then
 * on a single non-analysable CommonJS file. Nothing tree-shakes.
 *
 * This rewrites the map so `import` points at the ESM artifact that already exists, and adds
 * an explicit `require` condition so CommonJS consumers keep working unchanged:
 *
 *     { "types": "./x.d.ts", "module": "./x.esm.js",
 *       "import": "./x.esm.js", "require": "./x.cjs.js", "default": "./x.cjs.js" }
 *
 * `types` moves first because TypeScript and bundlers resolve conditions in declaration
 * order and will take an earlier `import`/`require` before ever seeing it.
 *
 * Trade-off: pointing `import` at real ESM reintroduces the dual-package hazard the bridge
 * was avoiding — an app that both imports and requires the same package gets two copies.
 * That is safe for these packages, which hold no module-level mutable state (the client is a
 * factory, UVE reads its state off `window`, types emit no runtime code). Do not apply this
 * blindly to a package that keeps a singleton.
 *
 * @param {object} config
 * @param {string} config.outputDir directory holding the generated package.json
 * @param {boolean|string[]} [config.sideEffects] value to write as the `sideEffects` field
 * @returns {import('rollup').Plugin}
 */
function patchExportsPlugin({ outputDir, sideEffects }) {
    return {
        name: 'patch-sdk-exports',
        writeBundle() {
            if (!outputDir) {
                console.warn(
                    '[patch-sdk-exports] Could not resolve the rollup output directory — ' +
                        'exports map was NOT patched. ESM consumers will resolve to the CJS bridge.'
                );

                return;
            }

            const pkgPath = path.join(outputDir, 'package.json');

            if (!fs.existsSync(pkgPath)) {
                console.warn(
                    `[patch-sdk-exports] No package.json at ${pkgPath} — exports map was NOT patched. ` +
                        'ESM consumers will resolve to the CJS interop bridge.'
                );

                return;
            }

            const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));

            if (sideEffects !== undefined) {
                pkg.sideEffects = sideEffects;
            }

            for (const [subpath, conditions] of Object.entries(pkg.exports ?? {})) {
                if (!conditions || typeof conditions !== 'object' || Array.isArray(conditions)) {
                    continue;
                }

                const esm = conditions.module;
                const cjs = conditions.default;

                // An ESM-only build has no `module`/CJS pair to reorder; leave it as generated.
                if (!esm) {
                    continue;
                }

                // Conditions resolve in declaration order, so anything this rewrite does not
                // know about (e.g. `react-server`) has to stay ahead of `import`/`default`
                // or it becomes unreachable.
                const passthrough = Object.fromEntries(
                    Object.entries(conditions).filter(
                        ([key]) =>
                            !['types', 'module', 'import', 'require', 'default'].includes(key)
                    )
                );

                pkg.exports[subpath] = {
                    ...(conditions.types ? { types: conditions.types } : {}),
                    ...passthrough,
                    module: esm,
                    import: esm,
                    ...(cjs && cjs.endsWith('.cjs.js') ? { require: cjs, default: cjs } : {})
                };
            }

            fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
        }
    };
}

module.exports = { patchExportsPlugin };
