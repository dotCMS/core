#!/usr/bin/env node
'use strict';

/**
 * CLI wrapper around resolve.js. Everything here is I/O; the decision itself lives in
 * resolve.js as a pure function so it can be unit-tested without a release, a network or a
 * filesystem.
 *
 * Usage (from the repo root):
 *   node .github/scripts/resolve-sdk-pin/cli.js \
 *     --release-version 26.08.08_lts_v1 \
 *     --is-lts true \
 *     --commit-date 2026-08-08T12:00:00Z \
 *     [--examples-dir examples] \
 *     [--min-sdk-file dotCMS/src/main/java/com/dotcms/rest/config/MinSdkVersion.java] \
 *     [--registry https://registry.npmjs.org]
 *
 * Prints one JSON object on stdout:
 *   {"action":"pin","version":"26.8.7-1","verifyPublished":true,"cutoff":"2026-08-08"}
 *   {"action":"keep"}   {"action":"none"}
 *
 * On refusal: a GitHub `::error::` line on stderr and exit 1. Refusing is the point — every
 * failure mode here ends with a broken `npm install` for a customer if it guesses instead.
 */

const fs = require('fs');
const path = require('path');
const { decide, ResolutionError } = require('./resolve');

const REFERENCE_PACKAGE = '@dotcms/client';

function parseArgs(argv) {
    const args = {};
    for (let i = 0; i < argv.length; i += 2) {
        if (!argv[i].startsWith('--')) throw new Error(`Unexpected argument: ${argv[i]}`);
        args[argv[i].slice(2)] = argv[i + 1];
    }
    return args;
}

/** Every examples/<app>/package.json, matching the workflow's `find -maxdepth 2`. */
function readExampleManifests(examplesDir) {
    if (!fs.existsSync(examplesDir)) return [];
    return fs
        .readdirSync(examplesDir, { withFileTypes: true })
        .filter((entry) => entry.isDirectory() && entry.name !== 'node_modules')
        .map((entry) => path.join(examplesDir, entry.name, 'package.json'))
        .filter((file) => fs.existsSync(file))
        .map((file) => ({ file, json: JSON.parse(fs.readFileSync(file, 'utf8')) }));
}

function dotcmsDependencies(manifests) {
    const specs = [];
    const packages = new Set();
    for (const { json } of manifests) {
        for (const [name, spec] of Object.entries(json.dependencies || {})) {
            if (name.startsWith('@dotcms/')) {
                specs.push(spec);
                packages.add(name);
            }
        }
    }
    return { specs, packages: [...packages].sort() };
}

function readMinSdkValue(file) {
    if (!file || !fs.existsSync(file)) return null;
    const match = /VALUE\s*=\s*"([^"]+)"/.exec(fs.readFileSync(file, 'utf8'));
    return match ? match[1] : null;
}

async function fetchJson(url) {
    const response = await fetch(url);
    if (!response.ok) {
        const error = new Error(`${url} -> HTTP ${response.status}`);
        error.status = response.status;
        throw error;
    }
    return response.json();
}

async function main() {
    const args = parseArgs(process.argv.slice(2));
    const registry = (args.registry || 'https://registry.npmjs.org').replace(/\/$/, '');
    const isLts = args['is-lts'] === 'true';

    const manifests = readExampleManifests(args['examples-dir'] || 'examples');
    const { specs, packages } = dotcmsDependencies(manifests);

    // Only the LTS path needs publish dates, and only when something actually floats.
    // Skipping the request otherwise keeps a normal release independent of the registry.
    let registryTime = {};
    if (isLts && specs.length > 0 && !specs.every((spec) => /^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/.test(spec))) {
        registryTime = (await fetchJson(`${registry}/${REFERENCE_PACKAGE}`)).time || {};
    }

    const result = decide({
        specs,
        isLts,
        releaseVersion: args['release-version'],
        commitDate: args['commit-date'],
        registryTime,
        minSdkValue: readMinSdkValue(args['min-sdk-file'])
    });

    // Existence check. Every @dotcms/* package the examples reference, not just the
    // reference one: siblings publish in lockstep but an individual publish can fail, and
    // pinning to a version a package never got is the same broken install.
    if (result.action === 'pin' && result.verifyPublished) {
        for (const name of packages) {
            try {
                await fetchJson(`${registry}/${name}/${result.version}`);
            } catch (error) {
                throw new ResolutionError(
                    `${name}@${result.version} is not published on npm (${error.message}). Pinning it would ` +
                        'break the install in every example app that depends on it.'
                );
            }
        }
        result.verified = packages;
    }

    process.stdout.write(`${JSON.stringify(result)}\n`);
}

main().catch((error) => {
    const message = error instanceof ResolutionError ? error.message : `resolve-sdk-pin failed: ${error.message}`;
    process.stderr.write(`::error::${message}\n`);
    process.exit(1);
});
