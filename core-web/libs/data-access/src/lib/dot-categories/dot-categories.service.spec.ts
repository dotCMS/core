import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSAPIResponse, DotCategory } from '@dotcms/dotcms-models';

import { DotCategoriesService } from './dot-categories.service';

describe('DotCategoriesService', () => {
    let spectator: SpectatorHttp<DotCategoriesService>;

    const createFakeCategory = (overrides: Partial<DotCategory> = {}): DotCategory =>
        ({
            categoryName: 'Test Category',
            key: 'test-key',
            categoryVelocityVarName: 'testCategory',
            sortOrder: 0,
            active: true,
            inode: 'test-inode',
            identifier: 'test-id',
            type: 'Category',
            childrenCount: 0,
            description: '',
            keywords: '',
            iDate: Date.now(),
            owner: 'system',
            ...overrides
        }) as DotCategory;

    const createHttp = createHttpFactory({
        service: DotCategoriesService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should get paginated categories with all params', () => {
        const mockCategory = createFakeCategory({ categoryName: 'cat1' });
        const mockResponse: DotCMSAPIResponse<DotCategory[]> = {
            entity: [mockCategory],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 2, perPage: 10, totalEntries: 50 }
        };

        spectator.service
            .getCategoriesPaginated({
                filter: 'test',
                page: 2,
                per_page: 10,
                orderby: 'category_name',
                direction: 'ASC'
            })
            .subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

        const req = spectator.expectOne(
            '/api/v1/categories?filter=test&page=2&per_page=10&orderby=category_name&direction=ASC&showChildrenCount=true',
            HttpMethod.GET
        );
        req.flush(mockResponse);
    });

    it('should get paginated categories with no params', () => {
        const mockResponse: DotCMSAPIResponse<DotCategory[]> = {
            entity: [],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 1, perPage: 25, totalEntries: 0 }
        };

        spectator.service.getCategoriesPaginated({}).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne(
            '/api/v1/categories?showChildrenCount=true',
            HttpMethod.GET
        );
        req.flush(mockResponse);
    });

    it('should get children paginated with inode', () => {
        const mockCategory = createFakeCategory({ categoryName: 'child1' });
        const mockResponse: DotCMSAPIResponse<DotCategory[]> = {
            entity: [mockCategory],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 1, perPage: 25, totalEntries: 1 }
        };

        spectator.service
            .getChildrenPaginated('parent-inode', { filter: 'child', page: 1 })
            .subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

        const req = spectator.expectOne(
            '/api/v1/categories/children?filter=child&page=1&showChildrenCount=true&inode=parent-inode&parentList=true',
            HttpMethod.GET
        );
        req.flush(mockResponse);
    });

    it('should create a category', () => {
        const mockCategory = createFakeCategory({ categoryName: 'New Category' });
        const mockResponse: DotCMSAPIResponse<DotCategory> = {
            entity: mockCategory,
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        const form = {
            categoryName: 'New Category',
            key: 'new-cat',
            categoryVelocityVarName: 'newCat',
            sortOrder: 0
        };

        spectator.service.createCategory(form).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v1/categories', HttpMethod.POST);
        expect(req.request.body).toEqual(form);
        req.flush(mockResponse);
    });

    it('should update a category', () => {
        const mockCategory = createFakeCategory({ categoryName: 'Updated Category' });
        const mockResponse: DotCMSAPIResponse<DotCategory> = {
            entity: mockCategory,
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        const form = {
            inode: 'test-inode',
            categoryName: 'Updated Category',
            key: 'updated-cat',
            categoryVelocityVarName: 'updatedCat',
            sortOrder: 1
        };

        spectator.service.updateCategory(form).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v1/categories', HttpMethod.PUT);
        expect(req.request.body).toEqual(form);
        req.flush(mockResponse);
    });

    it('should delete categories by inodes', () => {
        const mockResponse: DotCMSAPIResponse<{ successCount: number; fails: unknown[] }> = {
            entity: { successCount: 2, fails: [] },
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service.deleteCategories(['inode-1', 'inode-2']).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v1/categories', HttpMethod.DELETE);
        expect(req.request.body).toEqual(['inode-1', 'inode-2']);
        req.flush(mockResponse);
    });
});
