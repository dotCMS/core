import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotEsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotEsSearchStore } from './dot-es-search.store';

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
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') })
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
            spectator.service.setParam('depth', 2);
            expect(spectator.service.params().live).toBe(before.live);
            expect(spectator.service.params().depth).toBe(2);
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
            expect(errorManager.handle).toHaveBeenCalled();
            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
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

    describe('rawJson computed', () => {
        it('should build rawJson from the search response', () => {
            spectator.service.runSearch();
            expect(spectator.service.rawJson()).toContain('"took": 55');
        });

        it('should return empty string before any search', () => {
            expect(spectator.service.rawJson()).toBe('');
        });
    });

    describe('setActiveTab()', () => {
        it('should update activeTab signal', () => {
            spectator.service.setActiveTab('raw');
            expect(spectator.service.activeTab()).toBe('raw');
        });
    });
});
