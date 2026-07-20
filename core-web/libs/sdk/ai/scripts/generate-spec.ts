/* eslint-disable no-console */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { transformSpec } from './spec-transform';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const DEFAULT_SPEC_PATH = '/api/openapi.json';
const DEFAULT_SPEC_URL = `https://corpsites-headless.dotcms.cloud${DEFAULT_SPEC_PATH}`;

/**
 * Resolve the OpenAPI spec source (a URL or local file path), in priority order:
 *   1. an explicit CLI arg (`... generate-spec -- <url-or-path>`)
 *   2. `DOTCMS_SPEC_URL` — env vars are inherited by the `generate-spec` task that `build`
 *      runs via `dependsOn` (CLI args are NOT), so this is what lets
 *      `DOTCMS_SPEC_URL=… nx build mcp-server` regenerate from a local instance in one command.
 *   3. `${DOTCMS_URL}/api/openapi.json` — convenience: reuse the same instance the runtime targets.
 *   4. the demo instance (so CI builds with no env set produce the committed spec).
 */
function resolveSpecSource(): string {
    if (process.argv[2]) {
        return process.argv[2];
    }
    if (process.env.DOTCMS_SPEC_URL) {
        return process.env.DOTCMS_SPEC_URL;
    }
    if (process.env.DOTCMS_URL) {
        return `${process.env.DOTCMS_URL.replace(/\/+$/, '')}${DEFAULT_SPEC_PATH}`;
    }
    return DEFAULT_SPEC_URL;
}

/** Load and parse the raw OpenAPI document (from a URL or a local file path) into memory. */
async function loadSpec(source: string): Promise<Record<string, unknown>> {
    const isUrl = source.startsWith('http://') || source.startsWith('https://');

    let body: string;
    if (isUrl) {
        console.log(`[generate-spec] Fetching spec from ${source}`);
        const response = await fetch(source);

        if (!response.ok) {
            throw new Error(
                `Failed to fetch OpenAPI spec from ${source}\n` +
                    `Status: ${response.status} ${response.statusText}\n\n` +
                    `Make sure the URL is correct and the dotCMS instance is running.`
            );
        }
        body = await response.text();
    } else {
        const filePath = path.resolve(source);
        if (!fs.existsSync(filePath)) {
            throw new Error(`OpenAPI spec file not found: ${filePath}`);
        }
        console.log(`[generate-spec] Reading spec from ${filePath}`);
        body = fs.readFileSync(filePath, 'utf-8');
    }

    try {
        return JSON.parse(body) as Record<string, unknown>;
    } catch {
        throw new Error(
            `Response from ${source} is not valid JSON.\n` +
                `Make sure the source points to a valid OpenAPI spec.`
        );
    }
}

/**
 * A minimal, valid OpenAPI document. Written only as a last resort when the spec source is
 * unreachable AND no previously-generated spec exists, so the static
 * `import spec from '../generated/spec.json'` in `src/spec/spec.ts` still resolves and the
 * build/bundle succeeds. The mcp-server ships with an empty API surface in that case, which is
 * a degraded-but-working build rather than a hard failure.
 */
const EMPTY_SPEC = {
    openapi: '3.0.1',
    info: { title: 'dotCMS API (unavailable at build time)', version: '0.0.0' },
    paths: {},
    components: { schemas: {} }
};

async function generateSpec() {
    const source = resolveSpecSource();
    const outDir = path.resolve(__dirname, '../src/generated');
    const outPath = path.join(outDir, 'spec.json');

    let raw: Record<string, unknown>;
    try {
        raw = await loadSpec(source);
    } catch (err) {
        // The spec is fetched from a live dotCMS instance at build time. In CI (or any offline
        // build) that instance may be unreachable — which must NOT break the build. Fall back to
        // an already-generated spec if one exists, otherwise emit a minimal placeholder so the
        // static JSON import still resolves. Either way we exit 0: a stale/empty spec is a
        // degraded build, not a broken one.
        const message = err instanceof Error ? err.message : String(err);
        console.warn(`[generate-spec] Could not load spec from ${source}: ${message}`);

        if (fs.existsSync(outPath)) {
            console.warn(`[generate-spec] Reusing existing spec at ${outPath}`);
            return;
        }

        console.warn(`[generate-spec] Writing minimal placeholder spec to ${outPath}`);
        fs.mkdirSync(outDir, { recursive: true });
        fs.writeFileSync(outPath, JSON.stringify(EMPTY_SPEC), 'utf-8');
        return;
    }

    const { spec, stats } = transformSpec(raw);

    // Compact JSON: this file is machine-read only (query results are re-stringified by the tool
    // handlers). Pretty-printing would add ~270KB for zero model benefit — use `jq` to inspect.
    const json = JSON.stringify(spec);

    fs.mkdirSync(outDir, { recursive: true });
    fs.writeFileSync(outPath, json, 'utf-8');

    const sizeKB = (Buffer.byteLength(json, 'utf-8') / 1024).toFixed(1);
    console.log(
        `[generate-spec] Wrote ${stats.pathCount} paths + ${stats.schemaCount} schemas ` +
            `(${sizeKB}KB) to ${outPath}`
    );
    if (stats.danglingRefs.length > 0) {
        console.warn(
            `[generate-spec] ${stats.danglingRefs.length} dangling $ref(s) left in place ` +
                `(not found in components.schemas): ${stats.danglingRefs.join(', ')}`
        );
    }
}

generateSpec().catch((err) => {
    console.error(`[generate-spec] Failed: ${err.message}`);
    process.exit(1);
});
