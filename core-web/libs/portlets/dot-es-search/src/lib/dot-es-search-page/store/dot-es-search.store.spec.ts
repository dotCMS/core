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
            timed_out: false,
            aggregations: { contentType: { buckets: [{ key: 'Blog', doc_count: 10 }] } },
            suggest: { title_suggest: [{ options: [{ text: 'Blog' }] }] }
        }
    ]
};

const MOCK_RAW_RESPONSE = {
    hits: { total: 10, hits: [] },
    took: 55,
    timed_out: false,
    _shards: { total: 5, successful: 5, skipped: 0, failed: 0 }
};

describe('DotEsSearchStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEsSearchStore>>;

    const createService = createServiceFactory({
        service: DotEsSearchStore,
        providers: [
            mockProvider(DotEsSearchService, {
                search: jest.fn().mockReturnValue(of(MOCK_RESPONSE)),
                searchRaw: jest.fn().mockReturnValue(of(MOCK_RAW_RESPONSE))
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
        expect(spectator.service.rawResponse()).toBeNull();
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

        it('should expose hits via computed', () => {
            spectator.service.runSearch();
            expect(spectator.service.hits()).toEqual(MOCK_RESPONSE.esresponse[0].hits.hits);
            expect(spectator.service.hitCount()).toBe(10);
        });

        it('should expose aggregations via computed', () => {
            spectator.service.runSearch();
            expect(spectator.service.aggregations()).toEqual(
                MOCK_RESPONSE.esresponse[0].aggregations
            );
        });

        it('should expose suggestions via computed', () => {
            spectator.service.runSearch();
            expect(spectator.service.suggestions()).toEqual(MOCK_RESPONSE.esresponse[0].suggest);
        });

        it('should call httpErrorManager.handle and set ERROR status on failure', () => {
            const searchService = spectator.inject(DotEsSearchService);
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(searchService, 'search').mockReturnValue(
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
    });

    describe('loadRaw()', () => {
        it('should populate rawResponse on success', () => {
            spectator.service.loadRaw();
            expect(spectator.service.rawResponse()).toEqual(MOCK_RAW_RESPONSE);
        });

        it('should build rawJson computed from rawResponse', () => {
            spectator.service.loadRaw();
            expect(spectator.service.rawJson()).toContain('"took": 55');
        });

        it('should call httpErrorManager.handle on raw failure', () => {
            const searchService = spectator.inject(DotEsSearchService);
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(searchService, 'searchRaw').mockReturnValue(
                throwError(() => new Error('raw fail'))
            );

            spectator.service.loadRaw();
            expect(errorManager.handle).toHaveBeenCalled();
        });
    });

    describe('setActiveTab()', () => {
        it('should update activeTab signal', () => {
            spectator.service.setActiveTab('raw');
            expect(spectator.service.activeTab()).toBe('raw');
        });
    });
});
