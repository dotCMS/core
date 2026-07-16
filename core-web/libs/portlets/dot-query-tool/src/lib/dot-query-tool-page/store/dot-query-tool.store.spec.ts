import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { EMPTY, of, throwError } from 'rxjs';

import {
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    DEFAULT_LIMIT,
    DEFAULT_OFFSET,
    DotQueryToolStore,
    MAX_RESULTS
} from './dot-query-tool.store';

import { DotQueryToolService } from '../../services/dot-query-tool.service';

const MOCK_RESPONSE = {
    resultsSize: 3,
    queryTook: 12,
    contentTook: 34,
    jsonObjectView: {
        contentlets: [
            { inode: 'a', title: 'A', contentType: 'Blog', identifier: 'id-a' } as never,
            { inode: 'b', title: 'B', contentType: 'Blog', identifier: 'id-b' } as never
        ]
    }
};

describe('DotQueryToolStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotQueryToolStore>>;
    let searchSpy: jest.Mock;

    const createService = createServiceFactory({
        service: DotQueryToolStore,
        providers: [
            mockProvider(DotQueryToolService, {
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
        spectator = createService();
        spectator.flushEffects();
        searchSpy = spectator.inject(DotQueryToolService).search as jest.Mock;
        searchSpy.mockClear();
        searchSpy.mockReturnValue(of(MOCK_RESPONSE));
    });

    it('initialises with INIT status and default pagination', () => {
        expect(spectator.service.status()).toBe(ComponentStatus.INIT);
        expect(spectator.service.response()).toBeNull();
        expect(spectator.service.offset()).toBe(DEFAULT_OFFSET);
        expect(spectator.service.limit()).toBe(DEFAULT_LIMIT);
    });

    it('hydrates isAdmin from the current user service', () => {
        expect(spectator.service.isAdmin()).toBe(true);
    });

    describe('setters', () => {
        it('updates query', () => {
            spectator.service.setQuery('+live:true');
            expect(spectator.service.query()).toBe('+live:true');
        });

        it('clamps negative offsets to 0', () => {
            spectator.service.setOffset(-50);
            expect(spectator.service.offset()).toBe(0);
        });

        it('clamps limit below 1 to 1', () => {
            spectator.service.setLimit(0);
            expect(spectator.service.limit()).toBe(1);
        });
    });

    describe('runSearch', () => {
        it('sends the current state and stores the response', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.setSort('modDate desc');
            spectator.service.setOffset(20);
            spectator.service.setLimit(10);

            spectator.service.runSearch();

            expect(searchSpy).toHaveBeenCalledWith({
                query: '+live:true',
                sort: 'modDate desc',
                offset: 20,
                limit: 10
            });
            expect(spectator.service.status()).toBe(ComponentStatus.LOADED);
            expect(spectator.service.response()).toEqual(MOCK_RESPONSE);
            expect(spectator.service.resultsSize()).toBe(3);
        });

        it('forwards userId when set (server enforces admin gate)', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.setUserId('admin@dotcms.com');

            spectator.service.runSearch();

            expect(searchSpy).toHaveBeenCalledWith(
                expect.objectContaining({ userId: 'admin@dotcms.com' })
            );
        });

        it('omits userId when blank', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.runSearch();
            const payload = searchSpy.mock.calls[0][0];
            expect(payload.userId).toBeUndefined();
        });

        it('snaps limit to MAX_RESULTS and sets limitWasCapped when user runs an oversized search', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.setLimit(5000);
            spectator.service.runSearch();
            expect(spectator.service.limit()).toBe(MAX_RESULTS);
            expect(spectator.service.limitWasCapped()).toBe(true);
            expect(searchSpy.mock.calls[0][0].limit).toBe(MAX_RESULTS);
        });

        it('does not flip limitWasCapped until a search actually runs', () => {
            spectator.service.setLimit(MAX_RESULTS + 1);
            expect(spectator.service.limitWasCapped()).toBe(false);
        });

        it('keeps limitWasCapped true after the user edits the limit, until the next runSearch', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.setLimit(5000);
            spectator.service.runSearch();
            expect(spectator.service.limitWasCapped()).toBe(true);
            // User edits the input; the warning must still describe the displayed results.
            spectator.service.setLimit(50);
            expect(spectator.service.limitWasCapped()).toBe(true);
            // A fresh non-capped run clears the warning.
            spectator.service.runSearch();
            expect(spectator.service.limitWasCapped()).toBe(false);
        });

        it('clears limitWasCapped synchronously on runSearch (no fade behind the loading state)', () => {
            // Seed a capped state, then verify a fresh runSearch hides the warning before
            // the response arrives — observed via a deferred search observable.
            spectator.service.setQuery('+live:true');
            spectator.service.setLimit(5000);
            spectator.service.runSearch();
            expect(spectator.service.limitWasCapped()).toBe(true);

            // Issue a non-capped run; mock the service to never resolve so we can
            // observe the in-flight state.
            spectator.service.setLimit(50);
            searchSpy.mockReturnValueOnce(EMPTY);
            spectator.service.runSearch();
            expect(spectator.service.limitWasCapped()).toBe(false);
            expect(spectator.service.status()).toBe(ComponentStatus.LOADING);
        });

        it('leaves limit alone and keeps limitWasCapped false when under MAX_RESULTS', () => {
            spectator.service.setQuery('+live:true');
            spectator.service.setLimit(50);
            spectator.service.runSearch();
            expect(spectator.service.limit()).toBe(50);
            expect(spectator.service.limitWasCapped()).toBe(false);
        });

        it('routes errors through DotHttpErrorManagerService and sets ERROR status', () => {
            const error = { status: 500 } as unknown;
            searchSpy.mockReturnValueOnce(throwError(() => error));
            const handler = spectator.inject(DotHttpErrorManagerService).handle as jest.Mock;

            spectator.service.setQuery('+live:true');
            spectator.service.runSearch();

            expect(handler).toHaveBeenCalledWith(error);
            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
        });
    });

    describe('computeds', () => {
        beforeEach(() => {
            spectator.service.setQuery('+live:true');
            spectator.service.runSearch();
        });

        it('rawJson renders the response as pretty JSON', () => {
            expect(spectator.service.rawJson()).toContain('"resultsSize"');
        });

        it('showingFrom is offset+1 when there are hits', () => {
            spectator.service.setOffset(10);
            spectator.service.runSearch();
            expect(spectator.service.showingFrom()).toBe(3);
        });

        it('hasLoadedResults reflects loaded state plus non-empty contentlets', () => {
            expect(spectator.service.hasLoadedResults()).toBe(true);
        });
    });
});
