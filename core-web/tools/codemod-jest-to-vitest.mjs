#!/usr/bin/env node
/**
 * Jest → Vitest mechanical rewrites — FR-013.
 *
 * Committed and retained on purpose (FR-001a): it is what lets a reviewer verify
 * ~977 rewritten spec files by reading one transformation instead of a thousand
 * diffs. Anything this cannot do is a hand-edit, and hand-edits must be enumerated
 * in the PR description (FR-012, FR-013).
 *
 * Usage:
 *   node tools/codemod-jest-to-vitest.mjs <path> [<path> ...]   # rewrite in place
 *   node tools/codemod-jest-to-vitest.mjs --dry-run <path>       # report only
 *   node tools/codemod-jest-to-vitest.mjs --report <path>        # per-rule counts
 *
 * SCOPE DISCIPLINE: this only ever touches *.spec.* / *.test.* files plus two narrow
 * test-infrastructure directories (utils-testing sources, __mocks__).
 * It refuses to write to anything else. A codemod with a wide blast radius is
 * exactly how a "tests only" change stops being tests only, and the FR-001 check
 * would catch that late — better to make it impossible here.
 *
 * WHAT IT DELIBERATELY DOES NOT DO:
 *   - `jest.mock` factory hoisting differences. Vitest hoists `vi.mock` like Jest,
 *     but a factory closing over an outer `const` behaves differently. Detecting
 *     that reliably needs real scope analysis; instead such files are FLAGGED for
 *     a human rather than silently rewritten into something subtly wrong.
 *   - `jest.requireActual` → `vi.importActual`: the latter is async, so the call
 *     site has to become async too. Flagged, never auto-converted.
 *   - Type-only constructs beyond the simple `jest.Mock` forms below.
 */

import { readFileSync, writeFileSync, statSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const TEST_FILE = /\.(spec|test)\.(ts|tsx)$/;

/**
 * Files that are test infrastructure without being spec files. The spike proved the
 * scope discipline above was too tight: `libs/utils-testing` is imported by specs
 * workspace-wide and is full of `jest.fn()`, so skipping it left the migration unable
 * to compile. Still narrow — a directory allowlist, not "any .ts".
 */
const TEST_SUPPORT = /(^|\/)(utils-testing\/src\/.*|__mocks__\/[^/]+|test-setup)\.tsx?$/;

/**
 * Straight textual substitutions that are safe regardless of surrounding code.
 *
 * Each pattern allows whitespace between `jest` and the member. That is not
 * defensive padding: prettier breaks long chains across lines, so
 * `jest\n    .fn()\n    .mockReturnValueOnce(...)` appears in real specs. A
 * contiguous `\bjest\.fn\b` silently skipped those, and the spike surfaced it as a
 * runtime `ReferenceError: jest is not defined` — a rewrite gap that only shows up
 * when the test executes, which is the worst place to find one.
 */
const RULES = [
    // Spectator entry point — 593 files. The package publishes ./vitest alongside ./jest.
    { id: 'spectator-entry', re: /(['"])@openng\/spectator\/jest\1/g, to: '$1@openng/spectator/vitest$1' },

    // Mock/spy factory functions.
    { id: 'jest.fn', re: /\bjest\s*\.fn\b/g, to: 'vi.fn' },
    { id: 'jest.spyOn', re: /\bjest\s*\.spyOn\b/g, to: 'vi.spyOn' },
    { id: 'jest.mock', re: /\bjest\s*\.mock\b/g, to: 'vi.mock' },
    { id: 'jest.unmock', re: /\bjest\s*\.unmock\b/g, to: 'vi.unmock' },
    { id: 'jest.doMock', re: /\bjest\s*\.doMock\b/g, to: 'vi.doMock' },

    // Mock state.
    { id: 'clearAllMocks', re: /\bjest\s*\.clearAllMocks\b/g, to: 'vi.clearAllMocks' },
    { id: 'resetAllMocks', re: /\bjest\s*\.resetAllMocks\b/g, to: 'vi.resetAllMocks' },
    { id: 'restoreAllMocks', re: /\bjest\s*\.restoreAllMocks\b/g, to: 'vi.restoreAllMocks' },
    { id: 'resetModules', re: /\bjest\s*\.resetModules\b/g, to: 'vi.resetModules' },

    // Timers.
    { id: 'useFakeTimers', re: /\bjest\s*\.useFakeTimers\b/g, to: 'vi.useFakeTimers' },
    { id: 'useRealTimers', re: /\bjest\s*\.useRealTimers\b/g, to: 'vi.useRealTimers' },
    { id: 'advanceTimersByTime', re: /\bjest\s*\.advanceTimersByTime\b/g, to: 'vi.advanceTimersByTime' },
    { id: 'runAllTimers', re: /\bjest\s*\.runAllTimers\b/g, to: 'vi.runAllTimers' },
    { id: 'runOnlyPendingTimers', re: /\bjest\s*\.runOnlyPendingTimers\b/g, to: 'vi.runOnlyPendingTimers' },
    { id: 'setSystemTime', re: /\bjest\s*\.setSystemTime\b/g, to: 'vi.setSystemTime' },
    { id: 'getRealSystemTime', re: /\bjest\s*\.getRealSystemTime\b/g, to: 'vi.getRealSystemTime' },

    // Misc runtime.
    { id: 'setTimeout', re: /\bjest\s*\.setTimeout\b/g, to: 'vi.setConfig({ testTimeout: ' , special: 'setTimeout' },

    { id: 'clearAllTimers', re: /\bjest\s*\.clearAllTimers\b/g, to: 'vi.clearAllTimers' },
    { id: 'getTimerCount', re: /\bjest\s*\.getTimerCount\b/g, to: 'vi.getTimerCount' },
    { id: 'advanceTimersByTimeAsync', re: /\bjest\s*\.advanceTimersByTimeAsync\b/g, to: 'vi.advanceTimersByTimeAsync' },

    // Typed-mock helper.
    { id: 'jest.mocked', re: /\bjest\s*\.mocked\b/g, to: 'vi.mocked' },

    // Types. The trailing \b is what keeps `jest.Mock` from eating the prefix of
    // `jest.MockedFunction`, so these can be listed in any order safely.
    { id: 'type-MockedFunction', re: /\bjest\s*\.MockedFunction\b/g, to: 'MockedFunction' },
    { id: 'type-MockedClass', re: /\bjest\s*\.MockedClass\b/g, to: 'MockedClass' },
    { id: 'type-Mocked', re: /\bjest\s*\.Mocked\b/g, to: 'Mocked' },
    { id: 'type-Mock', re: /\bjest\s*\.Mock\b/g, to: 'Mock' },
    { id: 'type-MockInstance', re: /\bjest\s*\.MockInstance\b/g, to: 'MockInstance' },
    // Jest's SpyInstance / SpiedFunction both map onto Vitest's MockInstance.
    { id: 'type-SpyInstance', re: /\bjest\s*\.SpyInstance\b/g, to: 'MockInstance' },
    { id: 'type-SpiedFunction', re: /\bjest\s*\.SpiedFunction\b/g, to: 'MockInstance' },

    // Jest's x/f prefixed globals have no Vitest equivalent. Anchored on the opening
    // paren so a word like "profit" or a variable named `fit` is not rewritten.
    { id: 'xdescribe', re: /\bxdescribe\(/g, to: 'describe.skip(' },
    { id: 'xit', re: /\bxit\(/g, to: 'it.skip(' },
    { id: 'xtest', re: /\bxtest\(/g, to: 'test.skip(' },
    { id: 'fdescribe', re: /\bfdescribe\(/g, to: 'describe.only(' },
    { id: 'fit', re: /\bfit\(/g, to: 'it.only(' },

    // The `jest` global passed as a value — utils-testing's router mock takes the
    // whole namespace as an argument so it can create spies for the caller.
    { id: 'jest-as-argument', re: /\bMockDotRouterJestService\(\s*jest\s*\)/g, to: 'MockDotRouterJestService(vi)' },

    // Import source.
    { id: 'globals-import', re: /(['"])@jest\/globals\1/g, to: '$1vitest$1' },

    // `mockImplementation()` with NO argument. Jest installs a no-op returning
    // undefined; Vitest leaves the ORIGINAL implementation in place and calls through
    // (measured: a spy on a method returning 'ORIGINAL' still returned 'ORIGINAL').
    // 55 sites relied on the Jest reading — most of them silencing console, but also
    // `confirmation.confirm`, `store.fireWorkflowAction` and `router.navigateByUrl`,
    // where calling through ran real code against half-built mocks. `() => undefined`
    // restores Jest's behaviour and stays assignable for void-returning methods;
    // `navigateByUrl` returns a Promise and is handled separately below.
    {
        id: 'empty-mockImplementation',
        re: /\.mockImplementation\(\)/g,
        to: '.mockImplementation(() => undefined)'
    },

    // Router.navigateByUrl returns Promise<boolean>, so `() => undefined` would not
    // typecheck. Resolving true is also closer to the truth than Jest's undefined,
    // which would have rejected anything that awaited it.
    {
        id: 'navigateByUrl-mockImplementation',
        re: /\.spyOn\((\w+), 'navigateByUrl'\)\.mockImplementation\(\(\) => undefined\)/g,
        to: ".spyOn($1, 'navigateByUrl').mockResolvedValue(true)"
    }
];

/** Constructs that need a human. Reported, never rewritten. */
const FLAGS = [
    { id: 'requireActual', re: /\bjest\.requireActual\b/,
      why: 'vi.importActual is async — the call site must become async too' },
    { id: 'requireMock', re: /\bjest\.requireMock\b/,
      why: 'vi.importMock is async — same problem' },
    { id: 'mock-factory-closure', re: /vi\.mock\([^)]*,\s*\(\)\s*=>\s*\{[\s\S]{0,400}?\b(mock[A-Z]\w*|MOCK_\w+)\b/,
      why: 'mock factory appears to close over an outer binding; hoisting semantics differ — verify by hand' },
    { id: 'jest-residual', re: /\bjest\./,
      why: 'a jest.* reference this codemod does not know about' },
    { id: 'genMockFromModule', re: /\bjest\.(genMockFromModule|createMockFromModule)\b/,
      why: 'no direct Vitest equivalent' }
];

/**
 * Convert Jest's `done` callback style to a Promise, which is what Vitest supports.
 *
 *   it('x', (done) => { ...; done(); })
 *     ->
 *   it('x', () => new Promise<void>((done) => { ...; done(); }))
 *
 * Vitest removed `done` outright — the suites fail with "done() callback is
 * deprecated, use promise instead" — and 87 files in this workspace use it, so hand
 * conversion was not realistic.
 *
 * Brace counting rather than a regex for the body: a test body contains braces,
 * strings and template literals, and a regex would close the wrapper in the wrong
 * place. The scan below tracks string/template/comment state so a `}` inside a
 * string cannot end the body early. Anything it cannot parse confidently is left
 * alone and reported as a hand-edit — a wrong transform here is worse than none.
 */
function convertDoneCallbacks(src) {
    // The callback is not always literally named `done` — this workspace also uses
    // `doneFn`. Match the common done-style names and reuse whichever one the file
    // chose, rather than renaming the binding the body already refers to.
    const CALL = /\b(it|test)(\.\w+)*\(\s*(['"`])(?:[^\\]|\\.)*?\3\s*,\s*(async\s+)?\(\s*(done|doneFn|_done|cb)\s*(?::\s*[^)]*)?\)\s*=>\s*\{/g;
    let out = '';
    let last = 0;
    let count = 0;
    let m;
    while ((m = CALL.exec(src)) !== null) {
        const bodyStart = m.index + m[0].length;
        const end = findMatchingBrace(src, bodyStart);
        if (end === -1) continue; // unbalanced: leave it for a human
        const head = m[0];
        const isAsync = Boolean(m[4]);
        const cbName = m[5];
        const newHead = head.replace(
            /(async\s+)?\(\s*(?:done|doneFn|_done|cb)\s*(?::\s*[^)]*)?\)\s*=>\s*\{$/,
            `${isAsync ? 'async ' : ''}() => new Promise<void>((${cbName}) => {`
        );
        // The original call's own `)` must be consumed, not left behind: the new tail
        // `}))` already closes the body, the Promise and the it() call.
        let after = end + 1;
        while (after < src.length && /\s/.test(src[after])) after++;
        if (src[after] === ')') after++;
        else after = end + 1; // shape we did not expect — fall back and let it fail loudly

        out += src.slice(last, m.index) + newHead + src.slice(bodyStart, end) + '}))';
        last = after;
        count++;
        CALL.lastIndex = after;
    }
    out += src.slice(last);
    return { src: count ? out : src, count };
}

/** Index of the `}` closing the block that starts at `from`, ignoring braces inside strings and comments. */
function findMatchingBrace(src, from) {
    let depth = 1;
    let i = from;
    let quote = null;
    let comment = null;
    while (i < src.length) {
        const c = src[i];
        const next = src[i + 1];
        if (comment) {
            if (comment === '//' && c === '\n') comment = null;
            else if (comment === '/*' && c === '*' && next === '/') { comment = null; i++; }
        } else if (quote) {
            if (c === '\\') i++;
            else if (c === quote) quote = null;
        } else if (c === '/' && next === '/') { comment = '//'; i++; }
        else if (c === '/' && next === '*') { comment = '/*'; i++; }
        else if (c === "'" || c === '"' || c === '`') quote = c;
        else if (c === '{') depth++;
        else if (c === '}') { depth--; if (depth === 0) return i; }
        i++;
    }
    return -1;
}

/**
 * Convert `jest.requireActual` to `vi.importActual`.
 *
 * The two are not interchangeable — `vi.importActual` is async — which is why this
 * was originally left as a hand-edit for ~19 files. It is still mechanical, though:
 * the mock factory becomes `async` and the call gets awaited.
 *
 *   vi.mock('x', () => ({ ...jest.requireActual('x'), a: vi.fn() }))
 *     ->
 *   vi.mock('x', async () => ({ ...(await vi.importActual('x')), a: vi.fn() }))
 *
 * Only factories that actually contain a requireActual are made async, so a factory
 * that does not need it is left synchronous.
 */
function convertRequireActual(src) {
    if (!/\bjest\s*\.require(Actual|Mock)\b/.test(src)) return { src, count: 0 };
    let count = 0;

    // 1. Make the enclosing vi.mock factory async, but only where needed.
    const MOCK = /vi\.mock\(\s*(['"`])(?:[^\\]|\\.)*?\1\s*,\s*(?!async)\(\s*\)\s*=>/g;
    let out = '';
    let last = 0;
    let m;
    while ((m = MOCK.exec(src)) !== null) {
        const bodyStart = m.index + m[0].length;
        // Look only as far as the factory plausibly extends, so an unrelated
        // requireActual later in the file does not make this factory async.
        const window = src.slice(bodyStart, bodyStart + 2000);
        const needsAsync = /\bjest\s*\.require(Actual|Mock)\b/.test(window);
        out += src.slice(last, m.index) + (needsAsync ? m[0].replace(/\(\s*\)\s*=>$/, 'async () =>') : m[0]);
        last = bodyStart;
    }
    out += src.slice(last);
    src = out;

    // 2. Await the call itself, parenthesised so spreads keep working.
    src = src.replace(/\.\.\.\s*jest\s*\.requireActual(<[^>]*>)?\(/g, () => {
        count++;
        return '...(await vi.importActual(';
    });
    // Close the extra paren the spread form opened.
    src = src.replace(/\.\.\.\(await vi\.importActual\(([^)]*)\)/g, '...(await vi.importActual($1))');

    src = src.replace(/\bjest\s*\.requireActual(<[^>]*>)?\(/g, () => {
        count++;
        return 'await vi.importActual(';
    });
    src = src.replace(/\bjest\s*\.requireMock(<[^>]*>)?\(/g, () => {
        count++;
        return 'await vi.importMock(';
    });

    return { src, count };
}

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const reportOnly = args.includes('--report');
const targets = args.filter((a) => !a.startsWith('--'));

if (targets.length === 0) {
    console.error('usage: codemod-jest-to-vitest.mjs [--dry-run|--report] <path> [<path> ...]');
    process.exit(2);
}

/**
 * Directories this codemod must never enter, mirroring the DENY list in
 * verify-test-only-diff.mjs. Not belt-and-braces: running `codemod libs apps`
 * rewrote two Stencil specs, and the enforcement check caught it only afterwards.
 * Refusing here is cheaper than reverting there.
 */
const NEVER = [/(^|\/)dotcms-webcomponents(\/|$)/, /(^|\/)dotcms-ui-e2e(\/|$)/];

function collect(p, out = []) {
    if (NEVER.some((re) => re.test(p))) return out;
    const st = statSync(p);
    if (st.isDirectory()) {
        for (const entry of readdirSync(p)) {
            if (entry === 'node_modules' || entry.startsWith('.')) continue;
            collect(join(p, entry), out);
        }
    } else if (TEST_FILE.test(p) || TEST_SUPPORT.test(p)) {
        out.push(p);
    }
    return out;
}

/**
 * Ensure `vi` (and any needed types) are imported. Vitest globals may be enabled,
 * but relying on that makes each file's correctness depend on config it does not
 * name — an explicit import is what the existing specs already do for `@jest/globals`
 * where they use it, and it survives a config change.
 */
function ensureImports(src, needs) {
    if (needs.size === 0) return src;
    const existing = /^import\s*\{([^}]*)\}\s*from\s*(['"])vitest\2;?/m.exec(src);
    if (existing) {
        const have = new Set(existing[1].split(',').map((s) => s.trim()).filter(Boolean));
        for (const n of needs) have.add(n);
        const merged = [...have].sort().join(', ');
        return src.replace(existing[0], `import { ${merged} } from 'vitest';`);
    }
    const names = [...needs].sort().join(', ');
    const line = `import { ${names} } from 'vitest';\n`;
    // Place it after the final leading import so import order stays conventional.
    const imports = [...src.matchAll(/^import[\s\S]*?from\s*['"][^'"]+['"];?\s*$/gm)];
    if (imports.length === 0) return line + src;
    const last = imports[imports.length - 1];
    const at = last.index + last[0].length;
    return src.slice(0, at) + '\n' + line.trimEnd() + src.slice(at);
}

const counts = new Map();
const flagged = [];
let changed = 0;
let scanned = 0;

for (const target of targets) {
    for (const file of collect(target)) {
        scanned++;
        const original = readFileSync(file, 'utf8');
        let src = original;
        const needs = new Set();

        // The two rules that legitimately rewrite a module path run BEFORE masking:
        // '@openng/spectator/jest' -> '/vitest' and '@jest/globals' -> 'vitest' are
        // exactly the case the mask below is designed to prevent, so they are applied
        // first and by name rather than special-cased inside it.
        for (const rule of RULES.filter((r) => r.id === 'spectator-entry' || r.id === 'globals-import')) {
            const before = src;
            src = src.replace(rule.re, rule.to);
            if (src !== before) {
                counts.set(rule.id, (counts.get(rule.id) ?? 0) + (before.match(rule.re) ?? []).length);
            }
        }

        // Module paths are NOT code. A file named `dot-router-service-jest.mock.ts`
        // has a word-boundary `jest` in every import of it, and rewriting that turned
        // `'./lib/dot-router-service-jest.mock'` into `'…-vi.mock'` — an import of a
        // file that does not exist, which only surfaced when the suite ran. Mask
        // module-path string literals before rewriting and restore them after.
        const paths = [];
        src = src.replace(/(['"])(\.{1,2}\/[^'"]*|@?[\w@/.-]+\/[^'"]*)\1/g, (m) => {
            paths.push(m);
            return `\uE000PATH${paths.length - 1}\uE000`;
        });

        // Jasmine's bare `fail()` and jest's `done.fail()` do not exist in Vitest;
        // `expect.fail()` is the equivalent. 54 sites use them, all on "this should not
        // happen" branches, so they are dormant until something regresses — at which
        // point the test reports `ReferenceError: fail is not defined` instead of the
        // message its author wrote. Skipped where the file defines its own `fail`
        // (sdk/create-app's packaging spec does).
        if (!/\b(?:function|const|let)\s+fail\b/.test(src)) {
            const before = src;
            src = src.replace(/\bdone\.fail\(/g, 'expect.fail(').replace(/(^|[^.\w])fail\(/g, '$1expect.fail(');
            if (src !== before) {
                const n = (before.match(/\bdone\.fail\(|(?:^|[^.\w])fail\(/g) ?? []).length;
                counts.set('jasmine-fail', (counts.get('jasmine-fail') ?? 0) + n);
            }
        }

        for (const rule of RULES) {
            if (rule.id === 'spectator-entry' || rule.id === 'globals-import') continue;
            if (rule.special === 'setTimeout') {
                // jest.setTimeout(n) → vi.setConfig({ testTimeout: n })
                src = src.replace(/\bjest\.setTimeout\(\s*([^)]+?)\s*\)/g, (_m, n) => {
                    counts.set(rule.id, (counts.get(rule.id) ?? 0) + 1);
                    needs.add('vi');
                    return `vi.setConfig({ testTimeout: ${n} })`;
                });
                continue;
            }
            const before = src;
            src = src.replace(rule.re, rule.to);
            if (src !== before) {
                const n = (before.match(rule.re) ?? []).length;
                counts.set(rule.id, (counts.get(rule.id) ?? 0) + n);
                if (rule.id.startsWith('type-')) needs.add(rule.to);
                // The x/f prefixed globals map onto describe/it/test, which are already
                // globals here — adding `vi` for them would import something unused.
                else if (!['spectator-entry','globals-import','xdescribe','xit','xtest','fdescribe','fit'].includes(rule.id)) needs.add('vi');
            }
        }

        src = src.replace(/\uE000PATH(\d+)\uE000/g, (_m, i) => paths[Number(i)]);

        const ra = convertRequireActual(src);
        if (ra.count) {
            src = ra.src;
            counts.set('requireActual', (counts.get('requireActual') ?? 0) + ra.count);
            needs.add('vi');
        }

        const done = convertDoneCallbacks(src);
        if (done.count) {
            src = done.src;
            counts.set('done-callback', (counts.get('done-callback') ?? 0) + done.count);
        }
        src = ensureImports(src, needs);

        for (const f of FLAGS) {
            if (f.re.test(src)) flagged.push({ file, flag: f.id, why: f.why });
        }

        if (src !== original) {
            changed++;
            if (!dryRun && !reportOnly) writeFileSync(file, src);
        }
    }
}

console.log(`Scanned ${scanned} test file(s); ${changed} would change${dryRun || reportOnly ? ' (no writes)' : ''}.\n`);
if (counts.size) {
    console.log('Rewrites by rule:');
    for (const [id, n] of [...counts].sort((a, b) => b[1] - a[1])) {
        console.log(`  ${String(n).padStart(6)}  ${id}`);
    }
}
if (flagged.length) {
    console.log(`\n${flagged.length} file(s) need a human — these are the FR-013 hand-edits:`);
    const byFlag = new Map();
    for (const f of flagged) {
        if (!byFlag.has(f.flag)) byFlag.set(f.flag, { why: f.why, files: [] });
        byFlag.get(f.flag).files.push(f.file);
    }
    for (const [flag, { why, files }] of byFlag) {
        console.log(`\n  ${flag} (${files.length}) — ${why}`);
        for (const f of files.slice(0, 10)) console.log(`      ${f}`);
        if (files.length > 10) console.log(`      … and ${files.length - 10} more`);
    }
    console.log('\nEnumerate every one of these in the PR description (FR-013). If this list is large,');
    console.log('that is the signal the diff has stopped being reviewable by pattern — see tasks.md T094.');
}
