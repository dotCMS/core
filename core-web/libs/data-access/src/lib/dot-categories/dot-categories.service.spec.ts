jest.mock('@dotcms/utils', () => ({
    ...jest.requireActual('@dotcms/utils'),
    getDownloadLink: jest.fn().mockReturnValue({ click: jest.fn() })
}));

import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSAPIResponse, DotCategory } from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';

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
            '/api/v1/categories/children?filter=child&page=1&showChildrenCount=true&inode=parent-inode',
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

    describe('exportCategories', () => {
        beforeEach(() => {
            (getDownloadLink as jest.Mock).mockClear();
        });

        it('should call GET /api/v1/categories/_export and trigger download', () => {
            spectator.service.exportCategories().subscribe();

            const req = spectator.expectOne('/api/v1/categories/_export', HttpMethod.GET);
            expect(req.request.responseType).toBe('blob');

            const blob = new Blob(['csv-data'], { type: 'text/csv' });
            req.flush(blob, {
                headers: { 'Content-Disposition': 'attachment; filename="exported-categories.csv"' }
            });

            expect(getDownloadLink).toHaveBeenCalledWith(
                expect.any(Blob),
                'exported-categories.csv'
            );
        });

        it('should append contextInode query param when provided', () => {
            spectator.service.exportCategories('parent-inode').subscribe();

            const req = spectator.expectOne(
                '/api/v1/categories/_export?contextInode=parent-inode',
                HttpMethod.GET
            );

            const blob = new Blob(['csv-data'], { type: 'text/csv' });
            req.flush(blob, {
                headers: { 'Content-Disposition': 'attachment; filename="categories.csv"' }
            });

            expect(getDownloadLink).toHaveBeenCalled();
        });

        it('should use default filename when Content-Disposition header is missing', () => {
            spectator.service.exportCategories().subscribe();

            const req = spectator.expectOne('/api/v1/categories/_export', HttpMethod.GET);

            const blob = new Blob(['csv-data'], { type: 'text/csv' });
            req.flush(blob);

            expect(getDownloadLink).toHaveBeenCalledWith(expect.any(Blob), 'categories.csv');
        });
    });

    describe('importCategories', () => {
        it('should POST FormData to /api/v1/categories/_import', () => {
            const file = new File(['csv-data'], 'categories.csv', { type: 'text/csv' });
            const mockResponse: DotCMSAPIResponse<unknown> = {
                entity: { success: true },
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.importCategories(file, 'merge').subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

            const req = spectator.expectOne('/api/v1/categories/_import', HttpMethod.POST);
            expect(req.request.body instanceof FormData).toBe(true);
            expect(req.request.body.get('file')).toBeTruthy();
            expect(req.request.body.get('exportType')).toBe('merge');
            expect(req.request.body.has('contextInode')).toBe(false);
            req.flush(mockResponse);
        });

        it('should include contextInode in FormData when provided', () => {
            const file = new File(['csv-data'], 'categories.csv', { type: 'text/csv' });
            const mockResponse: DotCMSAPIResponse<unknown> = {
                entity: { success: true },
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.importCategories(file, 'replace', 'parent-inode').subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

            const req = spectator.expectOne('/api/v1/categories/_import', HttpMethod.POST);
            expect(req.request.body instanceof FormData).toBe(true);
            expect(req.request.body.get('exportType')).toBe('replace');
            expect(req.request.body.get('contextInode')).toBe('parent-inode');
            req.flush(mockResponse);
        });
    });
});
