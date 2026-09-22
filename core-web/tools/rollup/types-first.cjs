const fs = require('fs');
const path = require('path');

/**
 * Move `types` to the front of every conditional export.
 *
 * Export conditions are matched in declaration order, so a condition listed before `types`
 * can shadow it and leave TypeScript resolving to JavaScript instead of declarations.
 * `@nx/rollup` writes `module` first for a dual ESM/CJS build and offers no option to change
 * that, so the order is fixed up after the fact. publint reports the unfixed order as an
 * error ("types should be the first in the object").
 *
 * This only reorders keys. Every condition still points exactly where Nx pointed it —
 * `module` at the ESM build for bundlers, `import` at the CJS interop bridge so Node keeps a
 * single instance of the package. That arrangement is deliberate upstream and is left alone.
 *
 * @param {string | undefined} outputDir directory holding the generated package.json
 * @returns {import('rollup').Plugin}
 */
function typesFirstPlugin(outputDir) {
    return {
        name: 'exports-types-first',
        writeBundle() {
            const pkgPath = outputDir && path.join(outputDir, 'package.json');

            if (!pkgPath || !fs.existsSync(pkgPath)) {
                console.warn(
                    '[exports-types-first] No generated package.json found — export conditions ' +
                        'were left in Nx order, where `types` is shadowed by `module`.'
                );

                return;
            }

            const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf-8'));

            for (const [subpath, conditions] of Object.entries(pkg.exports ?? {})) {
                if (!conditions || typeof conditions !== 'object' || !conditions.types) {
                    continue;
                }

                const { types, ...rest } = conditions;
                pkg.exports[subpath] = { types, ...rest };
            }

            fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
        }
    };
}

module.exports = { typesFirstPlugin };
