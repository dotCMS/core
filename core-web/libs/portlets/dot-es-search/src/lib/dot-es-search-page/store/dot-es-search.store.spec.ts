import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotCurrentUserService,
    DotEsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotEsSearchStore, MAX_HITS } from './dot-es-search.store';

const MOCK_QUERY = '{ "query": { "match_all": {} } }';

const MOCK_RESPONSE = {
    contentlets: [],
    esresponse: [
        {
            hits: {
                total: 10,
                hits: [
                    {
                        _id: 'abc',
                        _index: 'live',
                        _type: 'content',
                        _score: 1,
                        _source: { title: 'Test' }
                    }
                ]
            },
            took: 55,
            timed_out: false
        }
    ]
};

const MOCK_AGG_RESPONSE = {
    contentlets: [],
    esresponse: [
        {
            hits: { total: 0, hits: [] },
            took: 20,
            timed_out: false,
            aggregations: {
                'sterms#by_type': {
                    buckets: [
                        { key: 'Blog', doc_count: 5 },
                        { key: 'News', doc_count: 3 }
                    ],
                    sum_other_doc_count: 2
                },
                'avg#avg_score': { value: 4.2 }
            }
        }
    ]
};

const MOCK_SUGGEST_RESPONSE = {
    contentlets: [],
    esresponse: [
        {
            hits: {
                total: 2,
                hits: [{ _id: 'x', _index: 'live', _type: 'content', _score: 1, _source: {} }]
            },
            took: 10,
            timed_out: false,
            suggest: {
                'title-correction': [
                    {
                        text: 'wintr',
                        offset: 0,
                        length: 5,
                        options: [{ text: 'winter', score: 0.8, freq: 10 }]
                    },
                    {
                        text: 'skying',
                        offset: 6,
                        length: 6,
                        options: [{ text: 'skiing', score: 0.9, freq: 20 }]
                    }
                ]
            }
        }
    ]
};

describe('DotEsSearchStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEsSearchStore>>;

    const createService = createServiceFactory({
        service: DotEsSearchStore,
        providers: [
            mockProvider(DotEsSearchService, {
                search: jest.fn().mockReturnValue(of(MOCK_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false }))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        spectator.flushEffects();
    });

    it('should initialise with INIT status and no response', () => {
        expect(spectator.service.status()).toBe(ComponentStatus.INIT);
        expect(spectator.service.response()).toBeNull();
    });

    describe('isAdmin', () => {
        it('should be false when current user is not admin', () => {
            expect(spectator.service.isAdmin()).toBe(false);
        });
    });

    describe('setQuery()', () => {
        it('should update the query signal', () => {
            spectator.service.setQuery(MOCK_QUERY);
            expect(spectator.service.query()).toBe(MOCK_QUERY);
        });
    });

    describe('setParam()', () => {
        it('should update a single param', () => {
            spectator.service.setParam('live', false);
            expect(spectator.service.params().live).toBe(false);
        });

        it('should preserve other params when updating one', () => {
            const before = { ...spectator.service.params() };
            spectator.service.setParam('userid', 'admin@dotcms.com');
            expect(spectator.service.params().live).toBe(before.live);
            expect(spectator.service.params().userid).toBe('admin@dotcms.com');
        });
    });

    describe('setWrapCode()', () => {
        it('should update the top-level wrapCode signal', () => {
            spectator.service.setWrapCode(true);
            expect(spectator.service.wrapCode()).toBe(true);
        });

        it('should toggle back to false', () => {
            spectator.service.setWrapCode(true);
            spectator.service.setWrapCode(false);
            expect(spectator.service.wrapCode()).toBe(false);
        });

        it('should not affect params', () => {
            const before = { ...spectator.service.params() };
            spectator.service.setWrapCode(true);
            expect(spectator.service.params()).toEqual(before);
        });
    });

    describe('runSearch()', () => {
        it('should set status to LOADED and populate response on success', () => {
            spectator.service.runSearch();
            expect(spectator.service.status()).toBe(ComponentStatus.LOADED);
            expect(spectator.service.response()).toEqual(MOCK_RESPONSE);
            expect(spectator.service.queryTimeMs()).toBeGreaterThanOrEqual(0);
        });

        it('should expose hits and contentlets via computed', () => {
            spectator.service.runSearch();
            expect(spectator.service.contentlets()).toEqual(MOCK_RESPONSE.contentlets);
            expect(spectator.service.hits()).toEqual(MOCK_RESPONSE.esresponse[0].hits.hits);
            expect(spectator.service.hitCount()).toBe(10);
        });

        it('should call httpErrorManager.handle and set ERROR status on failure', () => {
            const searchService = spectator.inject(DotEsSearchService);
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            (searchService.search as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('fail'))
            );

            spectator.service.runSearch();
            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
            expect(errorManager.handle).toHaveBeenCalled();
        });

        it('should set activeTab to results when explicitly called', () => {
            spectator.service.setActiveTab('raw');
            spectator.service.setActiveTab('results');
            expect(spectator.service.activeTab()).toBe('results');
        });

        it('should auto-set activeTab to results when response has hits', () => {
            spectator.service.runSearch();
            expect(spectator.service.activeTab()).toBe('results');
        });

        it('should auto-set activeTab to aggregations when response has no hits but has aggregations', () => {
            const searchService = spectator.inject(DotEsSearchService);
            (searchService.search as jest.Mock).mockReturnValueOnce(of(MOCK_AGG_RESPONSE));

            spectator.service.runSearch();
            expect(spectator.service.activeTab()).toBe('aggregations');
        });

        it('should reset activeTab to results at start of each run', () => {
            spectator.service.setActiveTab('raw');
            spectator.service.runSearch();
            expect(spectator.service.activeTab()).toBe('results');
        });

        it('should forward live=true (default) to search service', () => {
            const searchService = spectator.inject(DotEsSearchService);
            spectator.service.runSearch();
            expect(searchService.search).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ live: true })
            );
        });

        it('should forward live=false to search service when param is set', () => {
            const searchService = spectator.inject(DotEsSearchService);
            spectator.service.setParam('live', false);
            spectator.service.runSearch();
            expect(searchService.search).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({ live: false })
            );
        });
    });

    describe('aggregations computed', () => {
        let searchMock: jest.Mock;

        beforeEach(() => {
            searchMock = spectator.inject(DotEsSearchService).search as jest.Mock;
            searchMock.mockReturnValue(of(MOCK_RESPONSE));
        });

        it('should return null when no aggregations in response', () => {
            spectator.service.runSearch();
            expect(spectator.service.aggregations()).toBeNull();
        });

        it('should return aggregations map when present', () => {
            searchMock.mockReturnValueOnce(of(MOCK_AGG_RESPONSE));

            spectator.service.runSearch();
            expect(spectator.service.aggregations()).toEqual(
                MOCK_AGG_RESPONSE.esresponse[0].aggregations
            );
        });

        it('should report hasAggregations as true when aggregations exist', () => {
            searchMock.mockReturnValueOnce(of(MOCK_AGG_RESPONSE));

            spectator.service.runSearch();
            expect(spectator.service.hasAggregations()).toBe(true);
        });

        it('should report hasAggregations as false when no aggregations', () => {
            spectator.service.runSearch();
            expect(spectator.service.hasAggregations()).toBe(false);
        });
    });

    describe('suggestions computed', () => {
        let searchMock: jest.Mock;

        beforeEach(() => {
            searchMock = spectator.inject(DotEsSearchService).search as jest.Mock;
            searchMock.mockReturnValue(of(MOCK_RESPONSE));
        });

        it('should return null when no suggest in response', () => {
            spectator.service.runSearch();
            expect(spectator.service.suggestions()).toBeNull();
        });

        it('should return suggest map when present', () => {
            searchMock.mockReturnValueOnce(of(MOCK_SUGGEST_RESPONSE));

            spectator.service.runSearch();
            expect(spectator.service.suggestions()).toEqual(
                MOCK_SUGGEST_RESPONSE.esresponse[0].suggest
            );
        });

        it('should report hasSuggestions as true when suggest exists', () => {
            searchMock.mockReturnValueOnce(of(MOCK_SUGGEST_RESPONSE));

            spectator.service.runSearch();
            expect(spectator.service.hasSuggestions()).toBe(true);
        });

        it('should report hasSuggestions as false when no suggest', () => {
            spectator.service.runSearch();
            expect(spectator.service.hasSuggestions()).toBe(false);
        });
    });

    describe('returnedCount computed', () => {
        it('should be 0 before any search', () => {
            expect(spectator.service.returnedCount()).toBe(0);
        });

        it('should equal the number of hits returned in the response', () => {
            // MOCK_RESPONSE has 1 hit in hits.hits
            spectator.service.runSearch();
            expect(spectator.service.returnedCount()).toBe(1);
        });

        it('should cap at MAX_HITS when response contains more', () => {
            const searchService = spectator.inject(DotEsSearchService);
            const manyHits = Array.from({ length: MAX_HITS + 10 }, (_, i) => ({
                _id: String(i),
                _index: 'live',
                _type: 'content',
                _score: 1,
                _source: {}
            }));
            (searchService.search as jest.Mock).mockReturnValueOnce(
                of({
                    ...MOCK_RESPONSE,
                    esresponse: [
                        {
                            ...MOCK_RESPONSE.esresponse[0],
                            hits: { total: MAX_HITS + 10, hits: manyHits }
                        }
                    ]
                })
            );
            spectator.service.runSearch();
            expect(spectator.service.returnedCount()).toBe(MAX_HITS);
        });
    });

    describe('hasPartialResults computed', () => {
        it('should be false before any search', () => {
            expect(spectator.service.hasPartialResults()).toBe(false);
        });

        it('should be false when returned count equals total', () => {
            // MOCK_RESPONSE: 1 hit returned, total = 10 → 1 < 10 → true
            // We need a response where returned === total
            const searchService = spectator.inject(DotEsSearchService);
            (searchService.search as jest.Mock).mockReturnValueOnce(
                of({
                    ...MOCK_RESPONSE,
                    esresponse: [
                        {
                            ...MOCK_RESPONSE.esresponse[0],
                            hits: {
                                total: 1,
                                hits: [MOCK_RESPONSE.esresponse[0].hits.hits[0]]
                            }
                        }
                    ]
                })
            );
            spectator.service.runSearch();
            expect(spectator.service.hasPartialResults()).toBe(false);
        });

        it('should be true when fewer hits are returned than the total', () => {
            // MOCK_RESPONSE: 1 hit returned, total = 10
            spectator.service.runSearch();
            expect(spectator.service.hasPartialResults()).toBe(true);
        });

        it('should be true when size limits results below total (object total)', () => {
            const searchService = spectator.inject(DotEsSearchService);
            (searchService.search as jest.Mock).mockReturnValueOnce(
                of({
                    ...MOCK_RESPONSE,
                    esresponse: [
                        {
                            ...MOCK_RESPONSE.esresponse[0],
                            hits: {
                                total: { value: 10000, relation: 'eq' },
                                hits: [MOCK_RESPONSE.esresponse[0].hits.hits[0]]
                            }
                        }
                    ]
                })
            );
            spectator.service.runSearch();
            expect(spectator.service.hasPartialResults()).toBe(true);
        });
    });

    describe('rawJson computed', () => {
        it('should build rawJson from the search response', () => {
            spectator.service.runSearch();
            expect(spectator.service.rawJson()).toContain('"took": 55');
        });

        it('should return empty string before any search', () => {
            expect(spectator.service.rawJson()).toBe('');
        });
    });

    describe('hasLoadedResults computed', () => {
        it('should be false before any search', () => {
            expect(spectator.service.hasLoadedResults()).toBe(false);
        });

        it('should be false when search returns empty contentlets', () => {
            // MOCK_RESPONSE.contentlets is []
            spectator.service.runSearch();
            expect(spectator.service.hasLoadedResults()).toBe(false);
        });

        it('should be true when search returns at least one contentlet', () => {
            const searchService = spectator.inject(DotEsSearchService);
            (searchService.search as jest.Mock).mockReturnValueOnce(
                of({ ...MOCK_RESPONSE, contentlets: [{ identifier: 'abc', title: 'Test' }] })
            );
            spectator.service.runSearch();
            expect(spectator.service.hasLoadedResults()).toBe(true);
        });

        it('should be false after an error', () => {
            const searchService = spectator.inject(DotEsSearchService);
            (searchService.search as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('fail'))
            );
            spectator.service.runSearch();
            expect(spectator.service.hasLoadedResults()).toBe(false);
        });
    });

    describe('queryWasCapped', () => {
        it('should be false initially', () => {
            expect(spectator.service.queryWasCapped()).toBe(false);
        });

        it('should cap size to MAX_HITS and set queryWasCapped when query size exceeds the limit', () => {
            const oversizedQuery = JSON.stringify({ query: { match_all: {} }, size: MAX_HITS + 1 });
            spectator.service.setQuery(oversizedQuery);
            spectator.service.runSearch();

            expect(spectator.service.queryWasCapped()).toBe(true);
            const rewritten = JSON.parse(spectator.service.query());
            expect(rewritten['size']).toBe(MAX_HITS);
        });

        it('should not set queryWasCapped when size is within the limit', () => {
            const normalQuery = JSON.stringify({ query: { match_all: {} }, size: 10 });
            spectator.service.setQuery(normalQuery);
            spectator.service.runSearch();

            expect(spectator.service.queryWasCapped()).toBe(false);
        });

        it('should reset queryWasCapped at the start of each run', () => {
            const oversizedQuery = JSON.stringify({ query: { match_all: {} }, size: MAX_HITS + 1 });
            spectator.service.setQuery(oversizedQuery);
            spectator.service.runSearch();
            expect(spectator.service.queryWasCapped()).toBe(true);

            const normalQuery = JSON.stringify({ query: { match_all: {} }, size: 10 });
            spectator.service.setQuery(normalQuery);
            spectator.service.runSearch();
            expect(spectator.service.queryWasCapped()).toBe(false);
        });
    });

    describe('setActiveTab()', () => {
        it('should update activeTab signal', () => {
            spectator.service.setActiveTab('raw');
            expect(spectator.service.activeTab()).toBe('raw');
        });
    });
});

describe('DotEsSearchStore (admin user)', () => {
    let adminSpectator: SpectatorService<InstanceType<typeof DotEsSearchStore>>;

    const createAdminService = createServiceFactory({
        service: DotEsSearchStore,
        providers: [
            mockProvider(DotEsSearchService, {
                search: jest.fn().mockReturnValue(of(MOCK_RESPONSE))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: true }))
            })
        ]
    });

    beforeEach(() => {
        adminSpectator = createAdminService();
        adminSpectator.flushEffects();
    });

    it('isAdmin() should be true when current user is admin', () => {
        expect(adminSpectator.service.isAdmin()).toBe(true);
    });
});
