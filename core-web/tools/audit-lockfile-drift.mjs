#!/usr/bin/env node
/**
 * Assert that the packageExtensions corrections moved no resolved version they had no business
 * moving.
 *
 * The global virtual store is a per-developer opt-in, but the corrections that make it work are
 * committed for EVERYONE — local store included, pipeline included. That is this change's real
 * exposure, and it hides well: `packageExtensions` participates in resolution, so adding a peer
 * changes that package's peer-suffixed key and every dependent's key along with it. The lockfile
 * diff comes out large and almost entirely semantically empty, which is exactly the shape in which
 * a real change goes unnoticed in review.
 *
 * pnpm's lockfile separates the two cleanly, and this audit checks BOTH:
 *   packages:   keyed `name@version`     -> resolved versions. Any drift outside the correction
 *                                           set is a finding.
 *   snapshots:  keyed with peer suffixes -> the dependency graph. Churn is expected here, but not
 *                                           unexamined: every new key must trace to a corrected
 *                                           package or a dependent of one.
 *
 * An earlier version audited versions only, and reported "clean" on a +75/-21 diff — because a
 * package extension adds edges and peer-suffixed snapshot keys without moving a single version.
 * That is precisely the change this tool exists to catch, so a version-only check is not enough.
 * (Raised in review on #37654.)
 *
 * See specs/37573-pnpm-global-virtual-store/ (task T029, FR-014).
 *
 *   node tools/audit-lockfile-drift.mjs [--base origin/main] [--json]
 *
 * Corrected packages are read from the packageExtensions keys in pnpm-workspace.yaml, so the
 * allowlist cannot drift out of sync with the corrections themselves.
 *
 * Exit codes: 0 no unexpected drift, 1 drift found, 2 could not run.
 */

import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const CORE_WEB = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const LOCKFILE = join(CORE_WEB, 'pnpm-lock.yaml');
const WORKSPACE = join(CORE_WEB, 'pnpm-workspace.yaml');

const argv = process.argv.slice(2);
const base = argv.includes('--base') ? argv[argv.indexOf('--base') + 1] : 'origin/main';
const asJson = argv.includes('--json');

/**
 * Map package name -> set of resolved versions, read from the lockfile's `packages:` section only.
 *
 * Parsed with a line scanner rather than a YAML library on purpose: this must run before
 * `pnpm install` has put anything in node_modules, so it cannot depend on one.
 *
 * Note that pnpm 12 writes `pnpm-lock.yaml` as TWO YAML documents separated by `---`: the first
 * pins the package manager itself (`packageManagerDependencies`, i.e. the `@pnpm/exe.*` binaries),
 * the second is the project. This scanner deliberately reads both, so bumping pnpm shows up as
 * drift on `@pnpm/exe.*` — which is correct, but means a pnpm bump and these corrections should
 * not share a pull request, or the signal muddies.
 */
function resolvedVersions(lockfileText) {
    const versions = new Map();
    let inPackages = false;

    for (const line of lockfileText.split('\n')) {
        if (/^[a-zA-Z]/.test(line)) {
            inPackages = line.startsWith('packages:');
            continue;
        }
        if (!inPackages) continue;

        // Two-space-indented entries are the keys; deeper lines are their bodies.
        const match = line.match(/^ {2}'?((?:@[^/@]+\/)?[^@'\s]+)@([^':\s(]+)'?:/);
        if (!match) continue;

        const [, name, version] = match;
        if (!versions.has(name)) versions.set(name, new Set());
        versions.get(name).add(version);
    }

    return versions;
}

/** Package names appearing as packageExtensions keys, with any `@range` suffix stripped. */
function correctedPackages(workspaceText) {
    const corrected = new Set();
    let inBlock = false;

    for (const line of workspaceText.split('\n')) {
        if (/^[a-zA-Z]/.test(line)) {
            inBlock = line.startsWith('packageExtensions:');
            continue;
        }
        if (!inBlock) continue;

        const match = line.match(/^ {4}'?((?:@[^/@]+\/)?[^@'\s]+)(?:@[^':\s]+)?'?:/);
        if (match) corrected.add(match[1]);
    }

    return corrected;
}

function gitShow(ref, path) {
    return execFileSync('git', ['show', `${ref}:${path}`], {
        cwd: CORE_WEB,
        encoding: 'utf8',
        maxBuffer: 256 * 1024 * 1024
    });
}

function main() {
    if (!existsSync(LOCKFILE)) {
        console.error(`No lockfile at ${LOCKFILE}`);
        process.exit(2);
    }

    let baseLock;
    try {
        baseLock = gitShow(base, 'core-web/pnpm-lock.yaml');
    } catch {
        console.error(
            `Could not read core-web/pnpm-lock.yaml at ${base}.\n` +
                `Fetch it first: git fetch origin main`
        );
        process.exit(2);
    }

    const before = resolvedVersions(baseLock);
    const after = resolvedVersions(readFileSync(LOCKFILE, 'utf8'));
    const allowed = existsSync(WORKSPACE) ? correctedPackages(readFileSync(WORKSPACE, 'utf8')) : new Set();

    // ---- Axis 2: dependency edges and peer-resolution snapshots -----------------------------
    // Raised in review on #37654 and correct: a package extension adds edges and new
    // peer-suffixed `snapshots:` keys WITHOUT moving any version, so a version-only audit reports
    // "clean" on exactly the change it was written to catch. Every changed snapshot key must trace
    // to a corrected package or to something that depends on one.
    const snapshotKeys = (text) => {
        const keys = new Set();
        let inSnapshots = false;
        for (const line of text.split('\n')) {
            if (/^[a-zA-Z]/.test(line)) {
                inSnapshots = line.startsWith('snapshots:');
                continue;
            }
            if (!inSnapshots) continue;
            const match = line.match(/^ {2}'?(.+?)'?:$/);
            if (match) keys.add(match[1]);
        }

        return keys;
    };

    const beforeSnapshots = snapshotKeys(baseLock);
    const afterSnapshots = snapshotKeys(readFileSync(LOCKFILE, 'utf8'));
    const changedSnapshots = [...afterSnapshots].filter((k) => !beforeSnapshots.has(k));
    const untraceable = changedSnapshots.filter(
        (key) => ![...allowed].some((pkg) => key.includes(pkg))
    );

    const drift = [];
    const names = new Set([...before.keys(), ...after.keys()]);

    for (const name of names) {
        const was = [...(before.get(name) ?? [])].sort();
        const now = [...(after.get(name) ?? [])].sort();
        if (was.join(',') === now.join(',')) continue;

        drift.push({
            package: name,
            before: was,
            after: now,
            expected: allowed.has(name)
        });
    }

    const unexpected = drift.filter((d) => !d.expected);

    if (asJson) {
        console.log(JSON.stringify({ base, corrected: [...allowed], drift, changedSnapshots: changedSnapshots.length, untraceable }, null, 2));
        process.exit(unexpected.length > 0 || untraceable.length > 0 ? 1 : 0);
    }

    for (const d of drift.filter((x) => x.expected)) {
        console.log(`ok    ${d.package}: ${d.before.join(', ') || '(absent)'} -> ${d.after.join(', ') || '(absent)'}  [corrected package]`);
    }

    for (const d of unexpected) {
        console.error(`DRIFT ${d.package}: ${d.before.join(', ') || '(absent)'} -> ${d.after.join(', ') || '(absent)'}`);
    }

    console.log(
        `\n${allowed.size} corrected package(s) declared in pnpm-workspace.yaml.\n` +
            `${drift.length - unexpected.length} expected change(s), ${unexpected.length} unexpected.`
    );

    console.log(
        `\nAxis 2 — dependency edges / peer snapshots: ${changedSnapshots.length} new snapshot key(s), ` +
            `${untraceable.length} not traceable to a corrected package.`
    );

    for (const key of untraceable.slice(0, 20)) console.error(`UNTRACED ${key}`);
    if (untraceable.length > 20) console.error(`… and ${untraceable.length - 20} more`);

    if (unexpected.length === 0 && untraceable.length === 0) {
        console.log(
            '\nNo resolved version moved outside the correction set, and every new peer-suffixed\n' +
                'snapshot key traces to a corrected package or a dependent of one.'
        );
    }

    process.exit(unexpected.length > 0 || untraceable.length > 0 ? 1 : 0);
}

main();
