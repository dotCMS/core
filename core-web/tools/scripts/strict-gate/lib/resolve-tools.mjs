/**
 * Resolves the compiler toolchain from the workspace, never from a version pinned here.
 *
 * The harness deliberately declares no dependency of its own: whatever TypeScript and Angular
 * compiler the workspace is on is what the gate must measure against. Pinning a version inside
 * the harness would let it drift from the code it checks, which is the one way its numbers could
 * be quietly wrong.
 */
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

/** `core-web/` — the workspace root the harness resolves everything relative to. */
export const workspaceRoot = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    '../../../..'
);

const requireFromWorkspace = createRequire(path.join(workspaceRoot, 'package.json'));

/**
 * @param {string} specifier
 * @returns {{ name: string, version: string, path: string }}
 */
function describe(specifier) {
    const pkgPath = requireFromWorkspace.resolve(`${specifier}/package.json`);
    const pkg = requireFromWorkspace(`${specifier}/package.json`);
    return { name: specifier, version: pkg.version, path: path.dirname(pkgPath) };
}

/** Loads the workspace TypeScript. Throws a directed message when dependencies are missing. */
export async function loadTypeScript() {
    try {
        return (await import(requireFromWorkspace.resolve('typescript'))).default;
    } catch (cause) {
        throw new Error(
            `Cannot resolve 'typescript' from ${workspaceRoot}. Run 'pnpm install' in core-web/.`,
            { cause }
        );
    }
}

/** Loads the workspace Angular compiler. Only needed by template-aware mode. */
export async function loadAngularCompiler() {
    try {
        return await import(requireFromWorkspace.resolve('@angular/compiler-cli'));
    } catch (cause) {
        throw new Error(
            `Cannot resolve '@angular/compiler-cli' from ${workspaceRoot}. Run 'pnpm install' in core-web/.`,
            { cause }
        );
    }
}

/**
 * Resolves an executable a package declares in its `bin` field.
 *
 * Never assume a conventional path: nx declares `./dist/bin/nx.js`, not `bin/nx.js`, and under
 * pnpm the package lives inside `.pnpm/<hash>/`. Guessing breaks on either.
 *
 * @param {string} specifier  Package name, e.g. 'nx'.
 * @param {string} [binName]  Bin entry; defaults to the package name.
 */
export function resolveBin(specifier, binName = specifier) {
    const pkgPath = requireFromWorkspace.resolve(`${specifier}/package.json`);
    const { bin } = requireFromWorkspace(`${specifier}/package.json`);
    const entry = typeof bin === 'string' ? bin : bin?.[binName];
    if (!entry) throw new Error(`package '${specifier}' declares no bin '${binName}'`);
    return path.resolve(path.dirname(pkgPath), entry);
}

/**
 * Reports what the harness is actually running against. Recorded in the report so a measurement
 * can always be traced back to the toolchain that produced it.
 */
export function toolchainInfo() {
    return {
        node: process.version,
        typescript: describe('typescript').version,
        angularCompiler: describe('@angular/compiler-cli').version
    };
}
