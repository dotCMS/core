import { runFix, type RunFixDeps } from './runFix';

import type { FixRequest } from './contract';
import type { RenderSources, ScanFinding, ScanResult, SavedAsset } from './dotcms-client';
import type { FixOutput, TriageDecision } from './triage';

/**
 * Loop guard tests (plan §5). The DotcmsClient and the two LLM calls are
 * injected, so these exercise the deterministic skeleton with zero network or
 * API key — the guards are real code paths here.
 */

const HEADER_VTL = '//demo.dotcms.com/application/themes/travel/header.vtl';

function makeRequest(overrides: Partial<FixRequest> = {}): FixRequest {
    return {
        runId: 'r_test',
        dotcmsBaseUrl: 'https://demo.dotcms.com',
        page: {
            identifier: 'a9f3',
            uri: '/index',
            liveUrl: 'https://demo.dotcms.com/index',
            host: 'demo.dotcms.com',
            hostId: '48190c8c',
            languageId: 1
        },
        options: { skipCss: false },
        ...overrides
    };
}

function makeScan(violations: number, items: ScanFinding[] = []): ScanResult {
    return {
        ok: true,
        totalIssues: violations,
        counts: { errors: violations, warnings: 0, notices: 0 },
        findings: { total: violations, violations, needsReview: 0, items }
    };
}

function makeFinding(over: Partial<ScanFinding> = {}): ScanFinding {
    return {
        code: 'image-alt',
        type: 'error',
        typeCode: 1,
        message: 'Image missing alt',
        context: '<img src="x.png">',
        selector: 'img',
        runner: 'axe',
        resultType: 'violation',
        ...over
    };
}

const SOURCES: RenderSources = {
    containers: {},
    page: { identifier: 'a9f3', languageId: 1, uri: '/index' },
    theme: {
        folderPath: '/themes/travel',
        id: 't1',
        name: 'travel',
        vtls: [{ identifier: 'a56e', path: HEADER_VTL }],
        css: [],
        js: []
    },
    widgets: []
};

const savedOk = (over: Partial<SavedAsset> = {}): SavedAsset => ({
    fileSize: 1234,
    identifier: 'a56e',
    inode: 'i1',
    lang: 'en-us',
    live: false,
    name: 'header.vtl',
    path: HEADER_VTL,
    working: true,
    ...over
});

/**
 * A fake client. `scans` is a queue consumed in call order (live, baseline,
 * per-fix re-scans, final). `liveText` backs read(). save() records calls and
 * returns savedOk by default.
 */
function makeClient(opts: { scans: ScanResult[]; liveText?: string; save?: jest.Mock }) {
    const scanQueue = [...opts.scans];
    const live = opts.liveText ?? '<img src="x.png">';
    const save =
        opts.save ??
        jest.fn(async (path: string, _content?: string, _mime?: string) =>
            savedOk({ path, name: path.split('/').pop() })
        );
    return {
        scan: jest.fn(async () => scanQueue.shift() ?? makeScan(0)),
        locate: jest.fn(async () => SOURCES),
        read: jest.fn(async (_path: string) => live),
        saveWorking: save
    } as unknown as RunFixDeps['client'] & { saveWorking: jest.Mock; scan: jest.Mock };
}

const triageFixable = async (): Promise<TriageDecision> => ({
    fixability: 'vtl',
    targetPath: HEADER_VTL,
    evidenceFound: true,
    reason: 'alt missing on img in header.vtl'
});

const fixApplied = async (): Promise<FixOutput> => ({
    newContent: '<img src="x.png" alt="A photo">',
    diff: '+ alt="A photo"',
    applied: true,
    reason: 'added alt'
});

describe('runFix loop guards (plan §5)', () => {
    it('fixes a violation to working and reports it (happy path)', async () => {
        const client = makeClient({
            scans: [
                makeScan(12, [makeFinding()]), // live
                makeScan(12), // baseline (EDIT_MODE)
                makeScan(11), // per-fix re-scan (improved)
                makeScan(11) // final
            ]
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: triageFixable,
            fix: fixApplied
        });

        expect(report.results).toHaveLength(1);
        expect(report.results[0].status).toBe('fixed-to-working');
        expect(report.results[0].file).toBe(HEADER_VTL);
        expect(report.publishRequired).toBe(true);
        expect(client.saveWorking).toHaveBeenCalledTimes(1);
    });

    it('fixes the same file twice — the second fix builds on the first edit', async () => {
        // Two violations both attributed to header.vtl. The second generateFix call
        // must receive the FIRST fix's output as its originalContent (not the live
        // original) — we no longer guard against in-run edits to the same file.
        const fix: jest.Mock = jest
            .fn()
            .mockResolvedValueOnce({ newContent: 'v1', diff: 'd1', applied: true, reason: '' })
            .mockResolvedValueOnce({ newContent: 'v2', diff: 'd2', applied: true, reason: '' });
        const client = makeClient({
            scans: [
                makeScan(12, [makeFinding(), makeFinding({ code: 'link-name' })]), // live: 2 violations
                makeScan(12), // baseline
                makeScan(11), // re-scan after fix 1
                makeScan(10), // re-scan after fix 2
                makeScan(10) // final
            ]
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: triageFixable,
            fix
        });

        expect(report.results.every((r) => r.status === 'fixed-to-working')).toBe(true);
        // First fix saw the live original; second fix saw the first fix's output.
        expect(fix.mock.calls[0][0].originalContent).toBe('<img src="x.png">');
        expect(fix.mock.calls[1][0].originalContent).toBe('v1');
        expect(client.saveWorking).toHaveBeenCalledTimes(2);
    });

    it('auto-revert: re-scan worse → reverts the asset and reports regressed', async () => {
        const save = jest.fn(async (path: string, _content: string, _mime?: string) =>
            savedOk({ path })
        );
        const client = makeClient({
            scans: [
                makeScan(12, [makeFinding()]), // live
                makeScan(12), // baseline
                makeScan(15) // per-fix re-scan (WORSE than baseline 12)
            ],
            save
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: triageFixable,
            fix: fixApplied
        });

        expect(report.results[0].status).toBe('regressed');
        expect(report.results[0].reverted).toBe(true);
        // Two saves: the edit, then the revert back to original.
        expect(save).toHaveBeenCalledTimes(2);
        expect(save.mock.calls[1][1]).toBe('<img src="x.png">'); // reverted to original content
    });

    it('attribution-evidence gate: reports (no edit) when evidence not found', async () => {
        const client = makeClient({
            scans: [makeScan(12, [makeFinding()]), makeScan(12), makeScan(12)]
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: async () => ({
                fixability: 'vtl',
                targetPath: HEADER_VTL,
                evidenceFound: false,
                reason: 'cannot locate the node in source'
            }),
            fix: fixApplied
        });

        expect(report.results[0].status).toBe('reported');
        expect(report.results[0].reason).toMatch(/not provable/i);
        expect(client.saveWorking).not.toHaveBeenCalled();
    });

    it('reports report-only triage without editing', async () => {
        const client = makeClient({
            scans: [makeScan(12, [makeFinding()]), makeScan(12), makeScan(12)]
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: async () => ({
                fixability: 'report-only',
                targetPath: null,
                evidenceFound: false,
                reason: 'content-field text; out of scope'
            }),
            fix: fixApplied
        });

        expect(report.results[0].status).toBe('reported');
        expect(client.saveWorking).not.toHaveBeenCalled();
    });

    it('failed: save persists 0 bytes → status failed', async () => {
        const client = makeClient({
            scans: [makeScan(12, [makeFinding()]), makeScan(12), makeScan(12)],
            save: jest.fn(async (path: string, _c?: string, _m?: string) =>
                savedOk({ path, fileSize: 0 })
            )
        });
        const report = await runFix(makeRequest(), {
            client,
            triage: triageFixable,
            fix: fixApplied
        });

        expect(report.results[0].status).toBe('failed');
        expect(report.results[0].reason).toMatch(/0 bytes/i);
    });

    it('multilingual page adds language_id to the EDIT_MODE scan URL', async () => {
        const client = makeClient({ scans: [makeScan(0, []), makeScan(0), makeScan(0)] });
        await runFix(makeRequest({ page: { ...makeRequest().page, languageId: 2 } }), {
            client,
            triage: triageFixable,
            fix: fixApplied
        });
        // 2nd scan call is the EDIT_MODE baseline.
        const editModeUrl = client.scan.mock.calls[1][0] as string;
        expect(editModeUrl).toContain('mode=EDIT_MODE');
        expect(editModeUrl).toContain('language_id=2');
    });
});
