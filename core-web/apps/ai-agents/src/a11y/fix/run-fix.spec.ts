import { runFix, type RunFixDeps } from './run-fix';

import type { FixRequest } from '../domain/contract';
import type { RenderSources, ScanFinding, ScanResult, SavedAsset } from '../dotcms/dotcms-client';

/**
 * runFix orchestration tests. The deterministic CSS attribution + contrast math
 * are unit-tested in css-attribution/css-source-map/contrast specs; here we test
 * the LOOP's routing and guards with a fake client and PASS 2 (research) disabled
 * (research: false) so no model/network is touched:
 *   - CSS (color-contrast) → deterministic path
 *   - non-CSS → deferred to PASS 2 (reported here)
 *   - editor chrome (data-dot-object edit buttons) → dropped
 *   - scan URL assembly (multilingual language_id)
 */

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

function makeScan(
    violations: number,
    items: ScanFinding[] = [],
    over: Partial<ScanResult> = {}
): ScanResult {
    return {
        ok: true,
        totalIssues: violations,
        counts: { errors: violations, warnings: 0, notices: 0 },
        findings: { total: violations, violations, needsReview: 0, items },
        stylesheets: [],
        ...over
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

const contrastFinding = (over: Partial<ScanFinding> = {}): ScanFinding =>
    makeFinding({
        code: 'color-contrast',
        context: '<a class="btn">x</a>',
        selector: '.btn',
        data: { fgColor: '#ffffff', bgColor: '#e76300', expectedContrastRatio: '4.5:1' },
        ...over
    });

const SOURCES: RenderSources = {
    containers: {},
    page: { identifier: 'a9f3', languageId: 1, uri: '/index' },
    theme: { folderPath: '/themes/travel', id: 't1', name: 'travel', files: [] },
    widgets: []
};

const savedOk = (over: Partial<SavedAsset> = {}): SavedAsset => ({
    fileSize: 1234,
    identifier: 'a56e',
    inode: 'i1',
    lang: 'en-us',
    live: false,
    name: 'x',
    path: '//x',
    working: true,
    ...over
});

function makeClient(opts: { scans: ScanResult[]; save?: jest.Mock }) {
    const scanQueue = [...opts.scans];
    const save = opts.save ?? jest.fn(async (path: string) => savedOk({ path }));
    return {
        scan: jest.fn(async () => scanQueue.shift() ?? makeScan(0)),
        locate: jest.fn(async () => SOURCES),
        read: jest.fn(async () => ''),
        fetchStylesheet: jest.fn(async () => ''),
        saveWorking: save
    } as unknown as RunFixDeps['client'] & {
        saveWorking: jest.Mock;
        scan: jest.Mock;
        locate: jest.Mock;
    };
}

describe('runFix orchestration', () => {
    it('produces a valid §6 report (publishRequired true) with research disabled', async () => {
        const client = makeClient({ scans: [makeScan(0), makeScan(0), makeScan(0)] });
        const report = await runFix(makeRequest(), { client, research: false });
        expect(report.runId).toBe('r_test');
        expect(report.publishRequired).toBe(true);
        expect(report.page).toEqual({ uri: '/index', host: 'demo.dotcms.com', languageId: 1 });
    });

    it('defers non-CSS violations to PASS 2 (reported) — no LLM in PASS 1', async () => {
        const client = makeClient({
            scans: [makeScan(1, [makeFinding()]), makeScan(1), makeScan(1)]
        });
        const report = await runFix(makeRequest(), { client, research: false });
        const imgAlt = report.results.find((r) => r.ruleId === 'image-alt');
        expect(imgAlt?.status).toBe('reported');
        expect(imgAlt?.reason).toMatch(/agentic research/i);
        // Nothing was saved (PASS 1 doesn't touch VTL anymore; PASS 2 disabled).
        expect(client.saveWorking).not.toHaveBeenCalled();
    });

    it('routes color-contrast to the deterministic CSS path (attempts a stylesheet)', async () => {
        // No same-origin stylesheet → CSS path reports "no stylesheet", proving it routed
        // to processCssViolation (not the deferral path).
        const client = makeClient({
            scans: [makeScan(1, [contrastFinding()]), makeScan(1), makeScan(1)]
        });
        const report = await runFix(makeRequest(), { client, research: false });
        const cc = report.results.find((r) => r.ruleId === 'color-contrast');
        expect(cc?.status).toBe('reported');
        expect(cc?.reason).toMatch(/no same-origin stylesheet/i);
    });

    it('honors skipCss: contrast reported without attribution', async () => {
        const client = makeClient({
            scans: [makeScan(1, [contrastFinding()]), makeScan(1), makeScan(1)]
        });
        const report = await runFix(makeRequest({ options: { skipCss: true } }), {
            client,
            research: false
        });
        const cc = report.results.find((r) => r.ruleId === 'color-contrast');
        expect(cc?.reason).toMatch(/skipCss/i);
    });

    it('drops editor chrome (data-dot-object edit buttons) — not in results', async () => {
        const chrome = makeFinding({
            code: 'button-name',
            context: '<button data-dot-object="edit-content">edit</button>',
            selector: 'button'
        });
        const client = makeClient({
            scans: [makeScan(1, [chrome]), makeScan(1), makeScan(1)]
        });
        const report = await runFix(makeRequest(), { client, research: false });
        expect(report.results.find((r) => r.ruleId === 'button-name')).toBeUndefined();
    });

    it('multilingual page adds language_id to the PREVIEW_MODE scan URL', async () => {
        const client = makeClient({ scans: [makeScan(0), makeScan(0), makeScan(0)] });
        await runFix(makeRequest({ page: { ...makeRequest().page, languageId: 2 } }), {
            client,
            research: false
        });
        const previewUrl = client.scan.mock.calls[1][0] as string; // 2nd scan = PREVIEW baseline
        expect(previewUrl).toContain('mode=PREVIEW_MODE');
        expect(previewUrl).toContain('language_id=2');
    });

    it('aborts (no fixes) when a stylesheet/script failed to load', async () => {
        const broken = makeScan(5, [contrastFinding()], {
            renderReliable: false,
            renderWarnings: [{ url: '//x/styles.css', status: 404, resourceType: 'stylesheet' }]
        });
        const client = makeClient({ scans: [broken, broken, broken] });
        const report = await runFix(makeRequest(), { client, research: false });
        expect(report.results).toEqual([
            expect.objectContaining({ ruleId: 'render-unreliable', status: 'reported' })
        ]);
        expect(client.saveWorking).not.toHaveBeenCalled();
    });

    it('does NOT abort for non-render-affecting 404s (image/xhr/favicon)', async () => {
        // renderReliable:false but only an image + xhr failed — these don't affect
        // contrast/layout, so the run should proceed.
        const okish = makeScan(1, [makeFinding()], {
            renderReliable: false,
            renderWarnings: [
                { url: '//x/favicon.ico', status: 404, resourceType: 'other' },
                { url: '//x/hero.jpg', status: 404, resourceType: 'image' }
            ]
        });
        const client = makeClient({ scans: [okish, okish, okish] });
        const report = await runFix(makeRequest(), { client, research: false });
        // proceeds → the non-CSS violation is deferred to PASS 2, not the abort result
        expect(report.results.some((r) => r.ruleId === 'render-unreliable')).toBe(false);
    });
});
