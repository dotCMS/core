import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { MenuItem } from 'primeng/api';

import { withBreadcrumbs } from './breadcrumb.feature';

describe('withBreadcrumbs Feature', () => {
    // Create a test store that uses the withBreadcrumbs feature
    const TestStore = signalStore(withState({}), withBreadcrumbs());

    let store: InstanceType<typeof TestStore>;
    let sessionStorageSetItemSpy: jest.SpyInstance;
    let sessionStorageGetItemSpy: jest.SpyInstance;

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
        sessionStorageGetItemSpy = jest.spyOn(Storage.prototype, 'getItem');

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

        it('should load breadcrumbs from sessionStorage on init', () => {
            expect(sessionStorageGetItemSpy).toHaveBeenCalledWith('breadcrumbs');
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
});
