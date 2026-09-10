/**
 * The replay corpus — the spike's evidence base.
 *
 * ── Pre-registration rule (fixed before any case was run) ────────────────────
 * A pull request is labelled `clean` if EVERY project it touches already declares the
 * convention the gate enforces: `strict` plus noPropertyAccessFromIndexSignature,
 * noImplicitOverride, noImplicitReturns and noFallthroughCasesInSwitch. Otherwise `debt`.
 * The rule is structural — derivable from tsconfigs and the diff, never from a gate result —
 * which is what keeps the sample from being fitted to the outcome.
 *
 * ── Why the clean set is small ───────────────────────────────────────────────
 * Measured across 42 recent frontend pull requests: 2 touch only full-convention projects,
 * 1 touches only strict-without-the-extras projects, and 39 (93%) touch at least one non-strict
 * project. Clean cases are rare BY STRUCTURE, not by cherry-picking. SC-002 asked for at least
 * three; this workspace contains two. That gap is reported rather than papered over by padding
 * the sample with a weak case — see INTERMEDIATE_TIER below.
 */

/** @typedef {{pr:number, expectation:'clean'|'debt', rationale:string,
 *             knownFindings: object[] | {file?:object[], line?:object[]}}} SampleCase */

/** @type {SampleCase[]} */
export const CORPUS = [
    {
        pr: 37264,
        expectation: 'debt',
        rationale:
            'sdk-create-app inherits strict:false; five violations confirmed with tsc against the ' +
            'merged tree before this corpus existed',
        // Qualified per granularity: two of the five sit on pre-existing lines the pull request
        // did not write, so line-level correctly reports three. A flat list would show a mismatch
        // under one granularity or the other no matter which numbers it held.
        knownFindings: {
            file: [
                { file: 'core-web/libs/sdk/create-app/src/index.ts', line: 294, code: 'TS4111' },
                { file: 'core-web/libs/sdk/create-app/src/index.ts', line: 515, code: 'TS4111' },
                { file: 'core-web/libs/sdk/create-app/src/utils/index.ts', line: 41, code: 'TS7030' },
                { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 263, code: 'TS2345' },
                { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 271, code: 'TS2345' }
            ],
            line: [
                { file: 'core-web/libs/sdk/create-app/src/index.ts', line: 294, code: 'TS4111' },
                { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 263, code: 'TS2345' },
                { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 271, code: 'TS2345' }
            ]
        }
    },
    {
        pr: 37415,
        expectation: 'debt',
        rationale: 'touches libs/edit-content, which declares no strict setting',
        knownFindings: []
    },
    {
        pr: 37372,
        expectation: 'debt',
        rationale: 'touches dot-content-drive/portlet and libs/ui, neither of which is strict',
        knownFindings: []
    },
    {
        pr: 37405,
        expectation: 'clean',
        rationale: 'every changed file is in libs/portlets/dot-auth, which declares the full convention',
        knownFindings: []
    },
    {
        pr: 37339,
        expectation: 'clean',
        rationale: 'touches only dotcms-models and libs/portlets/dot-auth; both declare the full convention',
        knownFindings: []
    }
];

/**
 * Deliberately NOT in CORPUS. libs/sdk/angular declares `strict: true` without the four extra
 * flags, so a finding there would be real debt the project never measured — not a false positive.
 * Including it would contaminate the denominator of the very rate the blocking decision rests on.
 * Reported separately in findings.md instead.
 */
export const INTERMEDIATE_TIER = [
    {
        pr: 37086,
        rationale: 'libs/sdk/angular: strict:true but none of the four extra flags',
        note: 'any finding here is genuine unmeasured debt, not gate noise'
    }
];

/**
 * Template-arm cases. Kept OUT of CORPUS on purpose: the template arm has its own go/no-go
 * (SC-013), and mixing its results into the false-positive denominator would make one number
 * stand for two very different risks.
 *
 * dotcms-ui carries `strictTemplates: false` behind
 * `TODO(#35930): re-enable strictTemplates once Angular 22 template errors are fixed per app`,
 * which is precisely the situation the template arm exists to test: can a diff-scoped gate
 * coexist with an application-wide opt-out?
 */
export const TEMPLATE_CASES = [
    {
        pr: 37248,
        rationale: 'one template file in apps/dotcms-ui, where template strictness is switched off',
        expectation: 'unknown — the cost and the finding count are what this case measures'
    }
];

const identity = (f) => `${f.file}:${f.line}:${f.code}`;

/**
 * @param {SampleCase} sample
 * @param {{findings: object[]}} report
 */
export function adjudicate(sample, report, granularity = 'line') {
    const found = report.findings ?? [];
    const foundKeys = new Set(found.map(identity));

    // `knownFindings` is either a flat list (granularity-independent) or keyed by granularity.
    const raw = sample.knownFindings ?? [];
    const known = Array.isArray(raw) ? raw : (raw[granularity] ?? []);
    const knownKeys = new Set(known.map(identity));

    const missed = known.filter((f) => !foundKeys.has(identity(f)));
    const unexpected = found.filter((f) => !knownKeys.has(identity(f)));

    if (sample.expectation === 'clean') {
        return {
            matchedExpectation: found.length === 0,
            falsePositives: found,
            unexpected,
            missed: []
        };
    }

    return {
        matchedExpectation: knownKeys.size > 0 ? missed.length === 0 : found.length > 0,
        falsePositives: [],
        unexpected,
        missed
    };
}

/** @param {{sample: SampleCase, verdict: ReturnType<typeof adjudicate>}[]} results */
export function summarize(results) {
    const clean = results.filter((r) => r.sample.expectation === 'clean');
    const withFindings = clean.filter((r) => (r.verdict.falsePositives?.length ?? 0) > 0);

    return {
        sampleSize: results.length,
        cleanCases: clean.length,
        cleanCasesWithFindings: withFindings.length,
        falsePositiveRate: clean.length === 0 ? null : withFindings.length / clean.length,
        debtCases: results.length - clean.length,
        debtCasesDetected: results.filter(
            (r) => r.sample.expectation === 'debt' && r.verdict.matchedExpectation
        ).length,
        unexpectedFindings: results.reduce((n, r) => n + (r.verdict.unexpected?.length ?? 0), 0),
        // Stated in the data, not only in prose: a rate quoted without its denominator gets
        // repeated as if it were a statistical claim. This sample cannot support one.
        caveat:
            `Measured on ${results.length} replayed pull request(s), ${clean.length} of them ` +
            `pre-registered clean. This is not a statistical claim.`
    };
}
