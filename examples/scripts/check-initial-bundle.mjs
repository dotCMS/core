#!/usr/bin/env node
/* eslint-disable no-console -- this is a CLI; its output is the point */
/**
 * Assert that things which should be lazily loaded are absent from an example's INITIAL
 * route JavaScript.
 *
 * The distinction matters: a `next/dynamic` or `React.lazy` component still ends up in the
 * production output, just in a chunk of its own. Grepping the whole output directory proves
 * nothing. This resolves the set of chunks the browser actually downloads to render a route
 * before any interaction, and searches only those.
 *
 *   Next.js  the route's client-reference manifest (every chunk its client modules pull in)
 *            plus rootMainFiles from build-manifest.json.
 *   Astro    the module scripts each built HTML page references, followed through their
 *            static imports only — `import()` is a lazy boundary and is deliberately not
 *            followed.
 *
 * Usage (after building the example):
 *   node ../scripts/check-initial-bundle.mjs next  examples/nextjs
 *   node ../scripts/check-initial-bundle.mjs astro examples/astro
 */
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { gzipSync } from 'node:zlib';

/**
 * Strings that must not appear in the initial route JavaScript.
 *
 * `mustExistSomewhere` guards the other direction: a needle that has vanished from the build
 * entirely would pass the absence check for the wrong reason, so each one is also required to
 * be present in some non-initial chunk.
 */
const FORBIDDEN = [
    {
        needle: '__dotcms_unused_component_probe__',
        why: 'A mapped content type that no page uses is in the initial bundle. The component map is probably back to static imports.',
        mustExistSomewhere: true
    },
    {
        // NOT the bare string "tinymce": @dotcms/uve/internal legitimately ships the editor's
        // URL (`/ext/tinymcev7/tinymce.min.js`) and its toolbar config objects, and those are a
        // few hundred bytes of strings rather than the editor. `tinymceScriptSrc` is the prop
        // the @tinymce/tinymce-react wrapper takes, so it appears only where the real
        // integration was bundled.
        needle: 'tinymceScriptSrc',
        why: 'The TinyMCE React integration is in the initial bundle. DotCMSEditableText should only load it once the UVE enters edit mode.',
        mustExistSomewhere: true
    }
];

function walk(dir, predicate, found = []) {
    if (!existsSync(dir)) return found;

    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const full = join(dir, entry.name);

        if (entry.isDirectory()) walk(full, predicate, found);
        else if (predicate(full)) found.push(full);
    }

    return found;
}

/** Chunks the browser loads for each Next.js route before any interaction. */
async function nextInitialChunks(root) {
    const next = join(root, '.next');
    const chunks = new Set();

    const buildManifest = JSON.parse(readFileSync(join(next, 'build-manifest.json'), 'utf-8'));
    for (const file of buildManifest.rootMainFiles ?? []) {
        chunks.add(join(next, file));
    }

    const manifests = walk(join(next, 'server'), (f) =>
        f.endsWith('page_client-reference-manifest.js')
    );

    for (const manifest of manifests) {
        globalThis.__RSC_MANIFEST = {};
        // The manifest is a plain assignment to globalThis, not JSON — evaluate it.
        new Function(readFileSync(manifest, 'utf-8'))();

        for (const route of Object.values(globalThis.__RSC_MANIFEST)) {
            for (const entry of Object.values(route.clientModules ?? {})) {
                for (const chunk of entry.chunks ?? []) {
                    chunks.add(join(next, chunk.replace(/^\/_next\//, '')));
                }
            }
        }
    }

    delete globalThis.__RSC_MANIFEST;

    return [...chunks].filter((file) => existsSync(file) && statSync(file).isFile());
}

/**
 * Astro's build emits no static HTML here (the Vercel adapter renders on demand) and its
 * server manifest lists every asset, lazy ones included, so neither gives an initial-chunk
 * list directly.
 *
 * So classify each chunk by how it is referenced instead. A chunk is genuinely deferred when
 * nothing imports it statically AND something reaches it through `import()`. Requiring both
 * matters: an island entry chunk also has no static importer, so "no static importer" alone
 * would pass even if the component were inlined straight into the entry.
 *
 * @returns every client chunk, plus the chunks reached statically and dynamically
 */
function astroChunkGraph(root) {
    const dist = join(root, 'dist/client');
    const chunks = walk(dist, (f) => f.endsWith('.js'));
    const staticImporters = new Map(chunks.map((file) => [file, new Set()]));
    const dynamicallyImported = new Set();

    const resolveSpec = (file, spec) =>
        resolve(spec.startsWith('/') ? dist : dirname(file), spec.replace(/^\//, ''));

    for (const file of chunks) {
        const code = readFileSync(file, 'utf-8');

        for (const [, spec] of code.matchAll(
            /(?:^|[^.\w$])(?:import|export)\s*(?:[^'"()]*?from\s*)?["']([^"']+)["']/g
        )) {
            if (!spec.startsWith('.') && !spec.startsWith('/')) continue;

            const resolved = resolveSpec(file, spec);

            if (staticImporters.has(resolved)) staticImporters.get(resolved).add(file);
        }

        for (const [, spec] of code.matchAll(/import\s*\(\s*["']([^"']+)["']\s*\)/g)) {
            if (!spec.startsWith('.') && !spec.startsWith('/')) continue;

            dynamicallyImported.add(resolveSpec(file, spec));
        }
    }

    return { chunks, staticImporters, dynamicallyImported };
}

/**
 * Total weight of the JavaScript a route downloads before any interaction.
 *
 * @param files the initial chunk paths
 * @returns raw and gzip byte totals, and the file count
 */
function weigh(files) {
    let raw = 0;
    let gzip = 0;

    for (const file of files) {
        const contents = readFileSync(file);
        raw += contents.byteLength;
        gzip += gzipSync(contents).byteLength;
    }

    return { files: files.length, raw, gzip };
}

const kb = (bytes) => `${(bytes / 1024).toFixed(1)} KB`;

const [framework, target, ...flags] = process.argv.slice(2);
const reportOnly = flags.includes('--report');

if (!framework || !target) {
    console.error('Usage: check-initial-bundle.mjs <next|astro> <example-dir> [--report]');
    process.exit(2);
}

const root = resolve(target);

if (framework === 'astro') {
    const { chunks, staticImporters, dynamicallyImported } = astroChunkGraph(root);

    if (!chunks.length) {
        console.error(
            `No client chunks found under ${join(root, 'dist/client')}. Build the example first.`
        );
        process.exit(2);
    }

    const totals = weigh(chunks);
    console.log(
        `${target}: ${totals.files} client chunk(s), ${kb(totals.raw)} raw, ${kb(totals.gzip)} gzip`
    );

    if (reportOnly) {
        process.exit(0);
    }

    let astroFailed = false;

    for (const { needle, why } of FORBIDDEN) {
        const carriers = chunks.filter((file) => readFileSync(file, 'utf-8').includes(needle));

        if (!carriers.length) {
            astroFailed = true;
            console.error(
                `\n  FAIL  "${needle}" is nowhere in the client build. It was not deferred, it\n` +
                    '        disappeared. Check the feature is still wired up before trusting this check.'
            );
            continue;
        }

        const eager = carriers.filter(
            (file) => staticImporters.get(file).size > 0 || !dynamicallyImported.has(file)
        );

        if (eager.length) {
            astroFailed = true;
            console.error(`\n  FAIL  "${needle}" is not behind a dynamic import.\n        ${why}`);
            for (const file of eager.slice(0, 5)) {
                const by = [...staticImporters.get(file)].map((f) => relative(root, f));
                console.error(
                    `          ${relative(root, file)} — ` +
                        (by.length
                            ? `statically imported by: ${by.join(', ')}`
                            : 'never reached through import(), so it is an eager entry chunk')
                );
            }
            continue;
        }

        console.log(
            `  ok    "${needle}" lives in ${carriers.length} chunk(s), each reached only through import()`
        );
    }

    process.exit(astroFailed ? 1 : 0);
}

const chunks = await nextInitialChunks(root);

if (!chunks.length) {
    console.error(
        `No initial chunks found under ${root}. Build the example first, and check that the ` +
            'build output layout has not changed.'
    );
    process.exit(2);
}

const totals = weigh(chunks);
console.log(
    `${target}: ${totals.files} initial-route chunk(s), ${kb(totals.raw)} raw, ${kb(totals.gzip)} gzip`
);

if (reportOnly) {
    process.exit(0);
}

const initial = new Set(chunks);
// Client output only. Walking all of .next would let a needle that survives solely in the
// server bundle satisfy the "still exists" check, hiding a component that vanished from the
// browser entirely.
const allOutput = walk(join(root, '.next/static'), (f) => f.endsWith('.js'));

let failed = false;

for (const { needle, why, mustExistSomewhere } of FORBIDDEN) {
    const hits = chunks.filter((file) => readFileSync(file, 'utf-8').includes(needle));

    if (hits.length) {
        failed = true;
        console.error(`\n  FAIL  "${needle}" found in the initial route JavaScript.\n        ${why}`);
        hits.slice(0, 5).forEach((file) => console.error(`          ${relative(root, file)}`));
        continue;
    }

    if (mustExistSomewhere) {
        const deferred = allOutput.filter(
            (file) => !initial.has(file) && readFileSync(file, 'utf-8').includes(needle)
        );

        if (!deferred.length) {
            failed = true;
            console.error(
                `\n  FAIL  "${needle}" is absent from the initial route — but also from the rest of\n` +
                    '        the build. It was not deferred, it disappeared. Check that the feature\n' +
                    '        is still wired up before trusting this check.'
            );
            continue;
        }

        console.log(
            `  ok    "${needle}" absent from the initial route, deferred to ${deferred.length} chunk(s)`
        );
        continue;
    }

    console.log(`  ok    "${needle}" absent from the initial route JavaScript`);
}

process.exit(failed ? 1 : 0);
