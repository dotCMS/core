/**
 * Validates a run report against contracts/report.schema.json.
 *
 * Hand-rolled rather than pulled from a library because the harness adds no dependency, and the
 * schema uses a small, closed subset of JSON Schema. It covers exactly that subset and throws on
 * anything it does not understand — a validator that silently ignores a keyword it cannot handle
 * would report "valid" for a report it never actually checked.
 */
import fs from 'node:fs';
import path from 'node:path';
import { workspaceRoot } from './resolve-tools.mjs';

export const SCHEMA_PATH = path.join(
    workspaceRoot,
    '..',
    'specs/37401-diff-scoped-strict-typecheck-gate/contracts/report.schema.json'
);

const SUPPORTED = new Set([
    '$schema', '$id', 'title', 'description', '$defs',
    'type', 'enum', 'const', 'properties', 'required', 'additionalProperties',
    'items', 'pattern', 'minimum', 'maximum', 'minItems', '$ref'
]);

function typeOf(value) {
    if (value === null) return 'null';
    if (Array.isArray(value)) return 'array';
    if (Number.isInteger(value)) return 'integer';
    return typeof value;
}

function resolveRef(ref, root) {
    if (!ref.startsWith('#/')) throw new Error(`Unsupported $ref: ${ref}`);
    return ref
        .slice(2)
        .split('/')
        .reduce((node, key) => {
            if (node === undefined) throw new Error(`Unresolvable $ref: ${ref}`);
            return node[key];
        }, root);
}

function check(value, schema, root, at, errors) {
    for (const keyword of Object.keys(schema)) {
        if (!SUPPORTED.has(keyword)) {
            throw new Error(`validate-report does not implement JSON Schema keyword '${keyword}'`);
        }
    }

    if (schema.$ref) {
        check(value, resolveRef(schema.$ref, root), root, at, errors);
        return;
    }

    if (schema.type) {
        const actual = typeOf(value);
        const ok = schema.type === 'number' ? actual === 'number' || actual === 'integer' : actual === schema.type;
        if (!ok) {
            errors.push(`${at}: expected ${schema.type}, got ${actual}`);
            return;
        }
    }

    if (schema.enum && !schema.enum.includes(value)) {
        errors.push(`${at}: ${JSON.stringify(value)} is not one of ${JSON.stringify(schema.enum)}`);
    }
    if (schema.const !== undefined && value !== schema.const) {
        errors.push(`${at}: expected ${JSON.stringify(schema.const)}`);
    }
    if (schema.pattern && typeof value === 'string' && !new RegExp(schema.pattern).test(value)) {
        errors.push(`${at}: ${JSON.stringify(value)} does not match /${schema.pattern}/`);
    }
    if (schema.minimum !== undefined && typeof value === 'number' && value < schema.minimum) {
        errors.push(`${at}: ${value} < minimum ${schema.minimum}`);
    }
    if (schema.maximum !== undefined && typeof value === 'number' && value > schema.maximum) {
        errors.push(`${at}: ${value} > maximum ${schema.maximum}`);
    }

    if (typeOf(value) === 'array') {
        if (schema.minItems !== undefined && value.length < schema.minItems) {
            errors.push(`${at}: expected at least ${schema.minItems} items`);
        }
        if (schema.items) {
            value.forEach((item, i) => check(item, schema.items, root, `${at}[${i}]`, errors));
        }
    }

    if (typeOf(value) === 'object') {
        for (const key of schema.required ?? []) {
            if (!(key in value)) errors.push(`${at}: missing required property '${key}'`);
        }
        if (schema.additionalProperties === false && schema.properties) {
            for (const key of Object.keys(value)) {
                if (!(key in schema.properties)) {
                    errors.push(`${at}: unexpected property '${key}'`);
                }
            }
        }
        for (const [key, sub] of Object.entries(schema.properties ?? {})) {
            if (key in value) check(value[key], sub, root, `${at}.${key}`, errors);
        }
    }
}

/**
 * @param {unknown} report
 * @param {object} [schema] Defaults to the published contract.
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validateReport(report, schema = loadSchema()) {
    const errors = [];
    check(report, schema, schema, 'report', errors);

    // Invariants the schema alone cannot express (data-model.md, RunReport).
    if (errors.length === 0) {
        const failing = (report.findings?.length ?? 0) > 0;
        if (failing && report.exitCode === 0) {
            errors.push('report.exitCode: must be non-zero when findings is non-empty');
        }
        if (!failing && report.exitCode !== 0) {
            errors.push('report.exitCode: must be 0 when findings is empty');
        }
        for (const [i, finding] of (report.findings ?? []).entries()) {
            if (finding.origin !== 'changed') {
                errors.push(`report.findings[${i}].origin: survivors must be 'changed'`);
            }
        }
    }

    return { valid: errors.length === 0, errors };
}

export function loadSchema() {
    return JSON.parse(fs.readFileSync(SCHEMA_PATH, 'utf8'));
}
