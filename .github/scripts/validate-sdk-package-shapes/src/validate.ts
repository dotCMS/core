/**
 * Guards the package.json contract described in
 * specs/37399-sdk-packaging-version-fix/contracts/package-json-shape.md.
 */

const FLOATING_SPECIFIERS = new Set(['latest', 'next', '*']);

/** react/angular/vue/analytics must not carry these as regular `dependencies` — see Defect B1b. */
const SIBLING_DEPENDENCY_GUARD_PACKAGES = new Set(['react', 'angular', 'vue', 'analytics']);
const GUARDED_SIBLING_DEPS = ['@dotcms/client', '@dotcms/uve'];

type PackageJson = {
    name?: string;
    dependencies?: Record<string, string>;
    peerDependencies?: Record<string, string>;
    devDependencies?: Record<string, string>;
};

const isDotcmsPackage = (specifier: string): boolean => specifier.startsWith('@dotcms/');

/**
 * Validates a core-web/libs/sdk/<pkgName>/package.json.
 *
 * - Any `@dotcms/*` entry in `peerDependencies` must not be a floating specifier
 *   (`latest`/`next`/`*`) — the published value is always rewritten to an exact version at
 *   publish time, but the source placeholder must be real semver (the `"0.0.0"` sentinel) so
 *   local peer-dependency-satisfaction checks behave sanely before publish.
 * - `@dotcms/client`/`@dotcms/uve` must never be regular `dependencies` of react/angular/vue/
 *   analytics — that is the actual mechanism behind the yarn/pnpm duplicate-copy bug (Defect
 *   B1b), independent of whatever value they hold.
 * - `dependencies`/`devDependencies` values are intentionally NOT checked: they are masked by
 *   the release pipeline's rewrite step before publish, so a floating value there is not a
 *   customer-facing defect.
 */
export function validateSdkLibPackageJson(pkg: PackageJson, pkgName: string): string[] {
    const violations: string[] = [];

    for (const [dep, version] of Object.entries(pkg.peerDependencies ?? {})) {
        if (isDotcmsPackage(dep) && FLOATING_SPECIFIERS.has(version)) {
            violations.push(
                `${pkgName}: peerDependencies["${dep}"] is "${version}" — must be real semver (the "0.0.0" sentinel), never latest/next/*`
            );
        }
    }

    if (SIBLING_DEPENDENCY_GUARD_PACKAGES.has(pkgName)) {
        for (const dep of GUARDED_SIBLING_DEPS) {
            if (pkg.dependencies && dep in pkg.dependencies) {
                violations.push(
                    `${pkgName}: dependencies["${dep}"] must not exist — it belongs in peerDependencies (Defect B1b regression guard)`
                );
            }
        }
    }

    return violations;
}

/**
 * Validates an examples/<app>/package.json for the given branch.
 *
 * - `next`/`*` are never valid, on any branch.
 * - `latest` is valid ONLY on `main` (the deliberate Evergreen-alignment exception — see
 *   ADR-0019's "How this plays with Evergreen" note and this spec's Fix Scope). Every other
 *   branch (LTS release branches) must pin an exact version.
 */
export function validateExamplePackageJson(pkg: PackageJson, branch: string): string[] {
    const violations: string[] = [];

    for (const [dep, version] of Object.entries(pkg.dependencies ?? {})) {
        if (!isDotcmsPackage(dep)) {
            continue;
        }

        if (version === 'next' || version === '*') {
            violations.push(`examples: dependencies["${dep}"] is "${version}" on branch "${branch}" — never valid, on any branch`);
        } else if (version === 'latest' && branch !== 'main') {
            violations.push(
                `examples: dependencies["${dep}"] is "latest" on branch "${branch}" — only "main" may float; this branch must pin an exact version`
            );
        }
    }

    return violations;
}
