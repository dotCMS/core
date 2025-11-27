import { MenuItem } from 'primeng/api';

import { MenuItemEntity } from '@dotcms/dotcms-models';

import { processSpecialRoute, ROUTE_HANDLERS } from './breadcrumb.utils';

describe('Breadcrumb Utils - Route Handlers', () => {
    const mockMenuItems: MenuItemEntity[] = [
        {
            id: 'templates',
            label: 'Templates',
            labelParent: 'Content',
            menuLink: '/templates',
            url: '/templates',
            angular: true,
            active: false,
            ajax: false,
            parentMenuId: 'content-parent',
            parentMenuLabel: 'Content',
            parentMenuIcon: 'pi pi-file'
        } as MenuItemEntity
    ];

    describe('processSpecialRoute', () => {
        it('should execute templatesEdit handler for matching URL', () => {
            const breadcrumbs: MenuItem[] = [];
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            const result = processSpecialRoute(
                '/templates/edit/123',
                mockMenuItems,
                breadcrumbs,
                helpers
            );

            expect(result).toBe(true);
            expect(helpers.setBreadcrumbs).toHaveBeenCalled();
        });

        it('should return false when no handler matches the URL', () => {
            const breadcrumbs: MenuItem[] = [];
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            const result = processSpecialRoute(
                '/unknown-route',
                mockMenuItems,
                breadcrumbs,
                helpers
            );

            expect(result).toBe(false);
            expect(helpers.setBreadcrumbs).not.toHaveBeenCalled();
            expect(helpers.addNewBreadcrumb).not.toHaveBeenCalled();
        });
    });

    describe('ROUTE_HANDLERS.templatesEdit', () => {
        it('should match /templates/edit/:id URLs', () => {
            expect(ROUTE_HANDLERS.templatesEdit.test('/templates/edit/123')).toBe(true);
            expect(ROUTE_HANDLERS.templatesEdit.test('/templates/edit/abc-xyz')).toBe(true);
        });

        it('should not match invalid patterns', () => {
            expect(ROUTE_HANDLERS.templatesEdit.test('/templates/view/123')).toBe(false);
            expect(ROUTE_HANDLERS.templatesEdit.test('/templates/edit/')).toBe(false);
        });

        it('should build breadcrumbs when template exists in menu', () => {
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            const result = ROUTE_HANDLERS.templatesEdit.handler(
                '/templates/edit/123',
                mockMenuItems,
                [],
                helpers
            );

            expect(result).toBe(true);
            expect(helpers.setBreadcrumbs).toHaveBeenCalledWith([
                { label: 'Home', disabled: true },
                { label: 'Content', disabled: true },
                expect.objectContaining({ label: 'Templates', url: '/dotAdmin/#/templates' })
            ]);
        });

        it('should return false when template not found in menu', () => {
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            const result = ROUTE_HANDLERS.templatesEdit.handler(
                '/templates/edit/123',
                [],
                [],
                helpers
            );

            expect(result).toBe(false);
            expect(helpers.setBreadcrumbs).not.toHaveBeenCalled();
        });
    });

    describe('ROUTE_HANDLERS.contentFilter', () => {
        it('should match /content?filter= URLs', () => {
            expect(ROUTE_HANDLERS.contentFilter.test('/content?filter=Products')).toBe(true);
            expect(ROUTE_HANDLERS.contentFilter.test('/content?filter=Blog')).toBe(true);
        });

        it('should not match URLs without filter parameter', () => {
            expect(ROUTE_HANDLERS.contentFilter.test('/content')).toBe(false);
            expect(ROUTE_HANDLERS.contentFilter.test('/products?filter=test')).toBe(false);
        });

        it('should add breadcrumb with extracted filter value', () => {
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            ROUTE_HANDLERS.contentFilter.handler('/content?filter=Products', [], [], helpers);

            expect(helpers.addNewBreadcrumb).toHaveBeenCalledWith({
                label: 'Products',
                target: '_self',
                url: '/dotAdmin/#/content?filter=Products'
            });
        });

        it('should handle complex filter values', () => {
            const helpers = {
                setBreadcrumbs: jest.fn(),
                addNewBreadcrumb: jest.fn()
            };

            ROUTE_HANDLERS.contentFilter.handler(
                '/content?filter=My-Complex-Filter',
                [],
                [],
                helpers
            );

            expect(helpers.addNewBreadcrumb).toHaveBeenCalledWith(
                expect.objectContaining({ label: 'My-Complex-Filter' })
            );
        });
    });
});
