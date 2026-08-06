import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DotContentSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { A11yPageListStore } from './a11y-page-list.store';

import { StudioPageRow } from '../models/accessibility-studio.models';

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

describe('A11yPageListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof A11yPageListStore>>;
    let store: InstanceType<typeof A11yPageListStore>;
    let searchService: jest.Mocked<DotContentSearchService>;
    let currentSiteIdSignal: ReturnType<typeof signal<string | null>>;

    const createService = createServiceFactory({
        service: A11yPageListStore,
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
        jest.clearAllMocks();
        currentSiteIdSignal = signal<string | null>('site-1');
        spectator = createService();
        store = spectator.service;
        searchService = spectator.inject(
            DotContentSearchService
        ) as jest.Mocked<DotContentSearchService>;
        // The onInit effect loads the page list — this store is page-list-only, no gate.
        spectator.flushEffects();
    });

    it('loads + projects pages into rows on init', () => {
        expect(searchService.get).toHaveBeenCalled();
        expect(store.pages().length).toBe(2);
        expect(store.pages()[0]).toEqual(MOCK_ROW);
        expect(store.totalRecords()).toBe(42);
        expect(store.pageListStatus()).toBe('loaded');
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
        expect(store.pageListStatus()).toBe('error');
    });
});
