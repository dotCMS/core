#!/usr/bin/env node
/**
 * Profile the Vitest suite: where does the wall time actually go, per project.
 *
 * WHY THIS EXISTS, AND WHY IT IS NOT capture-baseline.sh
 *
 * `tools/capture-baseline.sh` answers "did we lose tests?" — counts only, no timings.
 * FR-017 / SC-008 ask for wall time before and after, and no tool in the repo produced
 * a number. Worse, the interesting number is not the total: on libs/data-access
 * (85 files, 853 tests) the first measurement taken with this script read
 *
 *   Duration 9.52s (transform 20.06s, setup 29.63s, import 88.76s, tests 2.80s, environment 12.90s)
 *
 * i.e. running the tests is 2.80s of ~151s of accumulated work — under 2%. Everything
 * else is per-file startup. A single "the suite takes N minutes" figure hides that, and
 * hiding it is how you end up tuning `pool` (measured: no help) instead of `isolate`
 * (measured: -42%).
 *
 * So this script reports the five-phase breakdown Vitest already prints, per project,
 * next to wall clock. It is a PARSER, not an instrument: every number here comes from
 * the `default` reporter's own summary, so it cannot drift from what Vitest reports.
 *
 * Usage:
 *   node tools/profile-tests.mjs --list
 *   node tools/profile-tests.mjs data-access ui
 *   node tools/profile-tests.mjs --top=6                 # the 6 projects with the most spec files
 *   node tools/profile-tests.mjs --all --json=profile.json
 *   node tools/profile-tests.mjs data-access --runs=3     # keeps the FASTEST run
 *   node tools/profile-tests.mjs data-access -- --no-isolate --pool=threads
 *
 * Anything after `--` is forwarded to Vitest verbatim, which is how the isolate/pool/
 * environment comparisons in the plan were taken without editing 46 config files.
 */

import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { join, resolve, relative } from 'node:path';

const CW = resolve(import.meta.dirname, '..');

/**
 * Discover projects by their generated Vitest config, not by nx.json.
 *
 * Same reasoning as the generator's own `projects()`: the plugin include lists and
 * project.json targets are not a complete or stable source, but a `vite.config.mts`
 * next to specs always is. libs/sdk/vue is hand-shaped rather than generated and is
 * deliberately still included — it runs tests, so it belongs in a profile.
 */
function discover() {
    const found = [];
    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            const child = `${rel}/${e.name}`;
            if (e.isDirectory()) walk(child);
            else if (e.name === 'vite.config.mts') {
                const src = readFileSync(join(CW, child), 'utf8');
                // Only configs that actually run tests. A vite.config.mts with no
                // `test` block is a build config (libs/edit-content-bridge ships one
                // for its standalone bundle) and profiling it would run zero files and
                // report a meaningless 0.
                if (!/\btest\s*:\s*\{/.test(src)) continue;
                found.push({
                    dir: rel,
                    config: child,
                    // The config's own test.name is the nx project name; the directory
                    // is not (libs/portlets/dot-analytics/portlet -> dot-analytics).
                    name: /name:\s*'([^']+)'/.exec(src)?.[1] ?? rel,
                    specs: countSpecs(rel)
                });
            }
        }
    };
    for (const top of ['libs', 'apps']) walk(top);
    return found.sort((a, b) => b.specs - a.specs);
}

/**
 * Spec files owned by this project, i.e. not by a nested project.
 *
 * The nesting is real and it matters for the ranking: libs/portlets holds 289 spec
 * files but every one of them belongs to a child project with its own config, so
 * attributing them to libs/portlets would put a project that runs nothing at the top
 * of the table.
 */
function countSpecs(dir, owners = null) {
    owners ??= discoveredDirs;
    let n = 0;
    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            const child = `${rel}/${e.name}`;
            if (e.isDirectory()) {
                if (owners && owners.has(child)) continue; // belongs to a nested project
                walk(child);
            } else if (/\.(spec|test)\.[cm]?[jt]sx?$/.test(e.name)) n++;
        }
    };
    walk(dir);
    return n;
}

/** Populated before countSpecs is first used; see the two-pass note in main(). */
let discoveredDirs = null;

/** `9.52s` / `812ms` / `1.20m` -> seconds. Vitest switches units on us. */
function seconds(text) {
    const m = /([\d.]+)\s*(ms|s|m)\b/.exec(text);
    if (!m) return null;
    const v = Number(m[1]);
    return m[2] === 'ms' ? v / 1000 : m[2] === 'm' ? v * 60 : v;
}

const stripAnsi = (s) => s.replace(/\[[0-9;]*m/g, '');

/**
 * Pull the summary out of a finished run.
 *
 * Reads the LAST match of each line, not the first: with `--reporter=default` a failing
 * run reprints the failed-file list above the summary, and an earlier version that took
 * the first `Test Files` match reported the failure header's counts instead of the
 * totals.
 */
function parse(output) {
    const text = stripAnsi(output);
    const last = (re) => {
        const all = [...text.matchAll(re)];
        return all.length ? all[all.length - 1] : null;
    };

    const files = last(/Test Files\s+(.+)/g)?.[1].trim() ?? null;
    const tests = last(/^\s*Tests\s+(.+)$/gm)?.[1].trim() ?? null;
    const dur = last(/Duration\s+([\d.]+\s*m?s)(?:\s*\(([^)]*)\))?/g);

    const phases = {};
    for (const p of (dur?.[2] ?? '').split(',')) {
        const m = /(\w+)\s+([\d.]+\s*m?s)/.exec(p);
        if (m) phases[m[1]] = seconds(m[2]);
    }

    return {
        files,
        tests,
        // Counts as numbers so a before/after diff can be asserted, not eyeballed.
        filesTotal: Number(/\((\d+)\)/.exec(files ?? '')?.[1] ?? NaN),
        testsTotal: Number(/\((\d+)\)/.exec(tests ?? '')?.[1] ?? NaN),
        failed: /failed/.test(files ?? '') || /failed/.test(tests ?? ''),
        duration: dur ? seconds(dur[1]) : null,
        phases
    };
}

function run(project, extra) {
    const args = ['exec', 'vitest', 'run', '--config', project.config, '--reporter=default', ...extra];
    const started = Date.now();
    const r = spawnSync('pnpm', args, {
        cwd: CW,
        encoding: 'utf8',
        maxBuffer: 64 * 1024 * 1024,
        // NX_NO_CLOUD keeps a cloud-less workspace from stalling on a token lookup;
        // vitest is invoked directly here so Nx's cache is bypassed by construction,
        // which is the point — a cache hit would report a run that never happened.
        env: { ...process.env, NX_NO_CLOUD: 'true' }
    });
    const wall = (Date.now() - started) / 1000;
    const out = `${r.stdout ?? ''}${r.stderr ?? ''}`;
    return { ...parse(out), wall, exitCode: r.status, output: out };
}

function table(rows) {
    const cols = [
        ['project', (r) => r.project],
        ['specs', (r) => String(r.specs)],
        ['wall', (r) => fmt(r.wall)],
        ['duration', (r) => fmt(r.duration)],
        ['transform', (r) => fmt(r.phases.transform)],
        ['setup', (r) => fmt(r.phases.setup)],
        ['import', (r) => fmt(r.phases.import)],
        ['tests', (r) => fmt(r.phases.tests)],
        ['env', (r) => fmt(r.phases.environment)],
        ['result', (r) => (r.exitCode === 0 ? `${r.testsTotal || 0} ok` : `FAIL (${r.files ?? 'no summary'})`)]
    ];
    const head = cols.map(([h]) => h);
    const body = rows.map((r) => cols.map(([, get]) => get(r) ?? '-'));
    const width = head.map((h, i) => Math.max(h.length, ...body.map((b) => b[i].length)));
    const line = (cells) => `| ${cells.map((c, i) => c.padEnd(width[i])).join(' | ')} |`;
    return [line(head), `|${width.map((w) => '-'.repeat(w + 2)).join('|')}|`, ...body.map(line)].join('\n');
}

const fmt = (s) => (s == null ? '-' : `${s.toFixed(2)}s`);

function main() {
    const argv = process.argv.slice(2);
    const sep = argv.indexOf('--');
    const extra = sep === -1 ? [] : argv.slice(sep + 1);
    const mine = sep === -1 ? argv : argv.slice(0, sep);

    const flag = (name) => mine.find((a) => a === `--${name}` || a.startsWith(`--${name}=`));
    const value = (name, dflt) => {
        const f = flag(name);
        if (!f) return dflt;
        const v = f.includes('=') ? f.split('=').slice(1).join('=') : null;
        return v ?? dflt;
    };

    // Two passes: discover() needs the set of project directories to attribute spec
    // files correctly, and countSpecs needs discover()'s result. Cheapest fix is to
    // collect the directories first from the same walk.
    discoveredDirs = new Set();
    const walk = (rel) => {
        for (const e of readdirSync(join(CW, rel), { withFileTypes: true })) {
            if (e.name === 'node_modules' || e.name.startsWith('.')) continue;
            const child = `${rel}/${e.name}`;
            if (e.isDirectory()) walk(child);
            else if (e.name === 'vite.config.mts' && /\btest\s*:\s*\{/.test(readFileSync(join(CW, child), 'utf8'))) {
                discoveredDirs.add(rel);
            }
        }
    };
    for (const top of ['libs', 'apps']) walk(top);

    const all = discover();

    if (flag('list')) {
        for (const p of all) console.log(`${String(p.specs).padStart(4)} specs  ${p.name.padEnd(34)} ${p.dir}`);
        console.log(`\n${all.length} projects, ${all.reduce((n, p) => n + p.specs, 0)} spec files`);
        return;
    }

    const top = Number(value('top', 0));
    const names = mine.filter((a) => !a.startsWith('--'));
    let selected;
    if (flag('all')) selected = all;
    else if (top) selected = all.slice(0, top);
    else if (names.length) {
        selected = names.map((n) => {
            const hit = all.find((p) => p.name === n || p.dir === n || p.dir.endsWith(`/${n}`));
            if (!hit) {
                console.error(`Unknown project '${n}'. Try --list.`);
                process.exit(2);
            }
            return hit;
        });
    } else {
        console.error('Nothing selected. Pass project names, --top=N, --all, or --list.');
        process.exit(2);
    }

    const runs = Math.max(1, Number(value('runs', 1)));
    const rows = [];
    for (const p of selected) {
        // Keep the FASTEST of N rather than the mean: the slow outliers here are cold
        // Vite caches and machine noise, and a mean lets one of those swamp the signal
        // we are trying to read. A regression still shows up, because it moves the floor.
        let best = null;
        for (let i = 0; i < runs; i++) {
            process.stderr.write(`profiling ${p.name}${runs > 1 ? ` (${i + 1}/${runs})` : ''}...\n`);
            const r = run(p, extra);
            if (r.exitCode !== 0 && best === null) best = r;
            else if (r.exitCode === 0 && (best === null || best.exitCode !== 0 || r.wall < best.wall)) best = r;
        }
        if (best.exitCode !== 0) {
            process.stderr.write(`  FAILED — last 20 lines:\n${stripAnsi(best.output).trimEnd().split('\n').slice(-20).join('\n')}\n`);
        }
        rows.push({ project: p.name, dir: p.dir, specs: p.specs, ...best });
    }

    console.log(`\n${table(rows)}`);

    const totals = rows.reduce(
        (a, r) => ({
            wall: a.wall + (r.wall ?? 0),
            tests: a.tests + (r.phases.tests ?? 0),
            files: a.files + (r.filesTotal || 0),
            cases: a.cases + (r.testsTotal || 0)
        }),
        { wall: 0, tests: 0, files: 0, cases: 0 }
    );
    console.log(
        `\n${rows.length} project(s): ${totals.files} files, ${totals.cases} tests, ${fmt(totals.wall)} wall, ` +
            `of which ${fmt(totals.tests)} is test execution` +
            (totals.wall > 0 ? ` (${((totals.tests / totals.wall) * 100).toFixed(1)}%)` : '')
    );
    if (extra.length) console.log(`vitest flags: ${extra.join(' ')}`);

    const jsonPath = value('json', flag('json') ? 'profile.json' : null);
    if (jsonPath) {
        // `output` is dropped: it is megabytes of reporter text per project and the
        // point of the file is to be diffable against a later run.
        const payload = {
            capturedAt: new Date().toISOString(),
            vitestFlags: extra,
            projects: rows.map(({ output, ...keep }) => keep)
        };
        writeFileSync(resolve(CW, jsonPath), `${JSON.stringify(payload, null, 2)}\n`);
        console.log(`\nwrote ${relative(CW, resolve(CW, jsonPath))}`);
    }

    // Non-zero if any project failed, so this is usable as a gate and not only as a report.
    if (rows.some((r) => r.exitCode !== 0)) process.exit(1);
}

if (!existsSync(join(CW, 'nx.json'))) {
    console.error(`Expected to run from core-web (looked for ${join(CW, 'nx.json')}).`);
    process.exit(2);
}
main();
