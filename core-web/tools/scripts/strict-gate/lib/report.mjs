/**
 * Assembles the run report. Shape is pinned by contracts/report.schema.json so the follow-up task
 * inherits a stable interface rather than whatever this implementation happened to emit.
 */

const key = (f) => `${f.file}|${f.line}|${f.code}`;

/**
 * A diagnostic reported under two configurations is ONE defect. Real case: src/utils/index.ts in
 * sdk-create-app reports TS7030 under both the lib and the spec configuration.
 */
export function dedupe(findings) {
    const seen = new Map();
    for (const finding of findings) if (!seen.has(key(finding))) seen.set(key(finding), finding);
    return [...seen.values()];
}

export function buildReport({
    base,
    head,
    flagSet,
    granularity,
    targets = [],
    unmapped = [],
    findings = [],
    discarded = {
        byOrigin: { dependency: 0, untouched: 0, infrastructure: 0 },
        byLayer: { source: 0, template: 0 }
    },
    durationMs = { total: 0, typescript: 0, templateAware: 0 }
}) {
    const unique = dedupe(findings);
    return {
        base,
        head,
        flagSet,
        granularity,
        targets: targets.map((t) => ({
            project: t.project,
            root: t.root,
            configPath: t.configPath,
            mode: t.mode ?? 'typescript',
            files: t.files
        })),
        unmapped: unmapped.map((u) => ({ path: u.path, reason: u.reason })),
        findings: unique.map((f) => ({
            file: f.file,
            line: f.line,
            column: f.column,
            code: f.code,
            message: f.message,
            origin: f.origin ?? 'changed',
            layer: f.layer ?? 'source'
        })),
        discarded,
        durationMs,
        // The gate's whole output in one integer. Exit 2 (harness failure) is assigned by run.mjs
        // and never conflated with 1 — a broken harness reporting "clean" is the one failure mode
        // that would quietly defeat the gate.
        exitCode: unique.length > 0 ? 1 : 0
    };
}
