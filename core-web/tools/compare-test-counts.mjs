#!/usr/bin/env node
/**
 * Test-count parity checker — FR-003a.
 *
 * Compares executed/skipped test counts, per project, against a baseline captured
 * before the migration. Reads JUnit XML rather than runner stdout so the same
 * comparison works across both runners.
 *
 * Usage:
 *   node tools/compare-test-counts.mjs --capture --project <name> --report <path> --out <baseline.json>
 *   node tools/compare-test-counts.mjs --baseline <baseline.json> --project <name> --report <path>
 *   node tools/compare-test-counts.mjs --baseline <baseline.json> --summary
 *
 * Exit codes:
 *   0  parity holds (or capture succeeded)
 *   1  parity broken — a project lost tests, or gained an undeclared skip
 *   2  invocation / input error, including a missing or empty report
 *
 * WHY THIS FAILS LOUDLY ON AN EMPTY REPORT (contracts/ci-artifacts.md):
 * If the report is missing or reports zero tests, this must NOT conclude "0 == 0,
 * parity holds". That would let a migration that silently stopped running tests
 * pass the one check meant to catch exactly that. Absent evidence is not evidence
 * of parity.
 *
 * WHY THIS IS PER-PROJECT RATHER THAN ONE SHARED REPORT — measured, not assumed:
 * Every project's jest-junit config writes the SAME file
 * (core-web/target/core-web-reports/TEST-results.xml), so under `nx run-many` the
 * last writer wins. Verified: running `utils` (111 tests) and `portlets-dot-usage`
 * (21 tests) together leaves a report containing 21 — utils vanished silently.
 *
 * Two consequences. First, per-project counts cannot come from one shared file, so
 * the caller runs each project separately and points --report at that project's own
 * output. Second, this is a PRE-EXISTING CI defect: today's TEST-results.xml already
 * describes one project out of 41, and whatever consumes it has been reading a
 * fraction of the suite. The migration did not cause it. See research.md R-12.
 *
 * Note also that <testsuite name> is the top-level describe() block, NOT the Jest
 * displayName and not the project — so suite names cannot identify a project either.
 * Project identity comes from --project, supplied by the caller.
 */

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

const DEFAULT_REPORT = 'target/core-web-reports/TEST-results.xml';

function parseArgs(argv) {
    const args = { report: DEFAULT_REPORT };
    for (let i = 2; i < argv.length; i++) {
        const a = argv[i];
        if (a === '--capture') args.capture = true;
        else if (a === '--out') args.out = argv[++i];
        else if (a === '--baseline') args.baseline = argv[++i];
        else if (a === '--report') args.report = argv[++i];
        else if (a === '--project') args.project = argv[++i];
        else if (a === '--after') args.after = argv[++i];
        else if (a === '--summary') args.summary = true;
        else if (a === '--allow-empty') args.allowEmpty = true;
        else if (a === '--json') args.json = true;
        else fail(2, `unknown argument: ${a}`);
    }
    return args;
}

function fail(code, message) {
    console.error(`compare-test-counts: ${message}`);
    process.exit(code);
}

/**
 * Minimal JUnit reader. Deliberately not a full XML parser: we only need the
 * attributes on <testsuite> elements, and adding a dependency to a check that
 * gates the whole migration is a poor trade.
 *
 * `name` here is the top-level describe() block — measured, e.g. the report for
 * `portlets-dot-usage` names its suite "DotUsageShellComponent". It is neither the
 * project nor the Jest displayName, so it is summed, never used as a key.
 */
function readSuites(xml) {
    const suites = [];
    const re = /<testsuite\b([^>]*)>/g;
    let m;
    while ((m = re.exec(xml)) !== null) {
        const attrs = m[1];
        const get = (k) => {
            const v = new RegExp(`\\b${k}="([^"]*)"`).exec(attrs);
            return v ? v[1] : null;
        };
        const name = get('name');
        if (name === null) continue;
        suites.push({
            name,
            tests: Number(get('tests') ?? 0),
            skipped: Number(get('skipped') ?? 0),
            failures: Number(get('failures') ?? 0),
            errors: Number(get('errors') ?? 0)
        });
    }
    return suites;
}

/**
 * Roll every suite in one project's report into a single total. Suite names are
 * describe() blocks, so they are summed rather than used as keys — the project
 * identity comes from the caller.
 */
function aggregate(suites) {
    return suites.reduce(
        (acc, s) => ({
            tests: acc.tests + s.tests,
            skipped: acc.skipped + s.skipped,
            failures: acc.failures + s.failures,
            errors: acc.errors + s.errors,
            suites: acc.suites + 1
        }),
        { tests: 0, skipped: 0, failures: 0, errors: 0, suites: 0 }
    );
}

function loadReport(reportPath, { allowEmpty = false } = {}) {
    const abs = resolve(process.cwd(), reportPath);
    if (!existsSync(abs)) {
        fail(2, `report not found at ${abs}\n` +
                `  Parity cannot be established without it, and reporting "0 == 0" here\n` +
                `  would defeat the purpose of the check. See contracts/ci-artifacts.md.`);
    }
    const counts = aggregate(readSuites(readFileSync(abs, 'utf8')));
    if (counts.tests === 0 && !allowEmpty) {
        fail(2, `report at ${abs} contains zero tests.\n` +
                `  A well-formed but empty report is the exact silent failure this check guards\n` +
                `  against; treating it as parity would hide a suite that stopped running.\n` +
                `  Pass --allow-empty only for a project genuinely known to have no specs.`);
    }
    return counts;
}

/** Baselines are merged incrementally so a 41-project capture survives interruption. */
function loadBaseline(path) {
    const abs = resolve(process.cwd(), path);
    if (!existsSync(abs)) return { capturedAt: null, nodeVersion: process.version, projects: {} };
    return JSON.parse(readFileSync(abs, 'utf8'));
}

function capture(args) {
    if (!args.out) fail(2, '--capture requires --out <baseline.json>');
    if (!args.project) fail(2, '--capture requires --project <name>: suite names are describe() blocks, not projects');
    const counts = loadReport(args.report, { allowEmpty: args.allowEmpty });

    const baseline = loadBaseline(args.out);
    baseline.capturedAt = baseline.capturedAt ?? new Date().toISOString();
    baseline.nodeVersion = process.version;
    baseline.projects[args.project] = counts;

    writeFileSync(resolve(process.cwd(), args.out), JSON.stringify(baseline, null, 2) + '\n');
    const n = Object.keys(baseline.projects).length;
    const total = Object.values(baseline.projects).reduce((a, c) => a + c.tests, 0);
    console.log(`${args.project}: ${counts.tests} tests (${counts.skipped} skipped) — baseline now ${n} projects, ${total} tests`);
}

function verdict(name, b, a) {
    if (!b) return { name, status: 'NEW', before: null, after: a, broken: false };
    if (!a) {
        // A project that vanished is the failure mode most easily mistaken for
        // success: nothing fails, because nothing ran.
        return { name, status: 'MISSING', before: b, after: null, broken: true };
    }
    const executedBefore = b.tests - b.skipped;
    const executedAfter = a.tests - a.skipped;
    const newSkips = a.skipped - b.skipped;
    if (executedAfter < executedBefore) {
        return { name, status: 'LOST', before: b, after: a, delta: executedAfter - executedBefore, broken: true };
    }
    if (newSkips > 0) return { name, status: 'NEW_SKIPS', before: b, after: a, newSkips, broken: true };
    return { name, status: 'OK', before: b, after: a, broken: false };
}

function compare(args) {
    const baselinePath = resolve(process.cwd(), args.baseline);
    if (!existsSync(baselinePath)) fail(2, `baseline not found at ${baselinePath}`);
    const before = (JSON.parse(readFileSync(baselinePath, 'utf8')).projects) ?? {};

    let rows;
    if (args.summary) {
        // Report-wide view over an already-populated after-baseline.
        if (!args.after) fail(2, '--summary requires --after <after-baseline.json>');
        const after = (JSON.parse(readFileSync(resolve(process.cwd(), args.after), 'utf8')).projects) ?? {};
        const names = [...new Set([...Object.keys(before), ...Object.keys(after)])].sort();
        rows = names.map((n) => verdict(n, before[n], after[n]));
    } else {
        if (!args.project) fail(2, 'comparing needs --project <name> (or --summary --after <file>)');
        const a = loadReport(args.report, { allowEmpty: args.allowEmpty });
        rows = [verdict(args.project, before[args.project], a)];
    }
    const broken = rows.some((r) => r.broken);

    if (args.json) {
        console.log(JSON.stringify({ broken, rows }, null, 2));
    } else {
        const pad = (s, n) => String(s).padEnd(n);
        console.log(pad('PROJECT', 44) + pad('STATUS', 12) + pad('BEFORE', 12) + 'AFTER');
        for (const r of rows) {
            const b = r.before ? `${r.before.tests - r.before.skipped}/${r.before.tests}` : '—';
            const a = r.after ? `${r.after.tests - r.after.skipped}/${r.after.tests}` : '—';
            console.log(pad(r.name, 44) + pad(r.status, 12) + pad(b, 12) + a);
        }
        console.log('\n(executed/total — "executed" excludes skipped)');
    }

    if (broken) {
        console.error('\nParity BROKEN. Every LOST/MISSING/NEW_SKIPS row above needs either a fix or,');
        console.error('for a genuinely unmigratable test, an entry in the FR-004 justification table');
        console.error('with a linked issue. An undeclared skip fails acceptance.');
        process.exit(1);
    }
    console.log('\nParity holds.');
}

const args = parseArgs(process.argv);
if (args.capture) capture(args);
else if (args.baseline) compare(args);
else fail(2, 'specify either --capture --project <name> --out <file>, or --baseline <file>');
