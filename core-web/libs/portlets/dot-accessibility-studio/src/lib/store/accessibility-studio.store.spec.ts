import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
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

/** A canned SSE run: two steps then `done` with the mock report. */
const MOCK_FIX_STREAM: A11yAgentStreamEvent[] = [
    { type: 'step', step: { message: 'Scanning live + working baseline', meta: { phase: 'scan' } } },
    { type: 'step', step: { message: 'Fixing color-contrast → .btn', meta: { phase: 'fix' } } },
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
                fixStream: jest.fn().mockReturnValue(of(...MOCK_FIX_STREAM))
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

        it('runScan calls the real scanner with an EDIT_MODE URL on the app origin', () => {
            store.runScan();
            expect(scannerService.checkA11y).toHaveBeenCalledTimes(1);
            const url = scannerService.checkA11y.mock.calls[0][0];
            expect(url).toContain(`${window.location.origin}/about-us`);
            expect(url).toContain('host_id=host-id-1');
            expect(url).toContain('language_id=1');
            expect(url).toContain('mode=EDIT_MODE');
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
            store.runScan(); // ready → scanned
            store.runScan(); // scanned → scanning → scanned again (re-scan)
            expect(scannerService.checkA11y).toHaveBeenCalledTimes(2);
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

        it('startFix streams steps then moves scanned → done with the full report', () => {
            store.runScan();
            store.startFix();
            // Each SSE `step` event was appended to the live activity log…
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
