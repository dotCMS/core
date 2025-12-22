import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { Subject, of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentlet, ESContent, SiteEntity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotCMSPagesStore } from './store';

import { DotPageListService, ListPagesParams } from '../services/dot-page-list.service';

const mockPage = (partial: Partial<DotCMSContentlet>): DotCMSContentlet =>
    partial as unknown as DotCMSContentlet;

const createESResponse = (contentlets: DotCMSContentlet[], resultsSize = contentlets.length) =>
    ({
        contentTook: 0,
        queryTook: 0,
        resultsSize,
        jsonObjectView: { contentlets }
    }) as ESContent;

describe('DotCMSPagesStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotCMSPagesStore>>;
    let store: InstanceType<typeof DotCMSPagesStore>;
    let dotPageListService: jest.Mocked<
        Pick<DotPageListService, 'getPages' | 'getSinglePage' | 'getFavoritePages'>
    >;
    let httpErrorManagerService: jest.Mocked<Pick<DotHttpErrorManagerService, 'handle'>>;

    const siteDetailsSig = signal<SiteEntity | null>(null);
    const loggedUserMock = jest.fn(() => ({ userId: 'user-1' }) as unknown);

    const createService = createServiceFactory({
        service: DotCMSPagesStore,
        providers: [
            {
                provide: DotPageListService,
                useValue: {
                    getPages: jest.fn(),
                    getSinglePage: jest.fn(),
                    // Included to satisfy the injected feature; we explicitly do NOT test it here.
                    getFavoritePages: jest.fn().mockReturnValue(of(createESResponse([])))
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
                    // Keep the hooks inert in these tests (we do NOT test hooks here).
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
            Pick<DotPageListService, 'getPages' | 'getSinglePage' | 'getFavoritePages'>
        >;
        httpErrorManagerService = spectator.inject(
            DotHttpErrorManagerService
        ) as unknown as jest.Mocked<Pick<DotHttpErrorManagerService, 'handle'>>;

        siteDetailsSig.set(null);
        loggedUserMock.mockClear();
        dotPageListService.getPages.mockReset();
        dotPageListService.getSinglePage.mockReset();
        (httpErrorManagerService.handle as jest.Mock).mockClear();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Initial state', () => {
        it('should initialize with defaults', () => {
            expect(store.pages()).toEqual([]);
            expect(store.pagination()).toEqual({
                currentPage: 1,
                perPage: 40,
                totalEntries: 0
            });
            expect(store.filters()).toEqual({
                search: '',
                sort: 'modDate DESC',
                limit: 40,
                languageId: null,
                archived: false,
                offset: 0,
                host: ''
            });
            expect(store.bundleDialog()).toEqual({ show: false, pageIdentifier: '' });
            expect(store.languages()).toEqual([]);
            expect(store.currentUser()).toBeNull();
            expect(store.status()).toBe('loading');
        });
    });

    describe('Computed properties', () => {
        it('$totalRecords should reflect pagination.totalEntries', () => {
            dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([], 123)));
            store.getPages();
            expect(store.pagination().totalEntries).toBe(123);
            expect(store.$totalRecords()).toBe(123);
        });

        it('$showBundleDialog should reflect bundleDialog.show', () => {
            expect(store.$showBundleDialog()).toBe(false);
            store.showBundleDialog('page-1');
            expect(store.$showBundleDialog()).toBe(true);
        });

        it('$assetIdentifier should reflect bundleDialog.pageIdentifier', () => {
            expect(store.$assetIdentifier()).toBe('');
            store.showBundleDialog('page-123');
            expect(store.$assetIdentifier()).toBe('page-123');
        });

        it('$isPagesLoading should reflect status', () => {
            // initial state is loading
            expect(store.$isPagesLoading()).toBe(true);

            dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));
            store.getPages();
            expect(store.status()).toBe('loaded');
            expect(store.$isPagesLoading()).toBe(false);
        });
    });

    describe('Methods', () => {
        describe('getPages', () => {
            it('should set status=loading, update filters, call service, then set pages/pagination/status=loaded', () => {
                const response$ = new Subject<ESContent>();
                dotPageListService.getPages.mockReturnValueOnce(response$.asObservable());

                store.getPages({ search: 'hello', offset: 80 });

                // Before emission
                expect(store.status()).toBe('loading');
                expect(store.filters()).toEqual(
                    expect.objectContaining({
                        search: 'hello',
                        offset: 80
                    })
                );

                expect(dotPageListService.getPages).toHaveBeenCalledTimes(1);
                const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
                expect(params).toEqual(
                    expect.objectContaining({
                        search: 'hello',
                        offset: 80,
                        limit: 40
                    })
                );

                const pages = [mockPage({ identifier: 'p1', title: 'P1' })];
                response$.next(createESResponse(pages, 99));
                response$.complete();

                // After emission
                expect(store.pages()).toEqual(pages);
                expect(store.pagination()).toEqual({
                    currentPage: 3, // floor(80/40)+1
                    perPage: 40,
                    totalEntries: 99
                });
                expect(store.status()).toBe('loaded');
            });

            it('should set status=error and call httpErrorManagerService.handle(error) when request fails', () => {
                const error = new Error('Pages failed');
                dotPageListService.getPages.mockReturnValueOnce(throwError(error));

                store.getPages({ search: 'x' });

                expect(httpErrorManagerService.handle).toHaveBeenCalledWith(error);
                expect(store.status()).toBe('error');
                expect(store.$isPagesLoading()).toBe(false);
            });

            it('should derive currentPage based on offset/limit', () => {
                const response$ = new Subject<ESContent>();
                dotPageListService.getPages.mockReturnValueOnce(response$.asObservable());

                store.getPages({ limit: 10, offset: 25 });
                response$.next(createESResponse([], 0));
                response$.complete();

                expect(store.pagination().currentPage).toBe(3); // floor(25/10)+1
                expect(store.pagination().perPage).toBe(10);
            });
        });

        it('searchPages should reset offset to 0 and set search', () => {
            dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

            store.searchPages('abc');

            const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
            expect(params.search).toBe('abc');
            expect(params.offset).toBe(0);
        });

        it('filterByLanguage should reset offset to 0 and set languageId', () => {
            dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

            store.filterByLanguage(2);

            const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
            expect(params.languageId).toBe(2);
            expect(params.offset).toBe(0);
        });

        it('filterByArchived should reset offset to 0 and set archived', () => {
            dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

            store.filterByArchived(true);

            const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
            expect(params.archived).toBe(true);
            expect(params.offset).toBe(0);
        });

        describe('onLazyLoad', () => {
            it('should compute offset and sort from event (ASC)', () => {
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

                store.onLazyLoad({ first: 40, sortField: 'modDate', sortOrder: 1 });

                const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
                expect(params.offset).toBe(40);
                expect(params.sort).toBe('modDate ASC');
            });

            it('should compute sort DESC when sortOrder is not 1', () => {
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

                store.onLazyLoad({ first: 0, sortField: 'modDate', sortOrder: -1 });

                const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
                expect(params.sort).toBe('modDate DESC');
            });

            it('should fallback to "title ASC" when sortField is missing', () => {
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

                store.onLazyLoad({ first: 0 });

                const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
                expect(params.sort).toBe('title ASC');
            });

            it('should clamp offset to 0 when first is negative/undefined', () => {
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([])));

                store.onLazyLoad({ first: -10, sortField: 'title', sortOrder: 1 });

                const [params] = dotPageListService.getPages.mock.calls[0] as [ListPagesParams];
                expect(params.offset).toBe(0);
            });
        });

        describe('updatePageNode', () => {
            it('should replace the matching page with the updated one', () => {
                const p1 = mockPage({ identifier: 'page-1', title: 'Home' });
                const p2 = mockPage({ identifier: 'page-2', title: 'About' });
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([p1, p2], 2)));
                store.getPages();

                const updated = mockPage({ identifier: 'page-2', title: 'About (updated)' });
                dotPageListService.getSinglePage.mockReturnValueOnce(of(updated));

                store.updatePageNode('page-2');

                expect(dotPageListService.getSinglePage).toHaveBeenCalledWith('page-2');
                expect(store.pages()).toEqual([p1, updated]);
            });

            it('should call httpErrorManagerService.handle(error) when request fails', () => {
                const p1 = mockPage({ identifier: 'page-1', title: 'Home' });
                const p2 = mockPage({ identifier: 'page-2', title: 'About' });
                dotPageListService.getPages.mockReturnValueOnce(of(createESResponse([p1, p2], 2)));
                store.getPages();

                const error = new Error('Single page failed');
                dotPageListService.getSinglePage.mockReturnValueOnce(throwError(error));

                store.updatePageNode('page-2');

                expect(httpErrorManagerService.handle).toHaveBeenCalledWith(error);
                expect(store.pages()).toEqual([p1, p2]);
            });
        });

        describe('bundle dialog methods', () => {
            it('showBundleDialog should set bundleDialog and computeds', () => {
                store.showBundleDialog('page-9');
                expect(store.bundleDialog()).toEqual({ show: true, pageIdentifier: 'page-9' });
                expect(store.$showBundleDialog()).toBe(true);
                expect(store.$assetIdentifier()).toBe('page-9');
            });

            it('hideBundleDialog should reset bundleDialog and computeds', () => {
                store.showBundleDialog('page-9');
                store.hideBundleDialog();
                expect(store.bundleDialog()).toEqual({ show: false, pageIdentifier: '' });
                expect(store.$showBundleDialog()).toBe(false);
                expect(store.$assetIdentifier()).toBe('');
            });
        });
    });
});
