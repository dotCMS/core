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
    const total = report.discarded.byOrigin.dependency + report.discarded.byOrigin.untouched;

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
    lines.push(`  main is not strict yet, so these files compile today. This gate checks the code`);
    lines.push(`  THIS pull request writes against ${FLAG_SET_LABEL[report.flagSet] ?? report.flagSet},`);
    lines.push(`  so new code stops adding to the debt the strict-mode migration has to clear.`);
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

/** Markdown for the job summary — what a human opening the run sees first. */
export function formatMarkdown(report) {
    const total = report.discarded.byOrigin.dependency + report.discarded.byOrigin.untouched;
    if (report.exitCode === 0) {
        return [
            '## ✅ strict-gate: pass',
            '',
            `No new strict-mode violations. ${total} pre-existing or dependency diagnostic(s) ignored, ` +
                `across ${report.targets.length} project config(s).`
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
