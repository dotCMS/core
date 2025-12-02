import { signalStore, withState } from '@ngrx/signals';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MenuItemEntity } from '@dotcms/dotcms-models';

import { withBreadcrumbs } from './breadcrumb.feature';
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

    const menuItemsSignal = signal<MenuItemEntity[]>(mockMenuItems);
    const TestStore = signalStore(withState({}), withBreadcrumbs(menuItemsSignal));

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: Router,
                    useValue: {
                        events: { pipe: () => ({ subscribe: () => ({}) }) },
                        url: ''
                    }
                },
                TestStore
            ]
        });

        TestBed.inject(TestStore);
        TestBed.flushEffects();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('processSpecialRoute', () => {
        it('should execute templatesEdit handler for matching URL', () => {
            const result = processSpecialRoute({
                url: '/templates/edit/123',
                menu: mockMenuItems,
                breadcrumbs: []
            });

            expect(result).toBeDefined();
            if (result) {
                expect(result.type).toBe('set');
                expect(result.breadcrumbs).toBeDefined();
                expect(result.breadcrumbs.length).toBe(2);
            }
        });

        it('should return undefined when no handler matches the URL', () => {
            const result = processSpecialRoute({
                url: '/unknown-route',
                menu: mockMenuItems,
                breadcrumbs: []
            });

            expect(result).toBeUndefined();
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
            const result = ROUTE_HANDLERS.templatesEdit.handler({
                url: '/templates/edit/123',
                menu: mockMenuItems,
                breadcrumbs: []
            });

            expect(result).toBeDefined();
            if (result) {
                expect(result.type).toBe('set');
                expect(result.breadcrumbs).toBeDefined();
                expect(result.breadcrumbs.length).toBe(2);
                expect(result.breadcrumbs[0]).toEqual({ label: 'Content', disabled: true });
                expect(result.breadcrumbs[1]).toMatchObject({
                    label: 'Templates',
                    target: '_self',
                    url: '/dotAdmin/#/templates'
                });
            }
        });

        it('should return undefined when template not found in menu', () => {
            const result = ROUTE_HANDLERS.templatesEdit.handler({
                url: '/templates/edit/123',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeUndefined();
        });

        it('should return undefined when templates breadcrumb already exists', () => {
            const existingBreadcrumbs = [
                { label: 'Content', disabled: true },
                { label: 'Templates', url: '/dotAdmin/#/templates' }
            ];

            const result = ROUTE_HANDLERS.templatesEdit.handler({
                url: '/templates/edit/123',
                menu: mockMenuItems,
                breadcrumbs: existingBreadcrumbs
            });

            expect(result).toBeUndefined();
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

        it('should match URLs with /c/ prefix like /c/content?filter=', () => {
            expect(ROUTE_HANDLERS.contentFilter.test('/c/content?filter=Test')).toBe(true);
            expect(ROUTE_HANDLERS.contentFilter.test('/c/content?filter=YouTube')).toBe(true);
        });

        it('should not match URLs without /content path', () => {
            expect(ROUTE_HANDLERS.contentFilter.test('/my-content?filter=Test')).toBe(false);
            expect(ROUTE_HANDLERS.contentFilter.test('/products?filter=Test')).toBe(false);
        });

        it('should not match /content?filter= without a value', () => {
            expect(ROUTE_HANDLERS.contentFilter.test('/content?filter=')).toBe(false);
        });

        it('should add breadcrumb with extracted filter value', () => {
            const result = ROUTE_HANDLERS.contentFilter.handler({
                url: '/content?filter=Products',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeDefined();
            if (result) {
                expect(result.type).toBe('append');
                expect(result.breadcrumbs).toBeDefined();
                expect(result.breadcrumbs.length).toBe(1);
                expect(result.breadcrumbs[0]).toEqual({
                    label: 'Products',
                    target: '_self',
                    url: '/dotAdmin/#/content?filter=Products'
                });
            }
        });

        it('should handle complex filter values', () => {
            const result = ROUTE_HANDLERS.contentFilter.handler({
                url: '/content?filter=My-Complex-Filter',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeDefined();
            if (result) {
                expect(result.breadcrumbs[0].label).toBe('My-Complex-Filter');
            }
        });

        it('should extract only the filter parameter when URL has multiple query params', () => {
            const result = ROUTE_HANDLERS.contentFilter.handler({
                url: '/content?filter=Products&sort=asc&page=1',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeDefined();
            if (result) {
                expect(result.breadcrumbs[0].label).toBe('Products');
                expect(result.breadcrumbs[0].url).toBe(
                    '/dotAdmin/#/content?filter=Products&sort=asc&page=1'
                );
            }
        });

        it('should return undefined when filter parameter is empty', () => {
            const result = ROUTE_HANDLERS.contentFilter.handler({
                url: '/content?filter=&sort=asc',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeUndefined();
        });

        it('should return undefined when query string is missing', () => {
            const result = ROUTE_HANDLERS.contentFilter.handler({
                url: '/content',
                menu: [],
                breadcrumbs: []
            });

            expect(result).toBeUndefined();
        });
    });
});
