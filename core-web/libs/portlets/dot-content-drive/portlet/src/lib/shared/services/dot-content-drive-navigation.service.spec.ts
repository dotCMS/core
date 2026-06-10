import { describe, expect, it } from '@jest/globals';
import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotContentDriveNavigationService } from './dot-content-drive-navigation.service';

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: jest.Mocked<Router>;
    let contentTypeService: jest.Mocked<DotContentTypeService>;
    let dotRouterService: jest.Mocked<DotRouterService>;
    let location: SpyObject<Location>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;

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
            })
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

        it('should navigate to new content editor when feature flag is enabled', () => {
            const mockContentlet = createFakeContentlet({
                contentType: 'blog',
                inode: 'test-inode-123'
            });

            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(router.navigate).toHaveBeenCalledWith(['content/test-inode-123']);
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
    });

    describe('createContent', () => {
        it('should navigate to new content editor (no query params) when feature flag is enabled', () => {
            const mockContentType = createFakeContentType({
                id: 'blog',
                name: 'Blog',
                metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
            });

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.createContent('blog');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(router.navigate).toHaveBeenCalledWith(['content/new/blog']);
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
});
