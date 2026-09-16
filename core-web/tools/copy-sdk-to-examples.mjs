#!/usr/bin/env node
/* eslint-disable no-console -- this is a CLI; its output is the point */
/**
 * Copy the locally built SDKs over the copies installed in the example apps.
 *
 * The examples depend on `"@dotcms/*": "latest"` from npm, deliberately — see
 * specs/37399-sdk-packaging-version-fix. That means `npm run build` in an example validates
 * the *published* packages, not your working tree, so an SDK change can look fine in an
 * example and still be broken.
 *
 * This overwrites the installed packages in place. It never touches an example's
 * package.json, so the manifests keep floating on `latest` and the
 * validate-sdk-package-shapes CI job stays green. To undo, reinstall the example.
 *
 * Usage:
 *   pnpm nx run-many -t build --projects='sdk-*'
 *   node tools/copy-sdk-to-examples.mjs              # every example
 *   node tools/copy-sdk-to-examples.mjs nextjs astro # only these
 */
import { cpSync, existsSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const CORE_WEB = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const SDK_DIST = join(CORE_WEB, 'dist/libs/sdk');
const EXAMPLES = resolve(CORE_WEB, '../examples');

/** Maps `@dotcms/<name>` to the directory holding its build output. */
function buildArtifactsByPackageName() {
    if (!existsSync(SDK_DIST)) {
        console.error(
            `No build output at ${SDK_DIST}.\n` +
                "Build the SDKs first:  pnpm nx run-many -t build --projects='sdk-*'"
        );
        process.exit(1);
    }

    const artifacts = new Map();

    for (const entry of readdirSync(SDK_DIST, { withFileTypes: true })) {
        if (!entry.isDirectory()) continue;

        const manifest = join(SDK_DIST, entry.name, 'package.json');

        if (!existsSync(manifest)) continue;

        artifacts.set(JSON.parse(readFileSync(manifest, 'utf-8')).name, join(SDK_DIST, entry.name));
    }

    return artifacts;
}

function copyInto(example, artifacts) {
    const root = join(EXAMPLES, example);
    const manifestPath = join(root, 'package.json');

    if (!existsSync(manifestPath)) {
        console.warn(`  ${example}: no package.json, skipped`);

        return;
    }

    const nodeModules = join(root, 'node_modules');

    if (!existsSync(nodeModules)) {
        console.warn(`  ${example}: no node_modules — run \`npm install\` there first, skipped`);

        return;
    }

    const manifest = JSON.parse(readFileSync(manifestPath, 'utf-8'));
    const dependencies = Object.keys({
        ...manifest.dependencies,
        ...manifest.devDependencies
    }).filter((name) => name.startsWith('@dotcms/'));

    const copied = [];
    const missing = [];

    for (const name of dependencies) {
        const source = artifacts.get(name);

        if (!source) {
            missing.push(name);
            continue;
        }

        const target = join(nodeModules, name);
        rmSync(target, { recursive: true, force: true });
        cpSync(source, target, { recursive: true });
        copied.push(name);
    }

    console.log(`  ${example}: ${copied.length ? copied.join(', ') : 'nothing to copy'}`);

    if (missing.length) {
        console.warn(
            `  ${example}: not built locally, left as installed — ${missing.join(', ')}`
        );
    }
}

const artifacts = buildArtifactsByPackageName();
const requested = process.argv.slice(2);
const examples = requested.length
    ? requested
    : readdirSync(EXAMPLES, { withFileTypes: true })
          .filter((entry) => entry.isDirectory())
          .map((entry) => entry.name);

console.log(`Copying locally built SDKs into ${examples.length} example(s):`);
examples.forEach((example) => copyInto(example, artifacts));
