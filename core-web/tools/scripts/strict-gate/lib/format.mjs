/**
 * Renders a run report for the people and the agents that have to act on it.
 *
 * Design constraint that drives everything here: the primary consumer is often a coding agent
 * reading CI output with no other context. It must be able to fix the failure from this text
 * alone, and — just as important — it must NOT overreach. An agent that does not know the gate
 * only counts changed lines will "helpfully" refactor an entire legacy file, producing a huge
 * diff nobody asked for. So the output states the scope rule as loudly as it states the errors.
 */

/** What to actually do about the codes this gate produces, by frequency in this workspace. */
const GUIDANCE = {
    TS4111: 'Access the property with bracket notation — `obj[\'KEY\']` instead of `obj.KEY`. It comes from an index signature.',
    TS7030: 'Not all code paths return a value. Add the missing `return`, or give the function an explicit `: void`.',
    TS7006: 'Parameter is implicitly `any`. Add an explicit type annotation.',
    TS18047: 'Value may be `null`. Narrow it first (`if (x)`), or use `?.` / `??`.',
    TS18048: 'Value may be `undefined`. Narrow it first, or use `?.` / `??`.',
    TS2345: 'Argument type does not match the parameter type. Fix the value, or widen/correct the signature.',
    TS2531: 'Object is possibly `null`. Narrow before use.',
    TS2532: 'Object is possibly `undefined`. Narrow before use.',
    TS7029: 'Switch case falls through. Add `break` / `return`, or mark it intentional.',
    TS4114: 'This member overrides a base member — add the `override` modifier.',
    TS2564: 'Property has no initializer and is not definitely assigned. Initialize it, or mark it `!`.'
};

const FLAG_SET_LABEL = {
    strict: "the repo's strict convention (`strict` + noPropertyAccessFromIndexSignature, noImplicitOverride, noImplicitReturns, noFallthroughCasesInSwitch) — the same settings tsconfig.base.json carries on the strict-mode branch",
    'null-checks': '`strictNullChecks` + `noImplicitAny` only',
    'strict-max': "the repo's strict convention plus noUncheckedIndexedAccess and exactOptionalPropertyTypes"
};

const scopeRule = (granularity) =>
    granularity === 'line'
        ? 'Only lines this pull request ADDED OR MODIFIED are checked. Pre-existing problems on untouched lines are deliberately ignored.'
        : 'Every line of a changed file is checked, including pre-existing problems on lines this pull request did not touch.';

/**
 * Diagnostics the gate deliberately did not report — dependency code plus untouched lines.
 * Infrastructure discards are excluded: they are not debt anyone is being forgiven, they are
 * diagnostics that were never strictness violations to begin with.
 */
const ignoredCount = (report) =>
    report.discarded.byOrigin.dependency + report.discarded.byOrigin.untouched;

function groupByFile(findings) {
    const byFile = new Map();
    for (const f of findings) {
        if (!byFile.has(f.file)) byFile.set(f.file, []);
        byFile.get(f.file).push(f);
    }
    for (const list of byFile.values()) list.sort((a, b) => a.line - b.line);
    return byFile;
}

/** Plain text — the default, and what an agent reading raw CI logs gets. */
export function formatText(report) {
    const lines = [];
    const total = ignoredCount(report);

    if (report.exitCode === 0) {
        lines.push('strict-gate: PASS — no new strict-mode violations in this diff.');
        lines.push('');
        lines.push(`  checked   ${report.targets.length} project config(s) under ${report.flagSet}`);
        lines.push(`  ignored   ${total} pre-existing/dependency diagnostic(s) outside this diff`);
        if (report.unmapped.length > 0) {
            lines.push(`  unmapped  ${report.unmapped.length} changed file(s) no project claimed (not a failure)`);
        }
        return lines.join('\n');
    }

    lines.push(`strict-gate: FAIL — ${report.findings.length} new strict-mode violation(s) introduced by this diff.`);
    lines.push('');
    lines.push('WHY THIS FAILS');
    lines.push('  main is not strict yet, so these files compile today. This gate checks the code');
    lines.push(`  THIS pull request writes against ${FLAG_SET_LABEL[report.flagSet] ?? report.flagSet},`);
    lines.push('  so new code stops adding to the debt the strict-mode migration has to clear.');
    lines.push('');
    lines.push('SCOPE — READ BEFORE FIXING');
    lines.push(`  ${scopeRule(report.granularity)}`);
    lines.push(`  ${total} diagnostic(s) from dependencies and untouched code were IGNORED on purpose.`);
    lines.push('  Fix ONLY the violations listed below. Do not refactor surrounding code, do not');
    lines.push('  "clean up" the rest of the file, and do not edit any tsconfig to silence this.');
    lines.push('');
    lines.push('VIOLATIONS');

    for (const [file, findings] of groupByFile(report.findings)) {
        lines.push('');
        lines.push(`  ${file}`);
        for (const f of findings) {
            lines.push(`    ${f.line}:${f.column}  ${f.code}  ${f.message}`);
            const hint = GUIDANCE[f.code];
            if (hint) lines.push(`             fix: ${hint}`);
        }
    }

    lines.push('');
    lines.push('REPRODUCE LOCALLY');
    lines.push('  cd core-web');
    lines.push(
        `  node tools/scripts/strict-gate/run.mjs --base origin/main --head HEAD ` +
            `--flags ${report.flagSet} --granularity ${report.granularity}`
    );
    return lines.join('\n');
}

/** GitHub Actions annotations — puts each violation inline on the pull request diff. */
export function formatGithub(report) {
    return report.findings
        .map((f) => {
            const hint = GUIDANCE[f.code] ? ` — ${GUIDANCE[f.code]}` : '';
            const message = `${f.code}: ${f.message}${hint}`.replace(/\r?\n/g, ' ');
            return `::error file=${f.file},line=${f.line},col=${f.column},title=strict-gate ${f.code}::${message}`;
        })
        .join('\n');
}

/**
 * What the run cost, paired with the diff size that produced it.
 *
 * Emitted on every markdown-formatted run, passing ones included. Note where it does NOT appear:
 * `formatGithub` — the format this repository's CI invocation passes — never calls this, so the
 * cost line reaches the run page only through `run.mjs`'s separate `GITHUB_STEP_SUMMARY` write,
 * never stdout and never an annotation. Anyone reading the Maven log will not see it.
 *
 * The gate blocks, so its cost sits on the critical path of every frontend merge and the real
 * distribution is worth knowing; a duration printed only when there are findings would sample the
 * fast and slow cases unevenly, and it is the tail that matters. The diff size travels with it
 * because a duration alone is not comparable between a one-file pull request and a forty-file one.
 *
 * One decimal place: the no-op case costs ~0.3s and rounding it to "0s" would make the cheapest
 * and most common outcome invisible in the evidence.
 */
function costLine(report) {
    // Guarded because this file's whole stance is that a plausible-looking wrong number is the
    // dangerous failure mode, and `formatMarkdown` is exported: a partial `durationMs` object
    // slips past buildReport's default (which only fires on undefined) and renders "NaNs".
    const ms = report.durationMs?.total;
    // Sub-second runs render in milliseconds. `toFixed(1)` turns anything under 50ms into "0.0s",
    // and the cheapest case — a pull request touching no frontend file at all — measures ~175ms,
    // within a factor of four of that cliff. Rounding the most common outcome to zero would make
    // it invisible in exactly the evidence this line exists to provide.
    const elapsed = !Number.isFinite(ms) ? '?s' : ms < 1000 ? `${Math.round(ms)}ms` : `${(ms / 1000).toFixed(1)}s`;
    // Distinct paths, not (config -> file) assignments. `selectConfigs` claims a source under
    // EVERY eligible config, so a project whose lib and spec configs both include a file yields
    // two target entries for one changed file; summing `files.length` would report a one-file
    // diff as two. Unmapped files count as well: they were changed, they just were not examined,
    // and leaving them out understates the diff this duration came from.
    const changed = new Set([
        ...report.targets.flatMap((t) => t.files),
        ...report.unmapped.map((u) => u.path)
    ]).size;
    // Deliberately not "Checked": `changed` includes unmapped files, which by definition were not
    // examined, and the note below says so. Two adjacent lines asserting opposite things about the
    // same file is worse than a plainer verb.
    return `_${changed} changed file(s) across ${report.targets.length} project config(s), ${elapsed}._`;
}

/**
 * Changed files no project claimed. They were NOT examined, and with the gate blocking, silence
 * about them reads as a clean pass — the edge case the spec calls out by name. Naming them is not
 * a failure signal; it is the difference between "nothing was wrong" and "nothing was looked at".
 */
function unmappedNote(report) {
    if (report.unmapped.length === 0) return null;
    const rows = report.unmapped.map((u) => `- \`${u.path}\` — ${u.reason}`).join('\n');
    return [
        `**${report.unmapped.length} changed file(s) were not examined** — no project configuration claims them:`,
        '',
        rows
    ].join('\n');
}

/** Markdown for the job summary — what a human opening the run sees first. */
export function formatMarkdown(report) {
    const total = ignoredCount(report);
    // `costLine` already carries the project-config count, and it carries the file count too, so
    // it is the more informative of the two places that used to state it.
    const note = unmappedNote(report);

    if (report.exitCode === 0) {
        return [
            '## ✅ strict-gate: pass',
            '',
            `No new strict-mode violations. ${total} pre-existing or dependency diagnostic(s) ignored.`,
            '',
            costLine(report),
            ...(note ? ['', note] : [])
        ].join('\n');
    }

    const rows = report.findings
        .map((f) => `| \`${f.file}\` | ${f.line}:${f.column} | \`${f.code}\` | ${f.message.replace(/\|/g, '\\|')} |`)
        .join('\n');

    return [
        `## ❌ strict-gate: ${report.findings.length} new strict-mode violation(s)`,
        '',
        `**Scope.** ${scopeRule(report.granularity)} ${total} diagnostic(s) from dependencies and untouched code were ignored — fix only what is listed.`,
        '',
        costLine(report),
        ...(note ? ['', note] : []),
        '',
        '| File | Line | Code | Message |',
        '|---|---|---|---|',
        rows,
        '',
        '<details><summary>Reproduce locally</summary>',
        '',
        '```bash',
        'cd core-web',
        `node tools/scripts/strict-gate/run.mjs --base origin/main --head HEAD --flags ${report.flagSet} --granularity ${report.granularity}`,
        '```',
        '',
        '</details>'
    ].join('\n');
}

export const FORMATTERS = {
    text: formatText,
    github: formatGithub,
    markdown: formatMarkdown,
    json: (report) => JSON.stringify(report, null, 2)
};
