/* eslint-disable no-console */
import { parse as parseYaml } from 'yaml';

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { transformSpec } from './spec-transform';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * The committed, auto-generated spec that ships with the backend. `swagger-maven-plugin`
 * writes it at compile phase and CI verifies the working copy matches — so it's always
 * present and offline, no running dotCMS instance required. This is the only source:
 * spec generation reads this local YAML file and nothing else.
 * Resolved relative to this script (the `generate-spec` task runs with cwd `libs/sdk/ai`).
 */
const LOCAL_SPEC_FILE = path.resolve(
    __dirname,
    '../../../../../dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml'
);

/**
 * Resolve the OpenAPI spec file to read. Defaults to the committed local `openapi.yaml`;
 * an explicit CLI arg (`... generate-spec -- <path>`) can point at an alternate local YAML.
 */
function resolveSpecFile(): string {
    return process.argv[2] ? path.resolve(process.argv[2]) : LOCAL_SPEC_FILE;
}

/** Parse an OpenAPI YAML document into memory. */
function parseSpec(body: string, filePath: string): Record<string, unknown> {
    try {
        const parsed = parseYaml(body);
        if (!parsed || typeof parsed !== 'object') {
            throw new Error('parsed value is not an object');
        }
        return parsed as Record<string, unknown>;
    } catch (err) {
        const detail = err instanceof Error ? err.message : String(err);
        throw new Error(`${filePath} is not a valid OpenAPI YAML spec: ${detail}`);
    }
}

/**
 * Normalize path keys to the full `/api/...` form.
 *
 * The committed `openapi.yaml` declares `servers: [{ url: '/' }]` and lists routes WITHOUT the
 * `/api` prefix (e.g. `/v1/page/...`), while the routes are actually served under `/api` at
 * runtime. `ALLOWED_PREFIXES`/`EXCLUDED_PATTERNS` are written against the full `/api/...` form,
 * so prepend `/api` to any path key that lacks it. Idempotent: paths already under `/api` are
 * left untouched.
 */
function normalizeApiPrefix(spec: Record<string, unknown>): Record<string, unknown> {
    const paths = spec.paths as Record<string, unknown> | undefined;
    if (!paths) return spec;

    const normalized: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(paths)) {
        const newKey = key.startsWith('/api/') || key === '/api' ? key : `/api${key}`;
        normalized[newKey] = value;
    }
    spec.paths = normalized;
    return spec;
}

/** Read and parse the raw OpenAPI document from the local YAML file. */
function loadSpec(filePath: string): Record<string, unknown> {
    if (!fs.existsSync(filePath)) {
        throw new Error(`OpenAPI spec file not found: ${filePath}`);
    }
    console.log(`[generate-spec] Reading spec from ${filePath}`);
    const body = fs.readFileSync(filePath, 'utf-8');

    return normalizeApiPrefix(parseSpec(body, filePath));
}

function generateSpec() {
    const filePath = resolveSpecFile();
    const raw = loadSpec(filePath);

    const { spec, stats } = transformSpec(raw);

    // Compact JSON: this file is machine-read only (query results are re-stringified by the tool
    // handlers). Pretty-printing would add ~270KB for zero model benefit — use `jq` to inspect.
    const json = JSON.stringify(spec);

    const outDir = path.resolve(__dirname, '../src/generated');
    const outPath = path.join(outDir, 'spec.json');

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

try {
    generateSpec();
} catch (err) {
    console.error(`[generate-spec] Failed: ${err instanceof Error ? err.message : String(err)}`);
    process.exit(1);
}
