/* eslint-disable no-console */
import SwaggerParser from '@apidevtools/swagger-parser';

import fs from 'node:fs';
import path from 'node:path';

const ALLOWED_PREFIXES = [
    '/api/v1/contenttype',
    '/api/v1/page',
    '/api/v1/nav',
    '/api/v1/workflow',
    '/api/v1/category',
    '/api/v1/tags',
    '/api/v1/folder',
    '/api/v1/site',
    '/api/v1/language',
    '/api/v1/role',
    '/api/v1/user',
    '/api/v1/containers',
    '/api/v1/themes',
    '/api/v1/templates',
    '/api/v1/assets',
    // '/api/es/search'
    '/api/content/_search'
];

const DEFAULT_SPEC_PATH = '/api/openapi.json';

function resolveSpecSource(): string {
    const arg = process.argv[2];
    if (arg) {
        return arg;
    }

    console.error(`[generate-spec] ERROR: No OpenAPI spec source provided.

Usage:
  npx tsx scripts/generate-spec.ts <url-or-path>

Examples:
  npx tsx scripts/generate-spec.ts https://demo.dotcms.com${DEFAULT_SPEC_PATH}
  npx tsx scripts/generate-spec.ts ./openapi.json
`);
    process.exit(1);
}

async function fetchSpec(source: string): Promise<string> {
    const isUrl = source.startsWith('http://') || source.startsWith('https://');

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

        const tempPath = path.resolve('.openapi-temp.json');
        const body = await response.text();

        // Validate it's actually JSON
        try {
            JSON.parse(body);
        } catch {
            throw new Error(
                `Response from ${source} is not valid JSON.\n` +
                    `Make sure the URL points to a valid OpenAPI spec endpoint.`
            );
        }

        fs.writeFileSync(tempPath, body, 'utf-8');
        return tempPath;
    }

    // Local file path
    const filePath = path.resolve(source);
    if (!fs.existsSync(filePath)) {
        throw new Error(`OpenAPI spec file not found: ${filePath}`);
    }

    console.log(`[generate-spec] Reading spec from ${filePath}`);
    return filePath;
}

async function generateSpec() {
    const source = resolveSpecSource();
    const specPath = await fetchSpec(source);

    try {
        // Dereference all $ref pointers
        const api = (await SwaggerParser.dereference(specPath)) as Record<string, unknown>;

        // Filter paths to allowed prefixes
        const allPaths = (api.paths || {}) as Record<string, unknown>;
        const filteredPaths: Record<string, unknown> = {};

        for (const [pathKey, pathValue] of Object.entries(allPaths)) {
            if (ALLOWED_PREFIXES.some((prefix) => pathKey.startsWith(prefix))) {
                // Strip response schemas but keep description and content types
                const methods = pathValue as Record<string, unknown>;
                const strippedMethods: Record<string, unknown> = {};

                for (const [method, methodValue] of Object.entries(methods)) {
                    if (typeof methodValue !== 'object' || methodValue === null) {
                        strippedMethods[method] = methodValue;
                        continue;
                    }

                    const op = { ...(methodValue as Record<string, unknown>) };
                    const responses = op.responses as Record<string, unknown> | undefined;

                    if (responses) {
                        const strippedResponses: Record<string, unknown> = {};
                        for (const [status, responseValue] of Object.entries(responses)) {
                            if (typeof responseValue !== 'object' || responseValue === null) {
                                strippedResponses[status] = responseValue;
                                continue;
                            }
                            const resp = responseValue as Record<string, unknown>;
                            const stripped: Record<string, unknown> = {};
                            if (resp.description) stripped.description = resp.description;
                            if (resp.content) {
                                // Keep standard `content` key but strip schemas — only MIME type keys remain
                                const strippedContent: Record<string, unknown> = {};
                                for (const mimeType of Object.keys(
                                    resp.content as Record<string, unknown>
                                )) {
                                    strippedContent[mimeType] = {};
                                }
                                stripped.content = strippedContent;
                            }
                            strippedResponses[status] = stripped;
                        }
                        op.responses = strippedResponses;
                    }

                    strippedMethods[method] = op;
                }

                filteredPaths[pathKey] = strippedMethods;
            }
        }

        // Build minimal spec (no components/schemas)
        const result: Record<string, unknown> = {
            openapi: api.openapi,
            info: api.info,
            paths: filteredPaths
        };

        // Include servers if present
        if (api.servers) {
            result.servers = api.servers;
        }

        // Dereferenced specs can have circular refs (e.g., Category.parent -> Category).
        // Use an ancestor-stack approach so shared references (same object appearing in
        // multiple endpoints) are duplicated in the output, while only true ancestor
        // cycles are replaced with "[Circular]".
        const ancestors: object[] = [];
        const json = JSON.stringify(
            result,
            function (_key, value) {
                if (typeof value === 'object' && value !== null) {
                    // Pop the stack back to the current parent object
                    while (ancestors.length > 0 && ancestors[ancestors.length - 1] !== this) {
                        ancestors.pop();
                    }
                    if (ancestors.includes(value)) return '[Circular]';
                    ancestors.push(value);
                }
                return value;
            },
            2
        );
        const outDir = path.resolve('src/generated');
        const outPath = path.join(outDir, 'spec.json');

        fs.mkdirSync(outDir, { recursive: true });
        fs.writeFileSync(outPath, json, 'utf-8');

        const pathCount = Object.keys(filteredPaths).length;
        const sizeKB = (Buffer.byteLength(json, 'utf-8') / 1024).toFixed(1);
        console.log(`[generate-spec] Wrote ${pathCount} paths (${sizeKB}KB) to ${outPath}`);
    } finally {
        // Clean up temp file if we fetched from URL
        const tempPath = path.resolve('.openapi-temp.json');
        if (fs.existsSync(tempPath)) {
            fs.unlinkSync(tempPath);
        }
    }
}

generateSpec().catch((err) => {
    console.error(`[generate-spec] Failed: ${err.message}`);
    process.exit(1);
});
