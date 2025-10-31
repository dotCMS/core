import { signalStore, withState } from '@ngrx/signals';
import { Observable, Subject } from 'rxjs';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Event, NavigationEnd, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { DotMenuItem } from '@dotcms/dotcms-models';

import { withBreadcrumbs } from './breadcrumb.feature';

describe('withBreadcrumbs Feature', () => {
    // Create a test store that uses the withBreadcrumbs feature
    // Empty menu items signal for basic tests
    const emptyMenuItemsSignal = signal<DotMenuItem[]>([]);
    const TestStore = signalStore(withState({}), withBreadcrumbs(emptyMenuItemsSignal));

    let store: InstanceType<typeof TestStore>;
    let sessionStorageSetItemSpy: jest.SpyInstance;

    const mockBreadcrumbs: MenuItem[] = [
        { label: 'Home', url: '/home' },
        { label: 'Products', url: '/products' },
        { label: 'Details', url: '/products/123' }
    ];

    beforeEach(() => {
        // Clear sessionStorage
        sessionStorage.clear();

        // Setup spies
        sessionStorageSetItemSpy = jest.spyOn(Storage.prototype, 'setItem');

        TestBed.configureTestingModule({
            providers: [TestStore]
        });

        store = TestBed.inject(TestStore);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('Initial State', () => {
        it('should initialize with empty breadcrumbs array', () => {
            expect(store.breadcrumbs()).toEqual([]);
        });

        it('should have breadcrumbCount as 0', () => {
            expect(store.breadcrumbCount()).toBe(0);
        });

        it('should have hasBreadcrumbs as false', () => {
            expect(store.hasBreadcrumbs()).toBe(false);
        });

        it('should have selectLastBreadcrumbLabel as null', () => {
            expect(store.selectLastBreadcrumbLabel()).toBeNull();
        });

        it('should have loadBreadcrumbs method available', () => {
            // loadBreadcrumbs is no longer called automatically on init
            // but the method should still be available
            expect(typeof store.loadBreadcrumbs).toBe('function');
        });
    });

    describe('SessionStorage Persistence', () => {
        it('should save to sessionStorage when setBreadcrumbs is called', () => {
            store.setBreadcrumbs(mockBreadcrumbs);

            // Wait for effect to run
            TestBed.flushEffects();

            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify(mockBreadcrumbs)
            );
        });

        it('should save to sessionStorage when appendCrumb is called', () => {
            store.setBreadcrumbs([mockBreadcrumbs[0]]);
            TestBed.flushEffects();
            sessionStorageSetItemSpy.mockClear();

            const newCrumb = { label: 'New Page', url: '/new' };
            store.appendCrumb(newCrumb);

            // Wait for effect to run
            TestBed.flushEffects();

            const expectedBreadcrumbs = [mockBreadcrumbs[0], newCrumb];
            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify(expectedBreadcrumbs)
            );
        });

        it('should save to sessionStorage when truncateBreadcrumbs is called', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            TestBed.flushEffects();
            sessionStorageSetItemSpy.mockClear();

            store.truncateBreadcrumbs(1);

            // Wait for effect to run
            TestBed.flushEffects();

            const expectedBreadcrumbs = mockBreadcrumbs.slice(0, 2);
            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify(expectedBreadcrumbs)
            );
        });

        it('should save to sessionStorage when setLastBreadcrumb is called', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            TestBed.flushEffects();
            sessionStorageSetItemSpy.mockClear();

            const updatedLastCrumb = { label: 'Updated', url: '/updated' };
            store.setLastBreadcrumb(updatedLastCrumb);

            // Wait for effect to run
            TestBed.flushEffects();

            const expectedBreadcrumbs = [...mockBreadcrumbs.slice(0, -1), updatedLastCrumb];
            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify(expectedBreadcrumbs)
            );
        });

        it('should save to sessionStorage when addNewBreadcrumb is called', () => {
            store.setBreadcrumbs([mockBreadcrumbs[0]]);
            TestBed.flushEffects();
            sessionStorageSetItemSpy.mockClear();

            const newBreadcrumb = {
                label: 'New Item',
                target: '_self',
                url: '/dotAdmin/#/new-url'
            };
            store.addNewBreadcrumb(newBreadcrumb);

            // Wait for effect to run
            TestBed.flushEffects();

            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
            const lastCall =
                sessionStorageSetItemSpy.mock.calls[sessionStorageSetItemSpy.mock.calls.length - 1];
            expect(lastCall[0]).toBe('breadcrumbs');
        });

        it('should persist empty array to sessionStorage when breadcrumbs are cleared', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            TestBed.flushEffects();
            sessionStorageSetItemSpy.mockClear();

            store.setBreadcrumbs([]);

            // Wait for effect to run
            TestBed.flushEffects();

            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify([])
            );
        });

        it('should persist to sessionStorage on every breadcrumb state change', () => {
            // Initial set
            store.setBreadcrumbs([mockBreadcrumbs[0]]);
            TestBed.flushEffects();
            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
            sessionStorageSetItemSpy.mockClear();

            // Append
            store.appendCrumb(mockBreadcrumbs[1]);
            TestBed.flushEffects();
            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
            sessionStorageSetItemSpy.mockClear();

            // Truncate
            store.truncateBreadcrumbs(0);
            TestBed.flushEffects();
            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
            sessionStorageSetItemSpy.mockClear();

            // Set last
            store.setLastBreadcrumb({ label: 'Updated', url: '/updated' });
            TestBed.flushEffects();
            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
            sessionStorageSetItemSpy.mockClear();

            // Add new
            store.addNewBreadcrumb({
                label: 'Another',
                url: '/dotAdmin/#/another',
                target: '_self'
            });
            TestBed.flushEffects();
            expect(sessionStorageSetItemSpy).toHaveBeenCalled();
        });

        it('should verify sessionStorage is called with correct breadcrumb data structure', () => {
            const testBreadcrumbs: MenuItem[] = [
                { label: 'dotCMS', disabled: true },
                { label: 'Content', disabled: true },
                { label: 'Blog Posts', target: '_self', url: '/dotAdmin/#/c/content' }
            ];

            store.setBreadcrumbs(testBreadcrumbs);
            TestBed.flushEffects();

            expect(sessionStorageSetItemSpy).toHaveBeenCalledWith(
                'breadcrumbs',
                JSON.stringify(testBreadcrumbs)
            );

            // Verify the data can be parsed back
            const savedData = sessionStorageSetItemSpy.mock.calls[0][1];
            const parsedData = JSON.parse(savedData);
            expect(parsedData).toEqual(testBreadcrumbs);
        });
    });

    describe('Set Breadcrumbs', () => {
        it('should set breadcrumbs array', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.breadcrumbs()).toEqual(mockBreadcrumbs);
        });

        it('should replace existing breadcrumbs', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            const newBreadcrumbs: MenuItem[] = [{ label: 'New', url: '/new' }];
            store.setBreadcrumbs(newBreadcrumbs);
            expect(store.breadcrumbs()).toEqual(newBreadcrumbs);
        });

        it('should update breadcrumbCount', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.breadcrumbCount()).toBe(3);
        });

        it('should update hasBreadcrumbs to true', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.hasBreadcrumbs()).toBe(true);
        });

        it('should update selectLastBreadcrumbLabel', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.selectLastBreadcrumbLabel()).toBe('Details');
        });
    });

    describe('Append Breadcrumb', () => {
        it('should append a breadcrumb to empty array', () => {
            const crumb: MenuItem = { label: 'Home', url: '/home' };
            store.appendCrumb(crumb);
            expect(store.breadcrumbs()).toEqual([crumb]);
        });

        it('should append a breadcrumb to existing array', () => {
            store.setBreadcrumbs([{ label: 'Home', url: '/home' }]);
            store.appendCrumb({ label: 'Products', url: '/products' });
            expect(store.breadcrumbs().length).toBe(2);
            expect(store.breadcrumbs()[1].label).toBe('Products');
        });

        it('should update breadcrumbCount after appending', () => {
            store.appendCrumb({ label: 'First', url: '/first' });
            expect(store.breadcrumbCount()).toBe(1);
            store.appendCrumb({ label: 'Second', url: '/second' });
            expect(store.breadcrumbCount()).toBe(2);
        });

        it('should update selectLastBreadcrumbLabel after appending', () => {
            store.appendCrumb({ label: 'First', url: '/first' });
            expect(store.selectLastBreadcrumbLabel()).toBe('First');
            store.appendCrumb({ label: 'Second', url: '/second' });
            expect(store.selectLastBreadcrumbLabel()).toBe('Second');
        });
    });

    describe('Clear Breadcrumbs', () => {
        beforeEach(() => {
            store.setBreadcrumbs(mockBreadcrumbs);
        });

        it('should clear breadcrumbs array', () => {
            store.clearBreadcrumbs();
            expect(store.breadcrumbs()).toEqual([]);
        });

        it('should reset breadcrumbCount to 0', () => {
            store.clearBreadcrumbs();
            expect(store.breadcrumbCount()).toBe(0);
        });

        it('should reset hasBreadcrumbs to false', () => {
            store.clearBreadcrumbs();
            expect(store.hasBreadcrumbs()).toBe(false);
        });

        it('should reset selectLastBreadcrumbLabel to null', () => {
            store.clearBreadcrumbs();
            expect(store.selectLastBreadcrumbLabel()).toBeNull();
        });
    });

    describe('Computed: breadcrumbCount', () => {
        it('should return correct count for various array sizes', () => {
            expect(store.breadcrumbCount()).toBe(0);

            store.setBreadcrumbs([{ label: 'One', url: '/one' }]);
            expect(store.breadcrumbCount()).toBe(1);

            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.breadcrumbCount()).toBe(3);
        });
    });

    describe('Computed: hasBreadcrumbs', () => {
        it('should return false for empty array', () => {
            expect(store.hasBreadcrumbs()).toBe(false);
        });

        it('should return true for non-empty array', () => {
            store.setBreadcrumbs([{ label: 'Home', url: '/home' }]);
            expect(store.hasBreadcrumbs()).toBe(true);
        });
    });

    describe('Computed: selectLastBreadcrumbLabel', () => {
        it('should return null for empty breadcrumbs', () => {
            expect(store.selectLastBreadcrumbLabel()).toBeNull();
        });

        it('should return label of single breadcrumb', () => {
            store.setBreadcrumbs([{ label: 'Home', url: '/home' }]);
            expect(store.selectLastBreadcrumbLabel()).toBe('Home');
        });

        it('should return label of last breadcrumb in array', () => {
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.selectLastBreadcrumbLabel()).toBe('Details');
        });

        it('should return null if last breadcrumb has no label', () => {
            store.setBreadcrumbs([{ label: 'Home', url: '/home' }, { url: '/no-label' }]);
            expect(store.selectLastBreadcrumbLabel()).toBeNull();
        });

        it('should update when breadcrumbs change', () => {
            store.setBreadcrumbs([{ label: 'First', url: '/first' }]);
            expect(store.selectLastBreadcrumbLabel()).toBe('First');

            store.setBreadcrumbs([
                { label: 'First', url: '/first' },
                { label: 'Second', url: '/second' }
            ]);
            expect(store.selectLastBreadcrumbLabel()).toBe('Second');
        });
    });

    describe('Edge Cases and Error Handling', () => {
        it('should handle breadcrumbs with undefined properties', () => {
            const crumbsWithUndefined: MenuItem[] = [
                { label: 'Home', url: '/home' },
                { label: undefined, url: '/undefined-label' },
                { url: '/no-label' }, // No label property
                { label: 'Last', url: undefined }
            ];

            store.setBreadcrumbs(crumbsWithUndefined);
            expect(store.breadcrumbCount()).toBe(4);
            expect(store.selectLastBreadcrumbLabel()).toBe('Last');
        });

        it('should handle breadcrumbs with null properties', () => {
            const crumbsWithNull: MenuItem[] = [
                { label: 'Home', url: '/home' },
                { label: null, url: '/null-label' },
                { label: 'Last', url: null }
            ];

            store.setBreadcrumbs(crumbsWithNull);
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe('Last');
        });

        it('should handle empty objects in breadcrumbs array', () => {
            const crumbsWithEmpty: MenuItem[] = [
                { label: 'Home', url: '/home' },
                {}, // Empty object
                { label: 'Last', url: '/last' }
            ];

            store.setBreadcrumbs(crumbsWithEmpty);
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe('Last');
        });

        it('should handle null/undefined input to setBreadcrumbs gracefully', () => {
            // Test with null - should not throw but the state becomes null
            expect(() => store.setBreadcrumbs(null as unknown as MenuItem[])).not.toThrow();
            // The implementation sets breadcrumbs to null, so we check it's not an array
            expect(store.breadcrumbs()).not.toBeInstanceOf(Array);

            // Reset state for next test
            store.setBreadcrumbs([]);

            // Test with undefined
            expect(() => store.setBreadcrumbs(undefined as unknown as MenuItem[])).not.toThrow();
            expect(store.breadcrumbs()).not.toBeInstanceOf(Array);
        });

        it('should handle null/undefined input to appendCrumb gracefully', () => {
            // Test with null
            expect(() => store.appendCrumb(null as unknown as MenuItem)).not.toThrow();
            expect(store.breadcrumbCount()).toBe(1);

            // Test with undefined
            expect(() => store.appendCrumb(undefined as unknown as MenuItem)).not.toThrow();
            expect(store.breadcrumbCount()).toBe(2);
        });
    });

    describe('Immutability Tests', () => {
        it('should not mutate original array when setting breadcrumbs', () => {
            const originalArray: MenuItem[] = [
                { label: 'Home', url: '/home' },
                { label: 'Products', url: '/products' }
            ];
            const originalArrayCopy = [...originalArray];

            store.setBreadcrumbs(originalArray);

            // Modify the store's breadcrumbs
            store.appendCrumb({ label: 'New', url: '/new' });

            // Original array should remain unchanged
            expect(originalArray).toEqual(originalArrayCopy);
        });

        it('should not mutate original array when appending breadcrumb', () => {
            const originalArray: MenuItem[] = [{ label: 'Home', url: '/home' }];
            const originalArrayCopy = [...originalArray];

            store.setBreadcrumbs(originalArray);
            store.appendCrumb({ label: 'New', url: '/new' });

            // Original array should remain unchanged
            expect(originalArray).toEqual(originalArrayCopy);
        });

        it('should create new array references when modifying breadcrumbs', () => {
            store.setBreadcrumbs([{ label: 'Home', url: '/home' }]);
            const firstArray = store.breadcrumbs();

            store.appendCrumb({ label: 'New', url: '/new' });
            const secondArray = store.breadcrumbs();

            // Should be different array references
            expect(firstArray).not.toBe(secondArray);
        });
    });

    describe('Performance and Large Data Tests', () => {
        it('should handle large number of breadcrumbs', () => {
            const largeBreadcrumbs: MenuItem[] = Array.from({ length: 100 }, (_, i) => ({
                label: `Breadcrumb ${i + 1}`,
                url: `/breadcrumb-${i + 1}`
            }));

            store.setBreadcrumbs(largeBreadcrumbs);
            expect(store.breadcrumbCount()).toBe(100);
            expect(store.selectLastBreadcrumbLabel()).toBe('Breadcrumb 100');
        });

        it('should handle breadcrumbs with very long labels', () => {
            const longLabel = 'A'.repeat(1000);
            const crumbsWithLongLabel: MenuItem[] = [
                { label: 'Home', url: '/home' },
                { label: 'Middle', url: '/middle' },
                { label: longLabel, url: '/long-label' }
            ];

            store.setBreadcrumbs(crumbsWithLongLabel);
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe(longLabel);
        });

        it('should handle breadcrumbs with special characters', () => {
            const specialCrumbs: MenuItem[] = [
                { label: 'Home & About', url: '/home' },
                { label: 'Products < > & " \' ', url: '/products' },
                { label: 'Details: 100%', url: '/details' }
            ];

            store.setBreadcrumbs(specialCrumbs);
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe('Details: 100%');
        });
    });

    describe('Complex Scenarios', () => {
        it('should handle full breadcrumb workflow', () => {
            // Start empty
            expect(store.breadcrumbs()).toEqual([]);

            // Add breadcrumbs one by one
            store.appendCrumb({ label: 'Home', url: '/home' });
            store.appendCrumb({ label: 'Products', url: '/products' });
            expect(store.breadcrumbCount()).toBe(2);
            expect(store.selectLastBreadcrumbLabel()).toBe('Products');

            // Replace with new set
            store.setBreadcrumbs(mockBreadcrumbs);
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe('Details');

            // Clear all
            store.clearBreadcrumbs();
            expect(store.hasBreadcrumbs()).toBe(false);
        });

        it('should handle multiple operations in sequence', () => {
            store.setBreadcrumbs([{ label: 'First', url: '/first' }]);
            store.appendCrumb({ label: 'Second', url: '/second' });
            store.appendCrumb({ label: 'Third', url: '/third' });
            expect(store.breadcrumbCount()).toBe(3);

            store.clearBreadcrumbs();
            expect(store.breadcrumbCount()).toBe(0);

            store.appendCrumb({ label: 'New', url: '/new' });
            expect(store.breadcrumbs().length).toBe(1);
            expect(store.selectLastBreadcrumbLabel()).toBe('New');
        });

        it('should handle navigation workflow with mixed operations', () => {
            // Start with some breadcrumbs
            store.setBreadcrumbs([
                { label: 'Dashboard', url: '/dashboard' },
                { label: 'Content', url: '/content' }
            ]);

            // Navigate deeper
            store.appendCrumb({ label: 'Edit', url: '/content/edit' });
            expect(store.breadcrumbCount()).toBe(3);
            expect(store.selectLastBreadcrumbLabel()).toBe('Edit');

            // Navigate to different section (replace breadcrumbs)
            store.setBreadcrumbs([
                { label: 'Dashboard', url: '/dashboard' },
                { label: 'Settings', url: '/settings' }
            ]);
            expect(store.breadcrumbCount()).toBe(2);
            expect(store.selectLastBreadcrumbLabel()).toBe('Settings');

            // Add more levels
            store.appendCrumb({ label: 'Users', url: '/settings/users' });
            store.appendCrumb({ label: 'Create', url: '/settings/users/create' });
            expect(store.breadcrumbCount()).toBe(4);
            expect(store.selectLastBreadcrumbLabel()).toBe('Create');
        });
    });

    describe('Router Integration with listenToRouterEvents', () => {
        class RouterMock {
            private _events = new Subject<Event>();

            get events(): Observable<Event> {
                return this._events.asObservable();
            }

            triggerNavigationEnd(url: string) {
                this._events.next(new NavigationEnd(1, url, url));
            }
        }

        let routerMock: RouterMock;
        let menuItemsSignal: ReturnType<typeof signal<DotMenuItem[]>>;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let TestStoreWithRouter: any;
        let storeWithRouter: InstanceType<typeof TestStoreWithRouter>;

        beforeEach(() => {
            // Reset TestBed to avoid conflicts with parent beforeEach
            TestBed.resetTestingModule();

            routerMock = new RouterMock();

            // Mock menu items with proper structure
            menuItemsSignal = signal<DotMenuItem[]>([
                {
                    id: 'content',
                    label: 'Content',
                    labelParent: 'Content',
                    menuLink: '/c/content',
                    url: '/c/content',
                    angular: true,
                    active: false,
                    ajax: false
                } as DotMenuItem,
                {
                    id: 'site-browser',
                    label: 'Site Browser',
                    labelParent: 'Tools',
                    menuLink: '/c/site-browser',
                    url: '/c/site-browser',
                    angular: true,
                    active: false,
                    ajax: false
                } as DotMenuItem,
                {
                    id: 'pages',
                    label: 'Pages',
                    labelParent: 'Content',
                    menuLink: '/pages',
                    url: '/pages',
                    angular: true,
                    active: false,
                    ajax: false
                } as DotMenuItem
            ]);

            // Create store with breadcrumb feature that receives menuItems signal
            TestStoreWithRouter = signalStore(withState({}), withBreadcrumbs(menuItemsSignal));

            TestBed.configureTestingModule({
                providers: [{ provide: Router, useValue: routerMock }, TestStoreWithRouter]
            });

            storeWithRouter = TestBed.inject(TestStoreWithRouter);
            TestBed.flushEffects();
        });

        it('should create breadcrumbs with Home when navigating to a menu link', () => {
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();

            expect(breadcrumbs.length).toBe(3);
            expect(breadcrumbs[0]).toEqual({
                label: 'Home',
                disabled: true
            });
            expect(breadcrumbs[1]).toEqual({
                label: 'Content',
                disabled: true
            });
            expect(breadcrumbs[2]).toMatchObject({
                label: 'Content',
                target: '_self',
                url: '/dotAdmin/#/c/content'
            });
        });

        it('should always include Home as the first breadcrumb item', () => {
            routerMock.triggerNavigationEnd('/c/site-browser');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();

            expect(breadcrumbs[0].label).toBe('Home');
            expect(breadcrumbs[0].disabled).toBe(true);
        });

        it('should set Home breadcrumb as disabled', () => {
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();
            const homeBreadcrumb = breadcrumbs[0];

            expect(homeBreadcrumb.disabled).toBe(true);
        });

        it('should truncate breadcrumbs when navigating back to existing URL', () => {
            // Navigate to first page
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const firstBreadcrumbs = storeWithRouter.breadcrumbs();
            expect(firstBreadcrumbs.length).toBe(3);

            // Navigate to second page
            routerMock.triggerNavigationEnd('/pages');
            TestBed.flushEffects();

            const secondBreadcrumbs = storeWithRouter.breadcrumbs();
            expect(secondBreadcrumbs.length).toBe(3);

            // Navigate back to first page (should truncate)
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const truncatedBreadcrumbs = storeWithRouter.breadcrumbs();
            expect(truncatedBreadcrumbs.length).toBe(3);
            expect(truncatedBreadcrumbs[0].label).toBe('Home');
        });

        it('should not add breadcrumb if URL is not in menu items', () => {
            const initialCount = storeWithRouter.breadcrumbCount();

            routerMock.triggerNavigationEnd('/unknown-route');
            TestBed.flushEffects();

            const finalCount = storeWithRouter.breadcrumbCount();

            // Should not have added any breadcrumbs for unknown route
            expect(finalCount).toBe(initialCount);
        });

        it('should maintain breadcrumb structure with Home after multiple navigations', () => {
            // First navigation
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            let breadcrumbs = storeWithRouter.breadcrumbs();
            expect(breadcrumbs[0].label).toBe('Home');
            expect(breadcrumbs.length).toBe(3);

            // Second navigation
            routerMock.triggerNavigationEnd('/c/site-browser');
            TestBed.flushEffects();

            breadcrumbs = storeWithRouter.breadcrumbs();
            expect(breadcrumbs[0].label).toBe('Home');
            expect(breadcrumbs.length).toBe(3);
        });

        it('should use GlobalStore for breadcrumb state management', () => {
            // Verify that navigating updates the breadcrumbs in the store
            const initialCount = storeWithRouter.breadcrumbCount();

            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const finalCount = storeWithRouter.breadcrumbCount();

            // Should have updated breadcrumbs in the store
            expect(finalCount).toBeGreaterThan(initialCount);
            expect(storeWithRouter.breadcrumbs().length).toBe(3);
        });

        it('should handle content filter URLs by adding new breadcrumb', () => {
            // First set up initial breadcrumbs
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const initialBreadcrumbs = storeWithRouter.breadcrumbs();
            expect(initialBreadcrumbs.length).toBe(3);

            // Navigate to filtered content
            routerMock.triggerNavigationEnd('/content?filter=Products');
            TestBed.flushEffects();

            const updatedBreadcrumbs = storeWithRouter.breadcrumbs();

            // Should have added a new breadcrumb with the filter label
            expect(updatedBreadcrumbs.length).toBeGreaterThan(initialBreadcrumbs.length);
            expect(updatedBreadcrumbs[updatedBreadcrumbs.length - 1]).toMatchObject({
                label: 'Products',
                target: '_self',
                url: '/dotAdmin/#/content?filter=Products'
            });
        });

        it('should properly construct breadcrumb URLs with /dotAdmin/# prefix', () => {
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();
            const currentBreadcrumb = breadcrumbs[breadcrumbs.length - 1];

            expect(currentBreadcrumb.url).toBe('/dotAdmin/#/c/content');
        });

        it('should use labelParent from menu item for parent breadcrumb', () => {
            routerMock.triggerNavigationEnd('/c/site-browser');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();

            // Second breadcrumb should be the parent with labelParent
            expect(breadcrumbs[1]).toEqual({
                label: 'Tools',
                disabled: true
            });
        });

        it('should update breadcrumbs reactively when menuItems signal changes', () => {
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            let breadcrumbs = storeWithRouter.breadcrumbs();
            expect(breadcrumbs[2].label).toBe('Content');

            // Update menu items with new label
            menuItemsSignal.set([
                {
                    id: 'content',
                    label: 'Updated Content',
                    labelParent: 'Updated Parent',
                    menuLink: '/c/content',
                    url: '/c/content',
                    angular: true,
                    active: false,
                    ajax: false
                } as DotMenuItem,
                {
                    id: 'new-page',
                    label: 'New Page',
                    labelParent: 'New Parent',
                    menuLink: '/new-page',
                    url: '/new-page',
                    angular: true,
                    active: false,
                    ajax: false
                } as DotMenuItem
            ]);

            // Navigate to the new page (different URL to avoid truncation)
            routerMock.triggerNavigationEnd('/new-page');
            TestBed.flushEffects();

            breadcrumbs = storeWithRouter.breadcrumbs();
            // Should use the new menu item data
            expect(breadcrumbs[2].label).toBe('New Page');
            expect(breadcrumbs[1].label).toBe('New Parent');
        });

        it('should handle navigation to root URL', () => {
            routerMock.triggerNavigationEnd('/');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();

            // Should not add breadcrumbs for root URL if not in menu
            expect(breadcrumbs.length).toBe(0);
        });

        it('should truncate correctly when navigating to a middle breadcrumb', () => {
            // Set up initial state with multiple breadcrumbs manually
            storeWithRouter.setBreadcrumbs([
                { label: 'Home', disabled: true },
                { label: 'Content', disabled: true },
                { label: 'Current', url: '/dotAdmin/#/c/content' },
                { label: 'Level 4', url: '/dotAdmin/#/level4' },
                { label: 'Level 5', url: '/dotAdmin/#/level5' }
            ]);
            TestBed.flushEffects();

            expect(storeWithRouter.breadcrumbCount()).toBe(5);

            // Navigate back to the third item
            routerMock.triggerNavigationEnd('/c/content');
            TestBed.flushEffects();

            const breadcrumbs = storeWithRouter.breadcrumbs();
            // Should have truncated and reset to the menu structure
            expect(breadcrumbs.length).toBe(3);
            expect(breadcrumbs[0].label).toBe('Home');
        });
    });
});
