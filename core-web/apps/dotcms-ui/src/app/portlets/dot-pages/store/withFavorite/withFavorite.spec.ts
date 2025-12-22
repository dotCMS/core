import { describe, expect, it, beforeEach, jest } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet, ESContent, DotSite } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withFavorites } from './withFavorite';

import { DotPageListService, ListPagesParams } from '../../services/dot-page-list.service';
import { DotCMSPagesPortletState } from '../store';

const initialFilters: ListPagesParams = {
    search: '',
    sort: 'modDate DESC',
    limit: 40,
    offset: 0,
    languageId: null,
    host: '',
    archived: false
};

const initialState: DotCMSPagesPortletState = {
    pages: [],
    filters: initialFilters,
    pagination: {
        currentPage: 1,
        perPage: 40,
        totalEntries: 0
    },
    bundleDialog: {
        show: false,
        pageIdentifier: ''
    },
    languages: [],
    currentUser: null,
    status: 'idle'
};

const mockContentlet = (partial: Partial<DotCMSContentlet>): DotCMSContentlet =>
    partial as unknown as DotCMSContentlet;

const MOCK_FAVORITES: DotCMSContentlet[] = [
    mockContentlet({ identifier: 'page-1', title: 'Home', url: '/home' }),
    mockContentlet({ identifier: 'page-2', title: 'About', url: '/about' })
];

const MOCK_ES_CONTENT: ESContent = {
    contentTook: 0,
    queryTook: 0,
    resultsSize: MOCK_FAVORITES.length,
    jsonObjectView: { contentlets: MOCK_FAVORITES }
};

// Mirror the pattern used in withLock.spec.ts: build a small store just for the feature under test.
export const pagesStoreWithFavoritesMock = signalStore(
    { protectedState: false },
    withState<DotCMSPagesPortletState>(initialState),
    withFavorites()
);

describe('withFavorites', () => {
    let spectator: SpectatorService<InstanceType<typeof pagesStoreWithFavoritesMock>>;
    let store: InstanceType<typeof pagesStoreWithFavoritesMock>;
    let dotPageListService: jest.Mocked<
        Pick<DotPageListService, 'getFavoritePages' | 'getSinglePage'>
    >;
    let httpErrorManagerService: jest.Mocked<Pick<DotHttpErrorManagerService, 'handle'>>;

    const siteDetailsSig = signal<DotSite | null>(null);
    const loggedUserMock = jest.fn(() => ({ userId: 'user-1' }) as unknown);

    const createService = createServiceFactory({
        service: pagesStoreWithFavoritesMock,
        providers: [
            {
                provide: DotPageListService,
                useValue: {
                    getFavoritePages: jest.fn().mockReturnValue(of(MOCK_ES_CONTENT)),
                    getSinglePage: jest.fn().mockReturnValue(of(MOCK_FAVORITES[0]))
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: {
                    handle: jest.fn()
                }
            },
            {
                provide: GlobalStore,
                useValue: {
                    siteDetails: siteDetailsSig,
                    loggedUser: loggedUserMock
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;

        dotPageListService = spectator.inject(DotPageListService) as unknown as jest.Mocked<
            Pick<DotPageListService, 'getFavoritePages' | 'getSinglePage'>
        >;
        httpErrorManagerService = spectator.inject(
            DotHttpErrorManagerService
        ) as unknown as jest.Mocked<Pick<DotHttpErrorManagerService, 'handle'>>;

        // Reset base state between tests; keep the feature defaults.
        patchState(store, initialState);
        patchState(store, { favoritePages: [], favoriteState: 'loading' });

        siteDetailsSig.set(null);
        loggedUserMock.mockClear();
        (dotPageListService.getFavoritePages as jest.Mock).mockClear();
        (dotPageListService.getSinglePage as jest.Mock).mockClear();
        (httpErrorManagerService.handle as jest.Mock).mockClear();
    });

    it('should initialize favorite state', () => {
        expect(store.favoritePages()).toEqual([]);
        expect(store.favoriteState()).toBe('loading');
        expect(store.$isFavoritePagesLoading()).toBe(true);
    });

    it('should compute $isFavoritePagesLoading based on favoriteState', () => {
        patchState(store, { favoriteState: 'loaded' });
        expect(store.$isFavoritePagesLoading()).toBe(false);

        patchState(store, { favoriteState: 'loading' });
        expect(store.$isFavoritePagesLoading()).toBe(true);
    });

    it('getFavoritePages() should fetch favorites, update favoritePages and set favoriteState to loaded', () => {
        dotPageListService.getFavoritePages.mockReturnValue(of(MOCK_ES_CONTENT));

        store.getFavoritePages({ host: 'demo.dotcms.com' });

        expect(dotPageListService.getFavoritePages).toHaveBeenCalledTimes(1);
        const [params, userId] = dotPageListService.getFavoritePages.mock.calls[0];
        expect(userId).toBe('user-1');
        expect(params).toEqual({
            ...initialFilters,
            host: 'demo.dotcms.com'
        });

        expect(store.favoritePages()).toEqual(MOCK_FAVORITES);
        expect(store.favoriteState()).toBe('loaded');
        expect(store.$isFavoritePagesLoading()).toBe(false);
    });

    it('getFavoritePages() should use fallback userId when loggedUser is null', () => {
        loggedUserMock.mockReturnValueOnce(null as unknown);

        store.getFavoritePages();

        const [, userId] = dotPageListService.getFavoritePages.mock.calls[0];
        expect(userId).toBe('dotcms.org.1');
    });

    it('getFavoritePages() should call httpErrorManagerService.handle(error) and set favoriteState=error when request fails', () => {
        const error = new Error('Favorites failed');
        dotPageListService.getFavoritePages.mockReturnValueOnce(throwError(error));

        store.getFavoritePages();

        expect(httpErrorManagerService.handle).toHaveBeenCalledWith(error);
        expect(store.favoriteState()).toBe('error');
        expect(store.$isFavoritePagesLoading()).toBe(false);
    });

    it('updateFavoritePageNode() should replace the matching favorite page with the updated one', () => {
        const current = [
            mockContentlet({ identifier: 'page-1', title: 'Home', url: '/home' }),
            mockContentlet({ identifier: 'page-2', title: 'About', url: '/about' })
        ];
        patchState(store, { favoritePages: current, favoriteState: 'loaded' });

        const updated = mockContentlet({
            identifier: 'page-2',
            title: 'About (updated)',
            url: '/about'
        });
        dotPageListService.getSinglePage.mockReturnValue(of(updated));

        store.updateFavoritePageNode('page-2');

        expect(dotPageListService.getSinglePage).toHaveBeenCalledWith('page-2');
        expect(store.favoritePages()).toEqual([current[0], updated]);
    });

    it('updateFavoritePageNode() should call httpErrorManagerService.handle(error) when request fails', () => {
        const current = [
            mockContentlet({ identifier: 'page-1', title: 'Home', url: '/home' }),
            mockContentlet({ identifier: 'page-2', title: 'About', url: '/about' })
        ];
        patchState(store, { favoritePages: current, favoriteState: 'loaded' });

        const error = new Error('Single page failed');
        dotPageListService.getSinglePage.mockReturnValueOnce(throwError(error));

        store.updateFavoritePageNode('page-2');

        expect(httpErrorManagerService.handle).toHaveBeenCalledWith(error);
        expect(store.favoritePages()).toEqual(current);
    });
});
