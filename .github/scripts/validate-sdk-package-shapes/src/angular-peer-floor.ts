/**
 * Guards @dotcms/angular's Angular peer range against the artifact it actually ships (#37680).
 *
 * The published package is partially compiled (ng-packagr's default for libraries): every
 * component, directive, pipe, injectable and factory is emitted as an `ɵɵngDeclare*({...})`
 * call that the consuming app's Angular linker turns into real Ivy code at build time. Each
 * call carries two version stamps:
 *
 *   i0.ɵɵngDeclareComponent({ minVersion: "17.0.0", version: "22.1.0", ngImport: i0, ... })
 *
 * - `minVersion` is the oldest linker the compiler claims can read that one declaration.
 * - `version` is the Angular compiler that produced it.
 *
 * `minVersion` on its own is NOT a usable floor. The build that caused #37680 had a highest
 * `minVersion` of "17.0.0" while emitting `ChangeDetectionStrategy.Eager` 18 times, which no
 * linker older than 21.2 can parse — the compiler does not bump `minVersion` for every enum
 * member it emits. Angular's supported contract for partially compiled libraries is instead
 * that the app must be on the same major as the Angular that built the library, or newer
 * (angular.dev, "Creating libraries" → "Ensuring library version compatibility"). So the floor
 * is the higher of the two: the compiler's major, and the highest `minVersion`.
 *
 * Since #37680 the SDK is built with the Angular pinned in core-web/libs/sdk/angular/toolchain
 * (its supported floor), not core-web's. This check is what keeps the declared peer range and
 * that pin in step, and it catches a build that silently fell back to a newer Angular.
 */

import semver from 'semver';

/** `ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "22.1.0", ...` — the shape ng-packagr emits. */
const DECLARATION = /ɵɵngDeclare(\w+)\(\{\s*minVersion:\s*"([^"]+)",\s*version:\s*"([^"]+)"/g;

/** Every declaration call, whatever its argument shape — used to detect ones DECLARATION missed. */
const ANY_DECLARATION_CALL = /ɵɵngDeclare\w+\(/g;

export type PartialDeclaration = {
    kind: string;
    minVersion: string;
    version: string;
};

export type AngularFloor = {
    /** The lowest Angular version the artifact can run on. */
    floor: string;
    /** Highest `minVersion` across all declarations. */
    highestMinVersion: string;
    /** Highest compiler `version` across all declarations. */
    compilerVersion: string;
    declarationCount: number;
};

type PackageJson = {
    peerDependencies?: Record<string, string>;
};

/**
 * Extracts every partial declaration's version stamps from an FESM bundle.
 *
 * Throws rather than returning a partial list when a declaration call does not have the
 * expected shape: a silently skipped declaration could be exactly the one with the highest
 * version, and the check would pass on an artifact it never fully read.
 */
export function readPartialDeclarations(source: string): PartialDeclaration[] {
    const declarations = [...source.matchAll(DECLARATION)].map(([, kind, minVersion, version]) => ({
        kind,
        minVersion,
        version
    }));

    const callCount = source.match(ANY_DECLARATION_CALL)?.length ?? 0;

    if (callCount !== declarations.length) {
        throw new Error(
            `found ${callCount} ɵɵngDeclare* calls but could only read version stamps from ${declarations.length} — the partial-compilation output format changed, update DECLARATION in angular-peer-floor.ts`
        );
    }

    return declarations;
}

/**
 * Derives the lowest Angular version the given declarations can run on.
 *
 * Throws on an empty list (the bundle was not partially compiled, or the wrong file was
 * passed) and on a stamp that is not a release version — `0.0.0-PLACEHOLDER` is what an
 * Angular built from source stamps, and it says nothing about the real floor.
 */
export function deriveAngularFloor(declarations: PartialDeclaration[]): AngularFloor {
    if (declarations.length === 0) {
        throw new Error('no ɵɵngDeclare* calls found — is this a partially compiled FESM bundle?');
    }

    for (const { kind, minVersion, version } of declarations) {
        for (const [field, value] of [
            ['minVersion', minVersion],
            ['version', version]
        ]) {
            if (!semver.valid(value) || semver.prerelease(value)?.includes('PLACEHOLDER')) {
                throw new Error(`ɵɵngDeclare${kind} has ${field} "${value}", which is not a release version`);
            }
        }
    }

    const highestMinVersion = declarations.map((d) => d.minVersion).sort(semver.rcompare)[0];
    const compilerVersion = declarations.map((d) => d.version).sort(semver.rcompare)[0];
    const compilerMajorFloor = `${semver.major(compilerVersion)}.0.0`;

    return {
        floor: semver.gt(highestMinVersion, compilerMajorFloor) ? highestMinVersion : compilerMajorFloor,
        highestMinVersion,
        compilerVersion,
        declarationCount: declarations.length
    };
}

/**
 * Checks that no `@angular/*` peer range admits a version below the artifact's floor.
 *
 * Only the lower bound is checked. Partial declarations are forward compatible — a newer
 * linker reads an older declaration — so an open-ended upper bound is not a defect this
 * check can prove, and choosing one is a support-policy decision, not a build fact.
 */
export function validateAngularPeerFloor(pkg: PackageJson, floor: AngularFloor): string[] {
    const violations: string[] = [];

    if (!pkg.peerDependencies?.['@angular/core']) {
        // Without the peer npm checks nothing at install time — the exact failure #37680 is about.
        violations.push('peerDependencies["@angular/core"] is missing — npm can only flag an incompatible Angular if the range is declared');
    }

    for (const [dep, range] of Object.entries(pkg.peerDependencies ?? {})) {
        if (!dep.startsWith('@angular/')) {
            continue;
        }

        const lowest = semver.validRange(range) ? semver.minVersion(range) : null;

        if (!lowest) {
            violations.push(`peerDependencies["${dep}"] is "${range}", which is not a semver range this check can evaluate`);
        } else if (semver.lt(lowest, floor.floor)) {
            violations.push(
                `peerDependencies["${dep}"] is "${range}", which admits ${lowest.version} — the built artifact needs Angular ${floor.floor} or newer (compiled with Angular ${floor.compilerVersion}; highest declaration minVersion ${floor.highestMinVersion}). Raise the lower bound to at least ${floor.floor}.`
            );
        }
    }

    return violations;
}
