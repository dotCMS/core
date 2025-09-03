import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Router } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType, DotContentDriveItem, FeaturedFlags } from '@dotcms/dotcms-models';

import { DotContentDriveNavigationService } from './dot-content-drive-navigation.service';

describe('DotContentDriveNavigationService', () => {
    let spectator: SpectatorService<DotContentDriveNavigationService>;
    let service: DotContentDriveNavigationService;
    let router: jest.Mocked<Router>;
    let contentTypeService: jest.Mocked<DotContentTypeService>;

    const createService = createServiceFactory({
        service: DotContentDriveNavigationService,
        providers: [
            mockProvider(Router, {
                navigate: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        router = spectator.inject(Router) as jest.Mocked<Router>;
        contentTypeService = spectator.inject(
            DotContentTypeService
        ) as jest.Mocked<DotContentTypeService>;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('editContent', () => {
        it('should navigate to page editor when contentType is htmlpageasset', () => {
            const mockContentlet = {
                contentType: 'htmlpageasset',
                urlMap: '/test-page',
                languageId: 1,
                inode: 'test-inode',
                identifier: 'test-id',
                title: 'Test Page',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'HTMLPAGE'
            } as unknown as DotContentDriveItem;

            service.editContent(mockContentlet);

            expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
                queryParams: { url: '/test-page', language_id: 1 }
            });
        });

        it('should use url property when urlMap is not available for htmlpageasset', () => {
            const mockContentlet = {
                contentType: 'htmlpageasset',
                url: '/test-page-url',
                languageId: 2,
                inode: 'test-inode',
                identifier: 'test-id',
                title: 'Test Page',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'HTMLPAGE'
            } as unknown as DotContentDriveItem;

            service.editContent(mockContentlet);

            expect(router.navigate).toHaveBeenCalledWith(['/edit-page/content'], {
                queryParams: { url: '/test-page-url', language_id: 2 }
            });
        });

        it('should navigate to new content editor when feature flag is enabled', () => {
            const mockContentlet = {
                contentType: 'blog',
                inode: 'test-inode-123',
                identifier: 'test-id',
                title: 'Test Blog',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'CONTENT'
            } as unknown as DotContentDriveItem;

            const mockContentType = {
                id: 'blog',
                name: 'Blog',
                metadata: {
                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                }
            } as unknown as DotCMSContentType;

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('blog');
            expect(router.navigate).toHaveBeenCalledWith(['content/test-inode-123']);
        });

        it('should navigate to old content editor when feature flag is disabled', () => {
            const mockContentlet: DotContentDriveItem = {
                contentType: 'news',
                inode: 'test-inode-456',
                identifier: 'test-id',
                title: 'Test News',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'CONTENT'
            } as unknown as DotContentDriveItem;

            const mockContentType = {
                id: 'news',
                name: 'News',
                metadata: {
                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false
                }
            } as unknown as DotCMSContentType;

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('news');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-456']);
        });

        it('should navigate to old content editor when feature flag is missing', () => {
            const mockContentlet: DotContentDriveItem = {
                contentType: 'product',
                inode: 'test-inode-789',
                identifier: 'test-id',
                title: 'Test Product',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'CONTENT'
            } as unknown as DotContentDriveItem;

            const mockContentType = {
                id: 'product',
                name: 'Product',
                metadata: {}
            } as unknown as DotCMSContentType;

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('product');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-789']);
        });

        it('should navigate to old content editor when metadata is undefined', () => {
            const mockContentlet: DotContentDriveItem = {
                contentType: 'event',
                inode: 'test-inode-000',
                identifier: 'test-id',
                title: 'Test Event',
                modDate: '2023-01-01',
                modUser: 'test-user',
                modUserName: 'Test User',
                baseType: 'CONTENT'
            } as unknown as DotContentDriveItem;

            const mockContentType = {
                id: 'event',
                name: 'Event'
                // metadata is undefined
            } as unknown as DotCMSContentType;

            contentTypeService.getContentType.mockReturnValue(of(mockContentType));

            service.editContent(mockContentlet);

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('event');
            expect(router.navigate).toHaveBeenCalledWith(['c/content/test-inode-000']);
        });
    });
});
