import { signalStore, withState } from '@ngrx/signals';
import { vi } from 'vitest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MenuItemEntity } from '@dotcms/dotcms-models';

import { withBreadcrumbs } from './breadcrumb.feature';
import { processSpecialRoute, ROUTE_HANDLERS, shouldReplaceLastCrumb } from './breadcrumb.utils';

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
        vi.restoreAllMocks();
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

/**
 * Content Drive links always carry query params (`path`, `filters`, `editContent`, …) and usually no
 * `mId`, so they never match a menu item and the trail used to stay as it was: the previous page's
 * title after a redirect, a blank one in a new tab (#37759).
 */
describe('ROUTE_HANDLERS.contentDrive', () => {
    // Read without the property access throwing, so a missing handler fails the assertion instead.
    const handler = () => ROUTE_HANDLERS['contentDrive'];

    const CONTENT_DRIVE: MenuItemEntity = {
        id: 'content-drive',
        label: 'Content Drive',
        labelParent: 'Content',
        menuLink: '/content-drive',
        url: '/content-drive',
        angular: true,
        active: false,
        ajax: false,
        parentMenuId: 'content-parent',
        parentMenuLabel: 'Content',
        parentMenuIcon: 'pi pi-file'
    } as MenuItemEntity;

    const URL = '/content-drive?path=/blog/&editContent=id-1&editContentLang=1';

    describe('matching', () => {
        it('matches a Content Drive URL with query params', () => {
            expect(handler()?.test(URL)).toBe(true);
            expect(handler()?.test('/content-drive?filters=contentType:Blog')).toBe(true);
            expect(handler()?.test('/content-drive?createContent=webPageContent')).toBe(true);
        });

        it('leaves the bare URL to the menu match, which already handles it', () => {
            expect(handler()?.test('/content-drive')).toBe(false);
        });

        it('matches no other path', () => {
            expect(handler()?.test('/content-drive-archive?x=1')).toBe(false);
            expect(handler()?.test('/c/content?filter=Blog')).toBe(false);
            expect(handler()?.test('/drive/content-drive?x=1')).toBe(false);
        });
    });

    it("starts a fresh trail from Content Drive's place in the menu", () => {
        expect(handler()?.handler({ url: URL, menu: [CONTENT_DRIVE], breadcrumbs: [] })).toEqual({
            type: 'set',
            breadcrumbs: [
                { label: 'Content', disabled: true },
                {
                    label: 'Content Drive',
                    target: '_self',
                    url: '/dotAdmin/#/content-drive?path=/blog/'
                }
            ]
        });
    });

    it('appends Content Drive to the trail of the page that sent the author there', () => {
        const fromQueryTool = [
            { label: 'Home', disabled: true },
            { label: 'Dev Tools', disabled: true },
            { label: 'Query Tool', target: '_self', url: '/dotAdmin/#/c/query-tool' }
        ];

        expect(
            handler()?.handler({ url: URL, menu: [CONTENT_DRIVE], breadcrumbs: fromQueryTool })
        ).toEqual({
            type: 'append',
            breadcrumbs: [
                {
                    label: 'Content Drive',
                    target: '_self',
                    url: '/dotAdmin/#/content-drive?path=/blog/'
                }
            ]
        });
    });

    describe('crumb URL', () => {
        const crumbUrlOf = (url: string) => {
            const result = handler()?.handler({ url, menu: [CONTENT_DRIVE], breadcrumbs: [] });

            return result?.type === 'set' ? result.breadcrumbs.at(-1)?.url : undefined;
        };

        // The crumb outlives the panel or dialog the URL opened. Once another crumb follows it, a
        // click on it must go back to the folder, not reopen the content or start another create.
        it('drops the params that open a panel or a folder dialog', () => {
            expect(
                crumbUrlOf(
                    '/content-drive?path=/blog/&createContent=webPageContent&editFolder=f-1&folderPermissions=f-1&createFolder=true'
                )
            ).toBe('/dotAdmin/#/content-drive?path=/blog/');
        });

        // Kept as the router wrote them, so a click on the crumb reports the same URL and the
        // trail truncates to it instead of growing.
        it('keeps the browsing params untouched and in order', () => {
            expect(
                crumbUrlOf(
                    '/content-drive?filters=contentType:Blog%3BlanguageId:1&editContent=id-1&path=/blog/&isTreeExpanded=true'
                )
            ).toBe(
                '/dotAdmin/#/content-drive?filters=contentType:Blog%3BlanguageId:1&path=/blog/&isTreeExpanded=true'
            );
        });

        it('links to the bare Content Drive when the URL only opened a panel', () => {
            expect(crumbUrlOf('/content-drive?editContent=id-1&editContentLang=1')).toBe(
                '/dotAdmin/#/content-drive'
            );
        });
    });

    it('leaves a trail that already ends on Content Drive, as after a reload', () => {
        const onContentDrive = [
            { label: 'Home', disabled: true },
            { label: 'Content', disabled: true },
            {
                label: 'Content Drive',
                target: '_self',
                url: '/dotAdmin/#/content-drive?path=/other/'
            }
        ];

        expect(
            handler()?.handler({ url: URL, menu: [CONTENT_DRIVE], breadcrumbs: onContentDrive })
        ).toBeUndefined();
    });

    it('does nothing when Content Drive is not in the menu', () => {
        expect(handler()?.handler({ url: URL, menu: [], breadcrumbs: [] })).toBeUndefined();
    });
});

describe('shouldReplaceLastCrumb', () => {
    describe('contentEdit rule', () => {
        it('should return true when both items match /content/{id}', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'New', url: '/dotAdmin/#/content/new-id' },
                    { label: 'Old', url: '/dotAdmin/#/content/old-id' }
                )
            ).toBe(true);
        });

        it('should return true when both items match /edit-page/content?{params}', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'New', url: '/dotAdmin/#/edit-page/content?url=page2' },
                    { label: 'Old', url: '/dotAdmin/#/edit-page/content?url=page1' }
                )
            ).toBe(true);
        });

        it('should return false when only new item matches content-edit', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'New', url: '/dotAdmin/#/content/abc' },
                    { label: 'Old', url: '/dotAdmin/#/c/content' }
                )
            ).toBe(false);
        });

        it('should return false when only last item matches content-edit', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'New', url: '/dotAdmin/#/settings' },
                    { label: 'Old', url: '/dotAdmin/#/content/abc' }
                )
            ).toBe(false);
        });
    });

    describe('analyticsTab rule', () => {
        it('should return true when both ids start with analytics-', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Conversions', id: 'analytics-conversions' },
                    { label: 'Engagement', id: 'analytics-engagement' }
                )
            ).toBe(true);
        });

        it('should return true when switching between any analytics tabs', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Pageview', id: 'analytics-pageview' },
                    { label: 'Conversions', id: 'analytics-conversions' }
                )
            ).toBe(true);
        });

        it('should return false when only new item id starts with analytics-', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Conversions', id: 'analytics-conversions' },
                    { label: 'Other', id: 'some-other-id' }
                )
            ).toBe(false);
        });

        it('should return false when only last item id starts with analytics-', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Other', id: 'other-id' },
                    { label: 'Engagement', id: 'analytics-engagement' }
                )
            ).toBe(false);
        });

        it('should return false when neither item has an analytics- id', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Page A', id: 'page-a' },
                    { label: 'Page B', id: 'page-b' }
                )
            ).toBe(false);
        });
    });

    describe('no rule matches', () => {
        it('should return false for unrelated breadcrumbs', () => {
            expect(
                shouldReplaceLastCrumb(
                    { label: 'Settings', url: '/dotAdmin/#/settings' },
                    { label: 'Dashboard', url: '/dotAdmin/#/dashboard' }
                )
            ).toBe(false);
        });

        it('should return false when items have no url and no id', () => {
            expect(shouldReplaceLastCrumb({ label: 'A' }, { label: 'B' })).toBe(false);
        });
    });
});
