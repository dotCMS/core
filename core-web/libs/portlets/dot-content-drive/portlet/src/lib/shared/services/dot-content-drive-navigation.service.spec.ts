import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Router } from '@angular/router';

import { DotContentTypeService, DotRouterService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotContentDriveNavigationService } from './dot-content-drive-navigation.service';

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: jest.Mocked<Router>;
    let contentTypeService: jest.Mocked<DotContentTypeService>;
    let dotRouterService: jest.Mocked<DotRouterService>;

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
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router);
        contentTypeService = spectator.inject(DotContentTypeService);
        dotRouterService = spectator.inject(DotRouterService);
    });

    afterEach(() => {
        jest.clearAllMocks();
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
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-456']);
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
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-789']);
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
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-000']);
        });
    });
});
