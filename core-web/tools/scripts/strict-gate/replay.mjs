#!/usr/bin/env node
/**
 * Replays merged pull requests through the gate — the spike's evidence engine.
 */
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { run, git } from './lib/exec.mjs';
import { workspaceRoot } from './lib/resolve-tools.mjs';
import { runGate, collectDiagnostics, reportFrom } from './run.mjs';
import { CORPUS, adjudicate, summarize } from './corpus.mjs';

const DEFAULT_REPO_DIR = path.resolve(workspaceRoot, '..');

async function nameWithOwner(repoDir) {
    const { stdout } = await git(['-C', repoDir, 'remote', 'get-url', 'origin']);
    const match = stdout.trim().match(/[:/]([^/:]+\/[^/]+?)(?:\.git)?$/);
    if (!match) throw new Error(`cannot derive owner/repo from origin '${stdout.trim()}'`);
    return match[1];
}

/**
 * Base is the merge commit's FIRST PARENT: `main` exactly as it stood before the merge, so
 * `M^1..M` is precisely what this pull request added. No merge-base computation, and no
 * dependency on a branch that was deleted after merge.
 */
export async function resolveMergeRange({ repoDir = DEFAULT_REPO_DIR, pr }) {
    const repo = await nameWithOwner(repoDir);
    const { stdout } = await run('gh', [
        'pr', 'view', String(pr), '--repo', repo, '--json', 'mergeCommit,state', '--jq',
        '"\\(.state) \\(.mergeCommit.oid // "none")"'
    ]);
    const [state, oid] = stdout.trim().replace(/^"|"$/g, '').split(' ');
    if (state !== 'MERGED' || !oid || oid === 'none') {
        throw new Error(`pull request #${pr} is ${state} with no merge commit — cannot replay it`);
    }

    const { stdout: parents } = await git(['-C', repoDir, 'rev-list', '--parents', '-n', '1', oid]);
    const [, firstParent] = parents.trim().split(' ');
    if (!firstParent) throw new Error(`commit ${oid} has no parent — cannot derive a base`);

    return { base: firstParent, head: oid };
}

function summaryTable(results) {
    const pad = (v, n) => String(v).padEnd(n);
    const lines = [
        '',
        `${pad('PR', 9)}${pad('expected', 10)}${pad('findings', 10)}${pad('discarded', 11)}${pad('ms', 8)}verdict`,
        '-'.repeat(60)
    ];
    for (const { sample, report, verdict } of results) {
        const discarded = report.discarded.byOrigin.dependency + report.discarded.byOrigin.untouched;
        lines.push(
            pad(`#${sample.pr}`, 9) +
                pad(sample.expectation, 10) +
                pad(report.findings.length, 10) +
                pad(discarded, 11) +
                pad(Math.round(report.durationMs.total), 8) +
                (verdict.matchedExpectation ? 'as predicted' : 'MISMATCH — adjudicate')
        );
    }
    const s = summarize(results.map(({ sample, verdict }) => ({ sample, verdict })));
    lines.push('');
    lines.push(`clean cases: ${s.cleanCases}   with findings: ${s.cleanCasesWithFindings}   ` +
        `false-positive rate: ${s.falsePositiveRate === null ? 'n/a' : s.falsePositiveRate}`);
    lines.push(`debt cases: ${s.debtCases}   detected: ${s.debtCasesDetected}   ` +
        `findings needing adjudication: ${s.unexpectedFindings}`);
    lines.push(s.caveat);
    return lines.join('\n');
}

const FLAG_SETS = ['strict', 'null-checks', 'strict-max'];
const GRANULARITIES = ['file', 'line'];

/**
 * Every combination of flag set and granularity over identical input (FR-007, FR-008).
 *
 * Compiles once per (pull request, flag set) and filters twice: granularity only affects the
 * filter, so paying for a second identical compilation would double the matrix's cost for nothing.
 */
async function runMatrix(cases) {
    const rows = [];
    for (const sample of cases) {
        const { base, head } = await resolveMergeRange({ pr: sample.pr });
        for (const flagSet of FLAG_SETS) {
            const collected = await collectDiagnostics({ base, head, flagSet });
            for (const granularity of GRANULARITIES) {
                const report = reportFrom(collected, { flagSet, granularity });
                rows.push({ pr: sample.pr, flagSet, granularity, report });
            }
        }
    }
    return rows;
}

function matrixTable(rows) {
    const prs = [...new Set(rows.map((r) => r.pr))];
    const out = ['', 'findings by flag set x granularity', ''];
    out.push(`${'PR'.padEnd(9)}${FLAG_SETS.map((f) => `${f}/file`.padEnd(16) + `${f}/line`.padEnd(16)).join('')}`);
    out.push('-'.repeat(9 + FLAG_SETS.length * 32));
    for (const pr of prs) {
        let line = `#${pr}`.padEnd(9);
        for (const flagSet of FLAG_SETS) {
            for (const granularity of GRANULARITIES) {
                const row = rows.find((r) => r.pr === pr && r.flagSet === flagSet && r.granularity === granularity);
                line += String(row?.report.findings.length ?? '-').padEnd(16);
            }
        }
        out.push(line);
    }

    out.push('');
    out.push('totals');
    for (const flagSet of FLAG_SETS) {
        for (const granularity of GRANULARITIES) {
            const subset = rows.filter((r) => r.flagSet === flagSet && r.granularity === granularity);
            const findings = subset.reduce((n, r) => n + r.report.findings.length, 0);
            const ms = Math.round(subset.reduce((n, r) => n + r.report.durationMs.total, 0) / subset.length);
            out.push(`  ${(flagSet + '/' + granularity).padEnd(22)}${String(findings).padStart(4)} findings   ${String(ms).padStart(6)} ms avg`);
        }
    }

    // The adoption cost, which is the whole granularity argument: how much pre-existing debt does
    // whole-file make an author inherit for touching the file at all?
    const inherited = FLAG_SETS.map((flagSet) => {
        const f = rows.filter((r) => r.flagSet === flagSet && r.granularity === 'file')
            .reduce((n, r) => n + r.report.findings.length, 0);
        const l = rows.filter((r) => r.flagSet === flagSet && r.granularity === 'line')
            .reduce((n, r) => n + r.report.findings.length, 0);
        return `  ${flagSet.padEnd(22)}${String(f - l).padStart(4)} extra findings inherited from untouched lines (${f} vs ${l})`;
    });
    out.push('', 'whole-file adoption cost', ...inherited);
    return out.join('\n');
}

async function main(argv) {
    const prs = [];
    const passthrough = {};
    for (let i = 0; i < argv.length; i += 1) {
        const [flag, inline] = argv[i].split('=');
        const value = inline ?? argv[i + 1];
        const consume = () => { if (inline === undefined) i += 1; };
        if (flag === '--pr') { prs.push(...value.split(',').map(Number)); consume(); }
        else if (flag === '--all') { /* run the whole corpus */ }
        else if (flag === '--matrix') { passthrough.matrix = true; }
        else if (flag === '--flags') { passthrough.flagSet = value; consume(); }
        else if (flag === '--granularity') { passthrough.granularity = value; consume(); }
        else if (flag === '--templates') { passthrough.templates = value === 'on'; consume(); }
        else if (flag === '--report') { consume(); }
        else throw new Error(`unknown option '${flag}' — see contracts/cli.md`);
    }
    const cases = prs.length > 0 ? prs.map((pr) => CORPUS.find((c) => c.pr === pr) ?? { pr, expectation: 'debt', rationale: 'ad-hoc', knownFindings: [] }) : CORPUS;

    if (passthrough.matrix) {
        process.stdout.write(`${matrixTable(await runMatrix(cases))}\n`);
        return 0;
    }

    const results = [];
    for (const sample of cases) {
        const { base, head } = await resolveMergeRange({ pr: sample.pr });
        const { matrix, ...gateOptions } = passthrough;
        const report = await runGate({ base, head, ...gateOptions });
        results.push({ sample, report, verdict: adjudicate(sample, report, report.granularity) });
    }

    process.stdout.write(`${summaryTable(results)}\n`);

    // A mismatch is information, not a defect: every report is still produced so the per-finding
    // adjudication SC-003 requires can proceed.
    return results.every((r) => r.verdict.matchedExpectation) ? 0 : 1;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    main(process.argv.slice(2)).then(
        (code) => process.exit(code),
        (error) => {
            process.stderr.write(`strict-gate replay: ${error.message}\n`);
            process.exit(2);
        }
    );
}
