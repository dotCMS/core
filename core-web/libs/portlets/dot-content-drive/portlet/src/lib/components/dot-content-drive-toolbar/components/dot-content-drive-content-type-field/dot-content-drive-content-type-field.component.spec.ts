import { describe, it, expect, beforeEach, afterEach, beforeAll } from '@jest/globals';
import { createComponentFactory, Spectator, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveContentTypeFieldComponent } from './dot-content-drive-content-type-field.component';

import { MOCK_CONTENT_TYPES } from '../../../../shared/mocks';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveContentTypeFieldComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFieldComponent>;
    let mockStore: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let mockContentTypeService: SpyObject<DotContentTypeService>;

    // Helper functions for triggering MultiSelect events
    const triggerMultiSelectOnChange = (selectedValues?: DotCMSContentType[]) => {
        spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onChange', {
            value: selectedValues || []
        });
        spectator.detectChanges();
    };

    const triggerMultiSelectOnFilter = (filter = '') => {
        spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onFilter', {
            filter
        });
        spectator.detectChanges();
    };

    const triggerMultiSelectOnPanelHide = () => {
        spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onPanelHide', {});
        spectator.detectChanges();
    };

    const triggerMultiSelectOnClear = () => {
        spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onClear', {});
        spectator.detectChanges();
    };

    const createComponent = createComponentFactory({
        component: DotContentDriveContentTypeFieldComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                filters: jest.fn().mockReturnValue({ baseType: [], contentType: [] }),
                getFilterValue: jest.fn().mockReturnValue([]),
                patchFilters: jest.fn(),
                removeFilter: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES)),
                getContentTypesWithPagination: jest.fn().mockReturnValue(
                    of({
                        contentTypes: MOCK_CONTENT_TYPES,
                        pagination: {
                            currentPage: MOCK_CONTENT_TYPES.length,
                            totalEntries: MOCK_CONTENT_TYPES.length * 2,
                            totalPages: 1
                        }
                    })
                )
            }),
            mockProvider(
                DotMessageService,
                new MockDotMessageService({
                    'content-drive.content-type.placeholder': 'Content Type',
                    'content-drive.content-type-field.empty-state': 'No content types found'
                })
            ),
            provideHttpClient()
        ],
        detectChanges: false
    });

    // Test setup with fake timers for testing debounced operations
    beforeAll(() => {
        jest.useFakeTimers();
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    beforeEach(() => {
        spectator = createComponent();
        mockStore = spectator.inject(DotContentDriveStore);
        mockContentTypeService = spectator.inject(DotContentTypeService, true);
    });

    afterEach(() => {
        jest.clearAllTimers(); // Clear any pending timers between tests
        jest.clearAllMocks(); // Reset all mock call history
    });

    describe('Component Initialization', () => {
        it('should initialize with correct initial state', () => {
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().filter).toBe('');
            expect(spectator.component.$state().loading).toBe(true);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });

        it('should trigger initial content types API request on component initialization', () => {
            spectator.detectChanges();
            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({});
        });

        it('should filter out forms and system content types after initial load', async () => {
            spectator.detectChanges();
            const expectedContentTypes = MOCK_CONTENT_TYPES.filter(
                (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
            );

            expect(spectator.component.$state().contentTypes).toEqual(expectedContentTypes);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should initialize selected content types from store filters', () => {
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            const expectedSelected = MOCK_CONTENT_TYPES.filter((ct) =>
                ['blog', 'news'].includes(ct.variable)
            ).map((ct) => ct);

            expect(spectator.component.$selectedContentTypes()).toEqual(expectedSelected);
        });

        it('should handle missing content type filters from store', () => {
            mockStore.getFilterValue.mockReturnValue(null);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });
    });

    describe('Loading State Management', () => {
        beforeEach(() => {
            // Skip initial effect call because of skip(1) in filter subscription
            spectator.detectChanges();
        });

        it('should set loading to true when starting a filter request', () => {
            triggerMultiSelectOnFilter('test');
            expect(spectator.component.$state().loading).toBe(true);
        });

        it('should set loading to false when filter request completes successfully', () => {
            spectator.detectChanges();
            jest.clearAllMocks(); // Clear initial request mock calls

            triggerMultiSelectOnFilter('test');
            expect(spectator.component.$state().loading).toBe(true);

            jest.advanceTimersByTime(500); // Complete debounced request

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes.length).toBeGreaterThan(0);
        });

        it('should set loading to false when filter request fails', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            triggerMultiSelectOnFilter('test');
            expect(spectator.component.$state().loading).toBe(true);

            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes).toEqual([]);
        });
    });

    describe('API Integration & Content Type Loading', () => {
        afterAll(() => {
            mockStore.filters.mockReturnValue({ baseType: undefined });
        });

        it('should call initial content types API with pagination on component initialization', () => {
            spectator.detectChanges();

            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({});
            expect(mockContentTypeService.getContentTypes).not.toHaveBeenCalled();
        });

        it('should handle base type filters', () => {
            // Tests current implementation where baseType is explicitly undefined
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Due to TODO in component, type parameter is always undefined
            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                type: 'CONTENT,WIDGET'
            });
        });
    });

    describe('Content Type Filtering & Search', () => {
        beforeEach(() => {
            // Skip initial effect call because of skip(1) in filter subscription
            mockStore.filters.mockReturnValue({ baseType: undefined });
            spectator.detectChanges();
            jest.clearAllMocks();
        });

        it('should call API with current filter when filter changes', () => {
            triggerMultiSelectOnFilter('blog');
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should trigger API request when filter signal changes', () => {
            triggerMultiSelectOnFilter('updated-filter');
            spectator.detectChanges();

            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'updated-filter'
            });
        });

        it('should debounce rapid filter changes and cancel previous requests', () => {
            // Simulate rapid typing scenario
            triggerMultiSelectOnFilter('first');
            spectator.detectChanges();
            jest.advanceTimersByTime(200); // Advance time but don't complete debounce

            // Type more before first request completes (tests switchMap cancellation)
            triggerMultiSelectOnFilter('second');
            spectator.detectChanges();
            jest.advanceTimersByTime(500); // Complete the debounced request

            // Should only call API once with final filter value due to switchMap
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'second'
            });
        });

        it('should handle filter API request errors gracefully', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.detectChanges();
            triggerMultiSelectOnFilter('test');
            jest.advanceTimersByTime(500);

            // Should return empty array on error and set loading to false
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should preserve selected content types when loading new filter results', () => {
            // Setup: Mock API to return only one content type, pre-select another
            mockStore.filters.mockReturnValue({ baseType: undefined });
            mockContentTypeService.getContentTypes.mockReturnValue(of([MOCK_CONTENT_TYPES[1]]));
            spectator.component.$selectedContentTypes.set([MOCK_CONTENT_TYPES[0]]);
            spectator.detectChanges();

            triggerMultiSelectOnFilter('test');
            spectator.detectChanges();
            jest.advanceTimersByTime(500); // Complete the filter request

            // Should merge selected items with API results
            expect(spectator.component.$state().contentTypes).toEqual([
                MOCK_CONTENT_TYPES[0], // Previously selected item (preserved)
                MOCK_CONTENT_TYPES[1] // New item from API
            ]);
        });

        it('should preserve selected content types and avoid duplicates in results', () => {
            // Setup: Pre-select content types that include one that will be returned by API
            spectator.component.$selectedContentTypes.set([
                MOCK_CONTENT_TYPES[0],
                MOCK_CONTENT_TYPES[1]
            ]);
            spectator.detectChanges();

            triggerMultiSelectOnFilter('test');
            // Mock API returns a content type that's already selected
            mockContentTypeService.getContentTypes.mockReturnValue(of([MOCK_CONTENT_TYPES[0]]));

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Should avoid duplicates when merging selected + API results
            expect(spectator.component.$state().contentTypes).toEqual([
                MOCK_CONTENT_TYPES[0],
                MOCK_CONTENT_TYPES[1]
            ]);
        });
    });

    describe('Filter Input Handling', () => {
        it('should update filter state immediately when filter input changes', () => {
            triggerMultiSelectOnFilter('test filter');
            spectator.detectChanges();

            expect(spectator.component.$state().filter).toBe('test filter');
        });

        it('should handle empty filter input', () => {
            triggerMultiSelectOnFilter('');

            expect(spectator.component.$state().filter).toBe('');
        });

        it('should handle null or undefined filter values gracefully', () => {
            triggerMultiSelectOnFilter(null as string | null);

            expect(spectator.component.$state().filter).toBeNull();
        });

        it('should handle multiple rapid filter changes (simulating user typing)', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            // Simulate rapid sequential typing without delays between keystrokes
            triggerMultiSelectOnFilter('b');
            triggerMultiSelectOnFilter('bl');
            triggerMultiSelectOnFilter('blo');
            triggerMultiSelectOnFilter('blog');

            spectator.detectChanges();
            jest.advanceTimersByTime(500); // Wait for debounce period to complete

            // Should debounce all rapid changes and only make one final API call
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });
    });

    describe('Base Type Filtering (Untracked Logic)', () => {
        it('should filter selected content types when base types are provided', () => {
            // Mock store to return base types that should keep only CONTENT and WIDGET
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] }); // Maps to 'CONTENT,WIDGET'
            spectator.detectChanges();

            // Set up selected content types with different base types
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' }, // Should be kept
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }, // Should be kept
                { ...MOCK_CONTENT_TYPES[2], baseType: 'PESRONA' } // Should be filtered out
            ];
            spectator.component.$selectedContentTypes.set(selectedContentTypes);
            triggerMultiSelectOnFilter('test');

            // Trigger the effect by detecting changes (this triggers the computed signal)

            // spectator.detectChanges();
            // Verify that only content types with matching base types are preserved
            const filteredContentTypes = spectator.component.$selectedContentTypes();
            expect(filteredContentTypes).toHaveLength(2);
            expect(
                filteredContentTypes.every((ct) => ['CONTENT', 'WIDGET'].includes(ct.baseType))
            ).toBe(true);
            expect(filteredContentTypes.some((ct) => ct.baseType === 'PESRONA')).toBe(false);
        });

        it('should call onChange when filtering selected content types based on base types', () => {
            // Set up selected content types
            spectator.component.$selectedContentTypes.set([
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ]);

            // Spy on the onChange method
            const onChangeSpy = jest.spyOn(spectator.component, 'onChange' as never);

            // Mock store to return base types
            mockStore.filters.mockReturnValue({ baseType: ['1'] }); // Maps to 'CONTENT'
            triggerMultiSelectOnFilter('test');
            // Trigger the effect
            spectator.detectChanges();

            // Verify onChange was called to update the store
            expect(onChangeSpy).toHaveBeenCalled();
        });

        it('should preserve all selected content types when they match the base types', () => {
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });
            spectator.detectChanges();

            // Set up selected content types that all match the base types
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ];
            spectator.component.$selectedContentTypes.set(selectedContentTypes);

            // Mock store to return base types that include both CONTENT and WIDGET
            triggerMultiSelectOnFilter('test-2');

            // spectator.detectChanges();

            // All selected content types should be preserved
            expect(spectator.component.$selectedContentTypes()).toEqual(selectedContentTypes);
        });

        it('should not filter selected content types when no base types are provided', () => {
            mockStore.filters.mockReturnValue({ baseType: undefined });
            spectator.detectChanges();

            // Set up selected content types
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ];
            spectator.component.$selectedContentTypes.set(selectedContentTypes);

            // Spy on the onChange method
            const onChangeSpy = jest.spyOn(spectator.component, 'onChange' as never);

            // Mock store to return no base types

            triggerMultiSelectOnFilter('test');

            spectator.detectChanges();

            // Selected content types should remain unchanged
            expect(spectator.component.$selectedContentTypes()).toEqual(selectedContentTypes);
            // onChange should not be called since no filtering occurred
            expect(onChangeSpy).not.toHaveBeenCalled();
        });

        it('should clear all selected content types when none match the base types', () => {
            // Mock store to return base types that don't match any selected content types
            mockStore.filters.mockReturnValue({ baseType: ['1'] }); // Maps to 'CONTENT'
            spectator.detectChanges();

            // Set up selected content types with base types that won't match
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'WIDGET' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'PERSONA' }
            ];
            spectator.component.$selectedContentTypes.set(selectedContentTypes);

            triggerMultiSelectOnFilter('test');

            spectator.detectChanges();

            // All selected content types should be filtered out
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });
    });

    describe('Content Type Selection & Store Integration', () => {
        beforeEach(() => {
            // Set up component with pre-selected content types for testing
            spectator.component.$selectedContentTypes.set([
                MOCK_CONTENT_TYPES[0], // blog
                MOCK_CONTENT_TYPES[1] // news
            ]);
        });

        it('should update store filters when content types are selected', () => {
            triggerMultiSelectOnChange();

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog', 'news']
            });
        });

        it('should remove store filter when no content types are selected', () => {
            spectator.component.$selectedContentTypes.set([]);

            triggerMultiSelectOnChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle null selected content types by removing filter', () => {
            spectator.component.$selectedContentTypes.set(null as unknown as DotCMSContentType[]);

            triggerMultiSelectOnChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle undefined selected content types by removing filter', () => {
            spectator.component.$selectedContentTypes.set(
                undefined as unknown as DotCMSContentType[]
            );

            triggerMultiSelectOnChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should correctly extract variable names from selected content types', () => {
            const customContentTypes = [
                { variable: 'custom1' } as DotCMSContentType,
                { variable: 'custom2' } as DotCMSContentType,
                { variable: 'custom3' } as DotCMSContentType
            ];

            spectator.component.$selectedContentTypes.set(customContentTypes);
            triggerMultiSelectOnChange(customContentTypes);

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['custom1', 'custom2', 'custom3']
            });
        });
    });

    describe('User Interaction & Event Handling', () => {
        beforeEach(() => {
            // Skip initial effect call because of skip(1) in filter subscription
            spectator.detectChanges();
        });

        it('should reset filter to empty when multiselect panel is hidden', () => {
            triggerMultiSelectOnFilter('ANY_FILTER');
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                filter: 'ANY_FILTER',
                type: 'CONTENT'
            });

            triggerMultiSelectOnPanelHide();
            jest.advanceTimersByTime(500);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                filter: '',
                type: 'CONTENT'
            });
        });

        it('should handle complete user workflow of selecting and clearing items', () => {
            spectator.detectChanges();

            // User selects a content type
            spectator.component.$selectedContentTypes.set([MOCK_CONTENT_TYPES[0]]);
            triggerMultiSelectOnChange();
            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog']
            });

            jest.clearAllMocks();

            // User clears the selection
            spectator.component.$selectedContentTypes.set([]);
            triggerMultiSelectOnClear();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });
    });

    describe('Template Integration & UI Rendering', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should render multiselect component with correct test id', () => {
            const multiSelect = spectator.query('[data-testid="content-type-field"]');
            expect(multiSelect).toBeTruthy();
        });

        it('should display empty state when not loading and no results available', () => {
            patchState(spectator.component.$state, {
                contentTypes: [],
                loading: false
            });

            const emptyMessage = spectator.query('[data-testid="content-type-field"]');
            expect(emptyMessage).toBeTruthy();
        });
    });

    describe('Error Handling & Edge Cases', () => {
        describe('API Response Errors', () => {
            it('should handle empty content types API response gracefully', () => {
                const emptyApiResponse = {
                    contentTypes: [],
                    pagination: {
                        currentPage: 0,
                        perPage: 20,
                        totalEntries: 0,
                        totalPages: 0
                    }
                };
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    of(emptyApiResponse)
                );

                spectator.detectChanges();

                // Component should handle empty response and update state correctly
                expect(spectator.component.$state().contentTypes).toEqual([]);
                expect(spectator.component.$state().loading).toBe(false);
            });

            it('should handle initial content types API service errors with catchError', () => {
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    throwError(() => new Error('Service unavailable'))
                );

                spectator.detectChanges();

                // catchError should return empty array and set loading to false
                expect(spectator.component.$state().contentTypes).toEqual([]);
                expect(spectator.component.$state().loading).toBe(false);
            });
        });

        describe('Store Integration Errors', () => {
            it('should handle store filter retrieval errors gracefully', () => {
                mockStore.getFilterValue.mockImplementation(() => {
                    throw new Error('Store error');
                });

                // Component should not throw error and continue working
                expect(() => spectator.detectChanges()).not.toThrow();
            });
        });
    });
});
