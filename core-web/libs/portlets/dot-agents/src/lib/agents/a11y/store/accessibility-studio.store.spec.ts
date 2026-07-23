import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotPageScannerService, PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';

import { AccessibilityStudioStore } from './accessibility-studio.store';

import { A11yAgentStreamEvent, StudioPageRow } from '../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../models/mock-fix-report';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';

/**
 * A canned SSE run in the current contract: the run-id frame, two `phase` steps,
 * a `progress` count, a `workingChanged` file set, then `done` with the report.
 */
const MOCK_FIX_STREAM: A11yAgentStreamEvent[] = [
    { type: 'run', runId: 'r_test_123' },
    { type: 'phase', step: { message: 'Scanning live + working baseline', meta: { phase: 'scan' } } },
    { type: 'phase', step: { message: 'Fixing color-contrast → .btn', meta: { phase: 'fix' } } },
    { type: 'progress', progress: { baseline: 5, current: 2, cleared: 3 } },
    {
        type: 'workingChanged',
        changedFiles: [{ path: '//site/application/themes/x/style.css', identifier: 'css-id' }]
    },
    { type: 'done', result: MOCK_FIX_REPORT }
];

// Two violation rules (3 + 2 = 5 error elements) + one incomplete rule (2 warnings).
const MOCK_SCAN_RESPONSE = {
    ok: true,
    standard: 'WCAG2AA',
    axe: {
        violations: [
            {
                id: 'image-alt',
                impact: 'critical',
                description: 'Images must have alternate text',
                help: '',
                helpUrl: 'https://example.com/image-alt',
                tags: [],
                nodes: [
                    { html: '<img>', target: ['img.a'], impact: 'critical', failureSummary: '' },
                    { html: '<img>', target: ['img.b'], impact: 'critical', failureSummary: '' },
                    { html: '<img>', target: ['img.c'], impact: 'critical', failureSummary: '' }
                ]
            },
            {
                id: 'button-name',
                impact: 'serious',
                description: 'Buttons must have discernible text',
                help: '',
                helpUrl: 'https://example.com/button-name',
                tags: [],
                nodes: [
                    {
                        html: '<button>',
                        target: ['button.x'],
                        impact: 'serious',
                        failureSummary: ''
                    },
                    {
                        html: '<button>',
                        target: ['button.y'],
                        impact: 'serious',
                        failureSummary: ''
                    }
                ]
            }
        ],
        incomplete: [
            {
                id: 'color-contrast',
                impact: 'moderate',
                description: 'Elements must have sufficient color contrast',
                help: '',
                helpUrl: 'https://example.com/color-contrast',
                tags: [],
                nodes: [
                    { html: '<a>', target: ['a.l1'], impact: 'moderate', failureSummary: '' },
                    { html: '<a>', target: ['a.l2'], impact: 'moderate', failureSummary: '' }
                ]
            }
        ]
    }
} as unknown as PageScannerA11yResponse;

const MOCK_CONTENTLETS = [
    {
        identifier: 'id-1',
        title: 'About Us',
        url: '/about-us',
        contentType: 'htmlpageasset',
        languageId: 1,
        host: 'host-id-1',
        hostName: 'demo.dotcms.com',
        modDate: '04/09/2026',
        modUserName: 'Admin User',
        live: true
    },
    {
        identifier: 'id-2',
        title: 'Blog Post',
        url: '/blog/post/hello',
        contentType: 'Blog',
        languageId: 1,
        host: 'host-id-1',
        hostName: 'demo.dotcms.com',
        modDate: '03/10/2026',
        modUserName: 'Admin User',
        live: false
    }
] as unknown as DotCMSContentlet[];

const MOCK_SEARCH_ENTITY = {
    jsonObjectView: { contentlets: MOCK_CONTENTLETS },
    resultsSize: 42
};

const MOCK_ROW: StudioPageRow = {
    identifier: 'id-1',
    title: 'About Us',
    path: '/about-us',
    type: 'htmlpageasset',
    languageId: 1,
    hostId: 'host-id-1',
    hostName: 'demo.dotcms.com',
    modDate: '04/09/2026',
    modUserName: 'Admin User',
    live: true
};

describe('AccessibilityStudioStore', () => {
    let spectator: SpectatorService<InstanceType<typeof AccessibilityStudioStore>>;
    let store: InstanceType<typeof AccessibilityStudioStore>;
    let searchService: jest.Mocked<DotContentSearchService>;
    let scannerService: jest.Mocked<DotPageScannerService>;
    let agentService: jest.Mocked<DotA11yAgentService>;
    let currentSiteIdSignal: ReturnType<typeof signal<string | null>>;

    const createService = createServiceFactory({
        service: AccessibilityStudioStore,
        providers: [
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(MOCK_SEARCH_ENTITY))
            }),
            mockProvider(DotPageScannerService, {
                checkA11y: jest.fn().mockReturnValue(of(MOCK_SCAN_RESPONSE))
            }),
            mockProvider(DotA11yAgentService, {
                fixStream: jest.fn().mockReturnValue(of(...MOCK_FIX_STREAM)),
                stop: jest.fn().mockReturnValue(of(null))
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn().mockReturnValue(of(null))
            }),
            mockProvider(GlobalStore, {
                get currentSiteId() {
                    return currentSiteIdSignal;
                }
            })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        currentSiteIdSignal = signal<string | null>('site-1');
        spectator = createService();
        store = spectator.service;
        searchService = spectator.inject(
            DotContentSearchService
        ) as jest.Mocked<DotContentSearchService>;
        scannerService = spectator.inject(
            DotPageScannerService
        ) as jest.Mocked<DotPageScannerService>;
        agentService = spectator.inject(DotA11yAgentService) as jest.Mocked<DotA11yAgentService>;
        // The onInit effect triggers loadPages while in the picker phase.
        spectator.flushEffects();
    });

    describe('Picker', () => {
        it('starts in the picker phase', () => {
            expect(store.phase()).toBe('picker');
            expect(store.inPicker()).toBe(true);
            expect(store.inStudio()).toBe(false);
        });

        it('loads + projects pages into rows on init', () => {
            expect(searchService.get).toHaveBeenCalled();
            expect(store.pages().length).toBe(2);
            expect(store.pages()[0]).toEqual(MOCK_ROW);
            expect(store.totalRecords()).toBe(42);
            expect(store.pickerStatus()).toBe('loaded');
        });

        it('prefers the urlMap over url for the row path (URL-mapped content)', () => {
            searchService.get.mockClear();
            searchService.get.mockReturnValueOnce(
                of({
                    jsonObjectView: {
                        contentlets: [
                            {
                                ...MOCK_CONTENTLETS[1],
                                url: '/blog-detail-template', // detail template URL
                                urlMap: '/blog/post/hello' // the real navigable path
                            }
                        ]
                    },
                    resultsSize: 1
                })
            );
            // Re-trigger a load with the urlMapped contentlet.
            store.setFilter('hello');
            spectator.flushEffects();

            expect(store.pages()[0].path).toBe('/blog/post/hello');
        });

        it('builds a host-scoped pages query', () => {
            const query = (searchService.get.mock.calls[0][0] as { query: string }).query;
            expect(query).toContain('+working:true');
            expect(query).toContain('+(urlmap:* OR basetype:5)');
            expect(query).toContain('+deleted:false');
            expect(query).toContain('+conhost:site-1');
            expect(query).not.toContain('title:');
        });

        it('does not fetch until the current site is known, then fetches scoped', () => {
            // Simulate the real boot order: site resolves AFTER init.
            searchService.get.mockClear();
            currentSiteIdSignal.set(null);
            spectator.flushEffects();
            expect(searchService.get).not.toHaveBeenCalled(); // no unscoped all-sites query

            currentSiteIdSignal.set('site-2');
            spectator.flushEffects();
            expect(searchService.get).toHaveBeenCalledTimes(1);
            const query = (searchService.get.mock.calls[0][0] as { query: string }).query;
            expect(query).toContain('+conhost:site-2');
        });

        it('adds a title/path/urlmap clause when filtering', () => {
            searchService.get.mockClear();
            store.setFilter('contact');
            spectator.flushEffects();

            const query = (searchService.get.mock.calls[0][0] as { query: string }).query;
            expect(query).toContain('+(title:contact* OR path:*contact* OR urlmap:*contact*)');
            expect(store.page()).toBe(1);
        });

        it('escapes Lucene special characters in the filter', () => {
            searchService.get.mockClear();
            store.setFilter('a:b(c)');
            spectator.flushEffects();

            const query = (searchService.get.mock.calls[0][0] as { query: string }).query;
            expect(query).toContain('a\\:b\\(c\\)');
        });

        it('translates pagination into limit/offset', () => {
            searchService.get.mockClear();
            store.setPagination(3, 10);
            spectator.flushEffects();

            const params = searchService.get.mock.calls[0][0] as {
                limit: number;
                offset: number;
            };
            expect(params.limit).toBe(10);
            expect(params.offset).toBe(20);
        });

        it('handles a search error without throwing', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            searchService.get.mockReturnValueOnce(throwError(() => new Error('boom')));
            store.setFilter('err');
            spectator.flushEffects();

            expect(errorManager.handle).toHaveBeenCalled();
            expect(store.pickerStatus()).toBe('error');
        });
    });

    describe('Studio state machine', () => {
        beforeEach(() => {
            store.openPage(MOCK_ROW);
        });

        it('openPage moves to the ready phase with the selected page', () => {
            expect(store.phase()).toBe('ready');
            expect(store.isReady()).toBe(true);
            expect(store.selected()).toEqual(MOCK_ROW);
            expect(store.scanResult()).toBeNull();
            expect(store.report()).toBeNull();
        });

        describe('openPageByUri (deep-link rehydration)', () => {
            it('is a no-op when the requested page path is already selected', () => {
                // MOCK_ROW (/about-us) is already selected by the outer beforeEach.
                searchService.get.mockClear();
                store.openPageByUri('/about-us');
                expect(searchService.get).not.toHaveBeenCalled();
                expect(store.rehydrateStatus()).toBe('idle');
            });

            it('fetches a page by its URI (host-scoped) and opens it to ready', () => {
                // Ignore the init loadPages call; assert on the openPageByUri fetch.
                searchService.get.mockClear();
                searchService.get.mockReturnValueOnce(
                    of({ jsonObjectView: { contentlets: [MOCK_CONTENTLETS[1]] } })
                );
                store.openPageByUri('/blog/post/hello');

                const query = (searchService.get.mock.calls[0][0] as { query: string }).query;
                expect(query).toContain('path:"/blog/post/hello"');
                expect(query).toContain('urlmap:"/blog/post/hello"');
                expect(query).toContain('+working:true');
                expect(query).toContain('+deleted:false');
                expect(query).toContain('+conhost:site-1'); // host-scoped
                expect(store.selected()?.path).toBe('/blog/post/hello');
                expect(store.phase()).toBe('ready');
                expect(store.rehydrateStatus()).toBe('idle');
            });

            it('stays loading (no fetch) until the current site is known', () => {
                searchService.get.mockClear();
                currentSiteIdSignal.set(null);
                store.openPageByUri('/blog/post/hello');
                expect(searchService.get).not.toHaveBeenCalled();
                expect(store.rehydrateStatus()).toBe('loading');
            });

            it('flags not-found when the path resolves to no page', () => {
                searchService.get.mockClear();
                searchService.get.mockReturnValueOnce(
                    of({ jsonObjectView: { contentlets: [] } })
                );
                store.openPageByUri('/missing');
                expect(store.rehydrateStatus()).toBe('not-found');
            });

            it('flags not-found and does not throw when the fetch errors', () => {
                searchService.get.mockClear();
                searchService.get.mockReturnValueOnce(throwError(() => new Error('boom')));
                store.openPageByUri('/blog/post/hello');
                expect(store.rehydrateStatus()).toBe('not-found');
            });
        });

        it('runScan fires two scans: the primary EDIT_MODE (working) scan and the comparison LIVE scan', () => {
            store.runScan();
            // One scan for the UI-driving preview render, one comparison scan for
            // the live-frame markers.
            expect(scannerService.checkA11y).toHaveBeenCalledTimes(2);

            const previewUrl = scannerService.checkA11y.mock.calls[0][0];
            expect(previewUrl).toContain(`${window.location.origin}/about-us`);
            expect(previewUrl).toContain('host_id=host-id-1');
            expect(previewUrl).toContain('language_id=1');
            expect(previewUrl).toContain('mode=EDIT_MODE');

            const liveUrl = scannerService.checkA11y.mock.calls[1][0];
            expect(liveUrl).toContain(`${window.location.origin}/about-us`);
            expect(liveUrl).toContain('host_id=host-id-1');
            expect(liveUrl).toContain('mode=LIVE');
        });

        it('runScan populates liveScanResult (comparison-only) alongside scanResult', () => {
            store.runScan();
            expect(store.scanResult()).toBe(MOCK_SCAN_RESPONSE);
            expect(store.liveScanResult()).toBe(MOCK_SCAN_RESPONSE);
            expect(store.liveA11yGroups().length).toBe(3);
        });

        it('a failing LIVE scan does not derail the UI (comparison-only, error swallowed)', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            // First call = preview (succeeds), second = live (fails).
            scannerService.checkA11y
                .mockReturnValueOnce(of(MOCK_SCAN_RESPONSE))
                .mockReturnValueOnce(throwError(() => new Error('live boom')));
            store.runScan();
            // Preview scan drove the UI to scanned; the live failure is silent.
            expect(store.phase()).toBe('scanned');
            expect(store.scanResult()).toBe(MOCK_SCAN_RESPONSE);
            expect(store.liveScanResult()).toBeNull();
            expect(errorManager.handle).not.toHaveBeenCalled();
        });

        it('runScan stores the scan result and the real error/warning counts', () => {
            store.runScan();
            expect(store.phase()).toBe('scanned');
            expect(store.scanned()).toBe(true);
            expect(store.scanResult()).toBe(MOCK_SCAN_RESPONSE);
            expect(store.errorCount()).toBe(5); // 3 + 2 violation elements
            expect(store.warningCount()).toBe(2); // 2 incomplete elements
            expect(store.beforeCount()).toBe(5);
            expect(store.a11yGroups().length).toBe(3);
        });

        it('runScan re-scans from the scanned phase (the re-scan button)', () => {
            store.runScan(); // ready → scanned  (preview + live scan)
            store.runScan(); // scanned → scanning → scanned again (re-scan)
            // Two scans per run (preview + comparison live) × two runs = 4.
            expect(scannerService.checkA11y).toHaveBeenCalledTimes(4);
            expect(store.phase()).toBe('scanned');
        });

        it('re-scanning drops the prior scan result before the new one lands', () => {
            store.runScan(); // ready → scanned, result populated
            // Hold the second scan open so we can observe the cleared state.
            scannerService.checkA11y.mockReturnValueOnce(NEVER);
            store.runScan();
            expect(store.phase()).toBe('scanning');
            expect(store.scanResult()).toBeNull();
        });

        it('returns to ready and reports the error if the scan fails', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            scannerService.checkA11y.mockReturnValueOnce(throwError(() => new Error('boom')));
            store.runScan();
            expect(errorManager.handle).toHaveBeenCalled();
            expect(store.phase()).toBe('ready');
            expect(store.scanResult()).toBeNull();
        });

        it('startFix streams phase steps then moves scanned → done with the full report', () => {
            store.runScan();
            store.startFix();
            // Each SSE `phase` event was appended to the live activity log…
            expect(agentService.fixStream).toHaveBeenCalledTimes(1);
            expect(store.steps()).toHaveLength(2);
            expect(store.steps()[0]).toEqual({
                message: 'Scanning live + working baseline',
                meta: { phase: 'scan' }
            });
            // …and the terminal `done` event set the report + phase.
            expect(store.phase()).toBe('done');
            expect(store.isDone()).toBe(true);
            expect(store.fixedCount()).toBe(7);
            expect(store.reportedCount()).toBe(5);
            expect(store.afterCount()).toBe(MOCK_FIX_REPORT.scan.after.violations);
            // On done, changedFiles is synced to the report's authoritative set.
            expect(store.changedFiles()).toBe(MOCK_FIX_REPORT.changedFiles);
        });

        it('progress events drive the live openCount down while fixing', () => {
            // Hold the stream open right after a progress frame (no done yet) so we
            // observe the live count rather than the terminal report's after-count.
            agentService.fixStream.mockReturnValueOnce(
                of<A11yAgentStreamEvent>(
                    { type: 'run', runId: 'r_test_123' },
                    { type: 'progress', progress: { baseline: 5, current: 2, cleared: 3 } }
                )
            );
            store.runScan();
            store.startFix();
            expect(store.phase()).toBe('fixing'); // no terminal event → still fixing
            // openCount reflects the live `current`, not the scan's before-count (5).
            expect(store.openCount()).toBe(2);
        });

        it('workingChanged events accumulate the changed-file set while fixing', () => {
            const files = [
                { path: '//site/a.css', identifier: 'id-a' },
                { path: '//site/b.vtl', identifier: 'id-b' }
            ];
            agentService.fixStream.mockReturnValueOnce(
                of<A11yAgentStreamEvent>(
                    { type: 'run', runId: 'r_test_123' },
                    { type: 'workingChanged', changedFiles: [files[0]] },
                    { type: 'workingChanged', changedFiles: files } // full set each frame
                )
            );
            store.runScan();
            store.startFix();
            // The latest frame carries the full set — replace, not append.
            expect(store.changedFiles()).toEqual(files);
        });

        it('captures the run id from the stream and targets stop at it', () => {
            // Hold the stream open (after the run event) so we can stop mid-run.
            agentService.fixStream.mockReturnValueOnce(
                of<A11yAgentStreamEvent>(
                    { type: 'run', runId: 'r_test_123' },
                    { type: 'step', step: { message: 'working' } }
                )
            );
            store.runScan();
            store.startFix();
            expect(store.phase()).toBe('fixing'); // no terminal event → still fixing
            expect(store.runId()).toBe('r_test_123');

            store.stopAgent();
            expect(agentService.stop).toHaveBeenCalledWith('r_test_123');
        });

        it('stopAgent is a no-op when no run id has arrived yet', () => {
            agentService.fixStream.mockReturnValueOnce(
                of<A11yAgentStreamEvent>({ type: 'step', step: { message: 'working' } })
            );
            store.runScan();
            store.startFix();
            expect(store.runId()).toBeNull();

            store.stopAgent();
            expect(agentService.stop).not.toHaveBeenCalled();
        });

        it('startFix sends the selected page + skipCss in the agent request', () => {
            store.setSkipCss(true);
            store.runScan();
            store.startFix();
            const request = agentService.fixStream.mock.calls[0][0];
            // The proxy-request shape (plan §8.1): identifier + languageId + skipCss only.
            // The Java proxy resolves the page and builds the full FixRequest.
            expect(request.identifier).toBe('id-1');
            expect(request.languageId).toBe(1);
            expect(request.skipCss).toBe(true);
        });

        it('startFix returns to scanned and records the error on a terminal error event', () => {
            agentService.fixStream.mockReturnValueOnce(
                of<A11yAgentStreamEvent>({ type: 'error', message: 'render unreliable' })
            );
            store.runScan();
            store.startFix();
            expect(store.phase()).toBe('scanned');
            expect(store.fixError()).toBe('render unreliable');
            expect(store.report()).toBeNull();
        });

        it('startFix returns to scanned and records the error if the stream throws', () => {
            agentService.fixStream.mockReturnValueOnce(throwError(() => new Error('network down')));
            store.runScan();
            store.startFix();
            expect(store.phase()).toBe('scanned');
            expect(store.fixError()).toBe('network down');
        });

        it('publish moves done → published', () => {
            store.runScan();
            store.startFix();
            store.publish();
            expect(store.phase()).toBe('published');
            expect(store.isPublished()).toBe(true);
        });

        it('publish is a no-op unless done', () => {
            store.runScan();
            store.publish();
            expect(store.phase()).toBe('scanned');
        });

        it('discard returns from done to scanned', () => {
            store.runScan();
            store.startFix();
            store.discard();
            expect(store.phase()).toBe('scanned');
        });

        it('backToPicker resets selection, scan result + report', () => {
            store.runScan();
            store.startFix();
            store.backToPicker();
            expect(store.phase()).toBe('picker');
            expect(store.selected()).toBeNull();
            expect(store.scanResult()).toBeNull();
            expect(store.report()).toBeNull();
        });

        it('splits results into fixed vs reported buckets', () => {
            store.runScan();
            store.startFix();
            expect(store.fixedResults().every((r) => r.status === 'fixed-to-working')).toBe(true);
            expect(store.reportedResults().every((r) => r.status !== 'fixed-to-working')).toBe(
                true
            );
        });
    });

    describe('skip CSS toggle', () => {
        it('defaults to false and can be toggled', () => {
            expect(store.skipCss()).toBe(false);
            store.setSkipCss(true);
            expect(store.skipCss()).toBe(true);
        });
    });
});
