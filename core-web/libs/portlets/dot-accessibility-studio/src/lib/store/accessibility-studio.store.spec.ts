import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { AccessibilityStudioStore } from './accessibility-studio.store';

import { StudioPageRow } from '../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../models/mock-fix-report';

const MOCK_CONTENTLETS = [
    {
        identifier: 'id-1',
        title: 'About Us',
        url: '/about-us',
        contentType: 'htmlpageasset',
        languageId: 1,
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
    hostName: 'demo.dotcms.com',
    modDate: '04/09/2026',
    modUserName: 'Admin User',
    live: true
};

describe('AccessibilityStudioStore', () => {
    let spectator: SpectatorService<InstanceType<typeof AccessibilityStudioStore>>;
    let store: InstanceType<typeof AccessibilityStudioStore>;
    let searchService: jest.Mocked<DotContentSearchService>;
    let currentSiteIdSignal: ReturnType<typeof signal<string | null>>;

    const createService = createServiceFactory({
        service: AccessibilityStudioStore,
        providers: [
            mockProvider(DotContentSearchService, {
                get: jest.fn().mockReturnValue(of(MOCK_SEARCH_ENTITY))
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
        currentSiteIdSignal = signal<string | null>('site-1');
        spectator = createService();
        store = spectator.service;
        searchService = spectator.inject(
            DotContentSearchService
        ) as jest.Mocked<DotContentSearchService>;
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
            expect(store.report()).toBeNull();
        });

        it('runScan moves ready → scanned and loads the mock report before-count', () => {
            store.runScan();
            expect(store.phase()).toBe('scanned');
            expect(store.scanned()).toBe(true);
            expect(store.beforeCount()).toBe(MOCK_FIX_REPORT.scan.before.violations);
        });

        it('runScan is a no-op when not in the ready phase', () => {
            store.runScan(); // ready → scanned
            store.runScan(); // already scanned, ignored
            expect(store.phase()).toBe('scanned');
        });

        it('startFix moves scanned → done with the full report', () => {
            store.runScan();
            store.startFix();
            expect(store.phase()).toBe('done');
            expect(store.isDone()).toBe(true);
            expect(store.fixedCount()).toBe(7);
            expect(store.reportedCount()).toBe(5);
            expect(store.afterCount()).toBe(MOCK_FIX_REPORT.scan.after.violations);
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

        it('backToPicker resets selection + report', () => {
            store.runScan();
            store.startFix();
            store.backToPicker();
            expect(store.phase()).toBe('picker');
            expect(store.selected()).toBeNull();
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
