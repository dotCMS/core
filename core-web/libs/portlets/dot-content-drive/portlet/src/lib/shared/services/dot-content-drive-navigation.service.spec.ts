import { describe, expect, it } from '@jest/globals';
import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { Router } from '@angular/router';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotContentDriveNavigationService } from './dot-content-drive-navigation.service';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * Builds a mock store exposing what the nav service reads: the `flags()` slice, plus the active
 * language filter and the environment default used to resolve an `editContent` deep link.
 */
const mockStoreWithFlag = (enabled: boolean) =>
    mockProvider(DotContentDriveStore, {
        flags: signal({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: enabled }),
        getFilterValue: jest.fn().mockReturnValue(undefined),
        defaultLanguageId: jest.fn().mockReturnValue(undefined)
    });

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: jest.Mocked<Router>;
    let contentTypeService: jest.Mocked<DotContentTypeService>;
    let dotRouterService: jest.Mocked<DotRouterService>;
    let location: SpyObject<Location>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;
    let contentSearch: jest.Mocked<DotContentSearchService>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, {
                navigate: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn()
            }),
            mockProvider(DotRouterService, {
                goToEditPage: jest.fn()
            }),
            mockProvider(Location, {
                path: jest.fn()
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotContentSearchService, {
                get: jest.fn()
            }),
            // Side panel feature flag ON by default (read from the store's flags slice) so the
            // side-panel tests below apply; the "side panel disabled" block re-creates it off.
            mockStoreWithFlag(true)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);
        contentTypeService = spectator.inject(DotContentTypeService);
        dotRouterService = spectator.inject(DotRouterService);
        location = spectator.inject(Location);
        httpErrorManager = spectator.inject(DotHttpErrorManagerService);
        contentSearch = spectator.inject(DotContentSearchService);
        store = spectator.inject(DotContentDriveStore, true);
    });

    afterEach(() => {
        jest.clearAllMocks();
        location.path.mockReset();
    });

    describe('editContent', () => {
        it('should navigate to page editor when baseType is htmlpageasset', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/test-page',
                languageId: 1
            });

            service.editContent(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/test-page',
                language_id: 1
            });
        });

        it('should use url property when urlMap is not available for Pages contentlet', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                url: '/test-page-url',
                languageId: 2
            });

            service.editContent(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/test-page-url',
                language_id: 2
            });
        });

        it('should open the new editor side panel when feature flag is enabled', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'blog',
                inode: 'test-inode-123',
                identifier: 'test-identifier-123',
                title: 'My Blog Post'
            });

            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                contentletInode: 'test-inode-123',
                identifier: 'test-identifier-123',
                title: 'My Blog Post'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should navigate to old content editor when feature flag is disabled', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'news',
                inode: 'test-inode-456'
            });

            const mockContentType = createFakeContentType({
                id: 'news',
                name: 'News',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('news');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-456'], {
                queryParams: {}
            });
        });

        it('should navigate to old content editor with mapped query params from Content Drive', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'news',
                inode: 'test-inode-456'
            });

            const mockContentType = createFakeContentType({
                id: 'news',
                name: 'News',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
            });

            // Mock the location to return a URL without CD_ prefix
            location.path.mockReturnValue('/content-drive?folderId=123&path=/images');

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('news');
            // The params should be sent WITH CD_ prefix added by mapQueryParamsToCDParams
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-456'], {
                queryParams: {
                    CD_folderId: '123',
                    CD_path: '/images'
                }
            });
        });

        it('should navigate to old content editor when feature flag is missing', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'product',
                inode: 'test-inode-789'
            });

            const mockContentType = createFakeContentType({
                id: 'product',
                name: 'Product',
                metadata: {}
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('product');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-789'], {
                queryParams: {}
            });
        });

        it('should navigate to old content editor when metadata is undefined', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'event',
                inode: 'test-inode-000'
            });

            const mockContentType = createFakeContentType({ id: 'event', name: 'Event' });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('event');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-000'], {
                queryParams: {}
            });
        });

        it('should surface the error and not navigate when getContentType fails', () => {
            const error = new HttpErrorResponse({ status: 500 });
            const mockContentlet = createFakeContentlet({
                contentType: 'blog',
                inode: 'test-inode-123'
            });

            contentTypeService.getContentType.mockReturnValue(throwError(() => error));

            service.editContent(mockContentlet);

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(router.navigate).not.toHaveBeenCalled();
        });
    });

    describe('createContent', () => {
        it('should open the new editor side panel when feature flag is enabled and no folder given', () => {
            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('blog');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(service.$editPanelRequest()).toEqual({
                mode: 'new',
                contentTypeId: 'blog',
                folderPath: undefined,
                title: 'Blog'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should forward folderPath to the new editor side panel so it is created in the current folder', () => {
            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('blog', { folderPath: 'demo.dotcms.com/about-us/' });

            expect(service.$editPanelRequest()).toEqual({
                mode: 'new',
                contentTypeId: 'blog',
                folderPath: 'demo.dotcms.com/about-us/',
                title: 'Blog'
            });
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should navigate to legacy content editor with mapped CD_ params when feature flag is disabled', () => {
            const mockContentType = createFakeContentType({
                id: 'news',
                name: 'News',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
            });

            location.path.mockReturnValue('/content-drive?path=/foo&filters=bar');

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('news');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('news');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/new/news'], {
                queryParams: {
                    CD_path: '/foo',
                    CD_filters: 'bar'
                }
            });
        });

        it('should forward the folder inode to the legacy content editor alongside the CD_ params', () => {
            const mockContentType = createFakeContentType({
                id: 'news',
                name: 'News',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false }
            });

            location.path.mockReturnValue('/content-drive?path=/foo');

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('news', { folderInode: 'inode-1' });

            expect(router.navigate).toHaveBeenCalledWith(['c/content/new/news'], {
                queryParams: {
                    CD_path: '/foo',
                    folder: 'inode-1'
                }
            });
        });

        it('should navigate to legacy content editor with mapped CD_ params when feature flag is missing', () => {
            const mockContentType = createFakeContentType({
                id: 'product',
                name: 'Product',
                metadata: {}
            });

            location.path.mockReturnValue('/content-drive?path=/foo&filters=bar');

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('product');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('product');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/new/product'], {
                queryParams: {
                    CD_path: '/foo',
                    CD_filters: 'bar'
                }
            });
        });

        it('should surface the error and not navigate when getContentType fails', () => {
            const error = new HttpErrorResponse({ status: 500 });

            location.path.mockReturnValue('/content-drive?path=/foo');
            contentTypeService.getContentType.mockReturnValue(throwError(() => error));

            service.createContent('blog');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(router.navigate).not.toHaveBeenCalled();
        });
    });

    describe('editPage', () => {
        it('should navigate to edit page with urlMap when available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/about-us',
                url: '/fallback-url',
                languageId: 1
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/about-us',
                language_id: 1
            });
        });

        it('should navigate to edit page with url when urlMap is not available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                url: '/contact',
                languageId: 2
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/contact',
                language_id: 2
            });
        });

        it('should prefer urlMap over url when both are available', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/primary-url',
                url: '/secondary-url',
                languageId: 3
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/primary-url',
                language_id: 3
            });
        });

        it('should handle empty urlMap and fallback to url', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '',
                url: '/home',
                languageId: 1
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/home',
                language_id: 1
            });
        });

        it('should handle undefined urlMap and use url', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: undefined,
                url: '/services',
                languageId: 4
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/services',
                language_id: 4
            });
        });

        it('should pass correct language_id parameter', () => {
            const mockContentlet = createFakeContentlet({
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                urlMap: '/blog-post',
                languageId: 5
            });

            service.editPage(mockContentlet);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/blog-post',
                language_id: 5
            });
        });
    });

    describe('openEditByIdentifier', () => {
        it('should resolve the identifier to its working inode and open the edit panel', () => {
            const resolved = createFakeContentlet({
                inode: 'working-inode-1',
                identifier: 'shared-identifier',
                title: 'Shared Content'
            });
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [resolved] } }));

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true',
                limit: 1
            });
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                contentletInode: 'working-inode-1',
                identifier: 'shared-identifier',
                title: 'Shared Content'
            });
        });

        it('should resolve the identifier in the language the drive is showing', () => {
            // An identifier exists once per language, so a lookup with no language term returns an
            // arbitrary version — the deep link could open a language the user is not looking at.
            store.getFilterValue.mockReturnValue(['2']);
            const resolved = createFakeContentlet({
                inode: 'es-inode',
                identifier: 'shared-identifier',
                title: 'Contenido'
            });
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [resolved] } }));

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true +languageId:2',
                limit: 1
            });
            expect(service.$editPanelRequest()).toEqual({
                mode: 'edit',
                contentletInode: 'es-inode',
                identifier: 'shared-identifier',
                title: 'Contenido'
            });
        });

        // A whitespace group (`languageId:(1 2)`) is NOT an implicit OR here — it fails the whole
        // query, which surfaces as an empty result set with no error.
        it('should OR the selected languages when several are active', () => {
            store.getFilterValue.mockReturnValue(['1', '2']);
            contentSearch.get.mockReturnValue(
                of({ jsonObjectView: { contentlets: [createFakeContentlet({ inode: 'i' })] } })
            );

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true +languageId:(1 OR 2)',
                limit: 1
            });
        });

        it('should fall back to the default language when no language filter is set', () => {
            // Only reachable before the store has seeded the filter; the default is still a better
            // guess than whichever version the index happens to return first.
            store.getFilterValue.mockReturnValue(undefined);
            store.defaultLanguageId.mockReturnValue(3);
            contentSearch.get.mockReturnValue(
                of({ jsonObjectView: { contentlets: [createFakeContentlet({ inode: 'i' })] } })
            );

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true +languageId:3',
                limit: 1
            });
        });

        it('should omit the language term when no language is known at all', () => {
            store.getFilterValue.mockReturnValue(undefined);
            store.defaultLanguageId.mockReturnValue(undefined);
            contentSearch.get.mockReturnValue(
                of({ jsonObjectView: { contentlets: [createFakeContentlet({ inode: 'i' })] } })
            );

            service.openEditByIdentifier('shared-identifier');

            expect(contentSearch.get).toHaveBeenCalledWith({
                query: '+identifier:shared-identifier +working:true',
                limit: 1
            });
        });

        it('should not open the panel when the identifier resolves to nothing', () => {
            contentSearch.get.mockReturnValue(of({ jsonObjectView: { contentlets: [] } }));

            service.openEditByIdentifier('missing-identifier');

            expect(service.$editPanelRequest()).toBeNull();
        });

        it('should surface the error and not open the panel when the search fails', () => {
            const error = new HttpErrorResponse({ status: 500 });
            contentSearch.get.mockReturnValue(throwError(() => error));

            service.openEditByIdentifier('shared-identifier');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(service.$editPanelRequest()).toBeNull();
        });
    });
});

describe('DotContentDriveNavigationService (side panel disabled)', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: jest.Mocked<Router>;
    let contentTypeService: jest.Mocked<DotContentTypeService>;

    const newEditorType = () =>
        createFakeContentType({
            id: 'blog',
            name: 'Blog',
            metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
        });

    // Own factory with the side panel flag OFF, so the new editor falls back to route navigation.
    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, { navigate: jest.fn() }),
            mockProvider(DotContentTypeService, { getContentType: jest.fn() }),
            mockProvider(DotRouterService, { goToEditPage: jest.fn() }),
            mockProvider(Location, { path: jest.fn() }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotContentSearchService, { get: jest.fn() }),
            mockStoreWithFlag(false)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);
        contentTypeService = spectator.inject(DotContentTypeService);
    });

    it('should navigate to the full-screen editor instead of opening the panel (edit)', () => {
        const mockContentlet = createFakeContentlet({ contentType: 'blog', inode: 'inode-x' });
        contentTypeService.getContentType.mockReturnValue(of(newEditorType()));

        service.editContent(mockContentlet);

        expect(router.navigate).toHaveBeenCalledWith(['content/inode-x']);
        expect(service.$editPanelRequest()).toBeNull();
    });

    it('should navigate to the full-screen new-content editor instead of the panel (create)', () => {
        contentTypeService.getContentType.mockReturnValue(of(newEditorType()));

        service.createContent('blog', { folderPath: 'demo.dotcms.com/about-us/' });

        expect(router.navigate).toHaveBeenCalledWith(['content/new/blog'], {
            queryParams: { folderPath: 'demo.dotcms.com/about-us/' }
        });
        expect(service.$editPanelRequest()).toBeNull();
    });

    it('should navigate to the full-screen editor instead of opening the panel for a deep link (?editContent=)', () => {
        // The param can outlive the flag being on (shared link, bookmark, staging→prod) — reading
        // the flag here (not skipping it) keeps this path honoring AC15 when the flag is off.
        const contentSearch = spectator.inject(DotContentSearchService);
        (contentSearch.get as jest.Mock).mockReturnValue(
            of({ jsonObjectView: { contentlets: [createFakeContentlet({ inode: 'inode-y' })] } })
        );

        service.openEditByIdentifier('shared-identifier');

        expect(router.navigate).toHaveBeenCalledWith(['content/inode-y']);
        expect(service.$editPanelRequest()).toBeNull();
    });
});

describe('DotContentDriveNavigationService ($sidePanelEnabled)', () => {
    // The flag now comes from the store's `withFlags` slice; the nav service just maps it to a
    // boolean. The failed-config-read degradation is owned (and tested) by withFlags itself — here
    // an empty/unresolved flags map (its degraded value) must simply read as `false`.
    const flagsSignal = signal<Partial<Record<FeaturedFlags, boolean>>>({});
    let spectator: SpectatorService<DotContentDriveNavigationService>;

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, { navigate: jest.fn() }),
            mockProvider(DotContentTypeService, { getContentType: jest.fn() }),
            mockProvider(DotRouterService, { goToEditPage: jest.fn() }),
            mockProvider(Location, { path: jest.fn() }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn().mockReturnValue(of({})) }),
            mockProvider(DotContentSearchService, { get: jest.fn() }),
            mockProvider(DotContentDriveStore, { flags: flagsSignal })
        ]
    });

    beforeEach(() => {
        flagsSignal.set({});
        spectator = createService();
    });

    it('is true when the store reports the flag enabled', () => {
        flagsSignal.set({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: true });

        expect(spectator.service.$sidePanelEnabled()).toBe(true);
    });

    it('is false when the store reports the flag disabled', () => {
        flagsSignal.set({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: false });

        expect(spectator.service.$sidePanelEnabled()).toBe(false);
    });

    it('is false (never throws) when the flags map is empty — the withFlags degraded/unresolved value', () => {
        flagsSignal.set({});

        expect(() => spectator.service.$sidePanelEnabled()).not.toThrow();
        expect(spectator.service.$sidePanelEnabled()).toBe(false);
    });
});
