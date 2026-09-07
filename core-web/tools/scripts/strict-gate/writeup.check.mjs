#!/usr/bin/env node
/**
 * T064 — completeness check for findings.md.
 *
 * User Story 5 delivers a written record, not code, so no unit or integration test applies to it.
 * Constitution Principle V allows that omission only as an explicit, recorded decision — and this
 * check is what stands in its place: it fails while any figure the spike promised is still absent,
 * so "the write-up is done" is a verifiable claim rather than an opinion.
 */
import fs from 'node:fs';
import path from 'node:path';
import { workspaceRoot } from './lib/resolve-tools.mjs';

const FINDINGS = path.join(
    workspaceRoot,
    '..',
    'specs/37401-diff-scoped-strict-typecheck-gate/findings.md'
);

/** Each requirement names what must be present and why the write-up is incomplete without it. */
const REQUIRED = [
    { id: 'FR-012', label: 'per-pull-request results', pattern: /#37264[\s\S]*#37415[\s\S]*#37372/ },
    { id: 'SC-003', label: 'per-finding adjudication', pattern: /\*\*real\*\*|verdict/i },
    { id: 'SC-002', label: 'false-positive measure', pattern: /false positive/i },
    { id: 'SC-004', label: 'discarded-diagnostic counts', pattern: /discard/i },
    { id: 'SC-007a', label: 'flag-set recommendation', pattern: /Decision 2 — Flag set/i },
    { id: 'SC-007b', label: 'granularity recommendation', pattern: /Decision 1 — Granularity/i },
    { id: 'SC-007c', label: 'runtime figure', pattern: /Decision 3 — Runtime/i },
    { id: 'SC-005', label: 'runtime against the 10s budget', pattern: /10s budget/i },
    { id: 'SC-006', label: 'edge cases exercised', pattern: /edge case/i },
    { id: 'SC-008', label: 'go / no-go on blocking merges', pattern: /go\s*\/\s*no-go on blocking/i },
    { id: 'SC-009', label: 'timebox outcome', pattern: /timebox/i },
    { id: 'SC-011', label: 'template strictness demonstrated', pattern: /SC-011/ },
    { id: 'SC-013', label: 'go / no-go on templates', pattern: /NO-GO|GO for day-one blocking/i },
    { id: 'FR-013', label: 'follow-up task or documented no-go', pattern: /follow-up/i }
];

export function checkWriteup(text) {
    const missing = REQUIRED.filter((r) => !r.pattern.test(text));
    return { complete: missing.length === 0, missing };
}

if (process.argv[1] === new URL(import.meta.url).pathname) {
    if (!fs.existsSync(FINDINGS)) {
        process.stderr.write(`writeup.check: ${FINDINGS} does not exist yet\n`);
        process.exit(1);
    }
    const { complete, missing } = checkWriteup(fs.readFileSync(FINDINGS, 'utf8'));
    if (complete) {
        process.stdout.write(`writeup.check: findings.md carries all ${REQUIRED.length} required figures\n`);
        process.exit(0);
    }
    process.stderr.write('writeup.check: findings.md is incomplete\n');
    for (const item of missing) process.stderr.write(`  missing ${item.id}: ${item.label}\n`);
    process.exit(1);
}
