#!/usr/bin/env node
/**
 * Scan the TEST-layer module graph for packages that import what they do not declare.
 *
 * Companion to scan-undeclared-imports.mjs, and the one that actually produced a work list.
 * The difference is the population each looks at: that one sweeps the whole installed tree and
 * reports hundreds of findings, most of which no bundler ever walks. This one looks only at the
 * packages the vite configs name in `test.server.deps.inline` — 28 of them — which IS the set
 * Vitest pulls through its own resolver. Small enough to act on, and complete for that graph.
 *
 * Why it exists at all: `nx build` going green proves nothing about the test layer. Under the
 * global virtual store the production bundle was clean while the entire Vitest suite failed to
 * load, because @analogjs/vitest-angular and @openng/spectator have the same manifest defect and
 * live in a graph the bundler never enters. Every module graph is its own authority.
 *
 * Run from core-web, under either virtual store layout:
 *
 *   pnpm exec node tools/scan-inlined-imports.mjs
 *
 * A finding is reported only when core-web itself can resolve the specifier — that is what makes
 * it an accidental rescue that the global store will take away, rather than a genuinely absent
 * optional dependency.
 *
 * See specs/37573-pnpm-global-virtual-store/ (FR-005) and #37573.
 */

import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { execSync } from 'node:child_process';
import { createRequire } from 'node:module';

const require = createRequire(join(process.cwd(), 'index.js'));

const configs = execSync(
    'ls apps/*/vite.config.mts libs/*/vite.config.mts libs/*/*/vite.config.mts libs/*/*/*/vite.config.mts libs/*/*/*/*/vite.config.mts 2>/dev/null || true',
    { encoding: 'utf8' }
)
    .trim()
    .split('\n')
    .filter(Boolean);

const packages = new Set();
for (const config of configs) {
    const text = readFileSync(config, 'utf8');
    // deps.inline entries look like  /@scope\/name/  or  /name/
    for (const m of text.matchAll(/\/(@[a-z0-9-]+)\\\/([a-z0-9.-]+)\//g)) packages.add(`${m[1]}/${m[2]}`);
    for (const m of text.matchAll(/^\s+\/([a-z][a-z0-9-]*)(?:\\\.[a-z]+)?\/,/gm)) packages.add(m[1]);
}

const SKIP = new Set(['node_modules', 'test', 'tests', '__tests__', 'examples', 'example', 'docs', 'bin']);
const findings = [];

for (const pkg of [...packages].sort()) {
    const manifestPath = join('node_modules', pkg, 'package.json');
    if (!existsSync(manifestPath)) continue;

    const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
    const declared = new Set([
        ...Object.keys(manifest.dependencies ?? {}),
        ...Object.keys(manifest.peerDependencies ?? {}),
        ...Object.keys(manifest.optionalDependencies ?? {})
    ]);

    const files = [];
    const walk = (dir, depth) => {
        if (depth > 3 || files.length > 80) return;
        let entries;
        try {
            entries = readdirSync(dir, { withFileTypes: true });
        } catch {
            return;
        }
        for (const entry of entries) {
            const full = join(dir, entry.name);
            if (entry.isDirectory()) {
                if (!SKIP.has(entry.name) && !entry.name.startsWith('.')) walk(full, depth + 1);
            } else if (/\.(mjs|js)$/.test(entry.name)) {
                files.push(full);
            }
        }
    };
    walk(join('node_modules', pkg), 0);

    const missing = new Set();
    for (const file of files) {
        let source;
        try {
            source = readFileSync(file, 'utf8');
        } catch {
            continue;
        }
        for (const m of source.matchAll(/from ['"]([^.'"][^'"]*)['"]/g)) {
            const spec = m[1];
            if (spec.startsWith('node:')) continue;
            const name = spec.startsWith('@') ? spec.split('/').slice(0, 2).join('/') : spec.split('/')[0];
            if (name === pkg || declared.has(name)) continue;
            // Only report it if core-web can actually resolve it — that is what makes it an
            // accidental rescue rather than a genuinely absent optional dependency.
            try {
                require.resolve(name);
                missing.add(name);
            } catch {
                /* not resolvable from the project either: not a rescue */
            }
        }
    }

    if (missing.size) findings.push(`${pkg}@${manifest.version}  ->  ${[...missing].sort().join(', ')}`);
}

console.log(`Vitest-inlined packages examined: ${packages.size}`);
console.log(findings.length ? findings.join('\n') : '(no undeclared imports)');
