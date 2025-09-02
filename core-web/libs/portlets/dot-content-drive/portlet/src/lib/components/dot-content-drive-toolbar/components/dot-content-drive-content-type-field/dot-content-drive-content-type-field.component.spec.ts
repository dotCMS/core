import { describe, it, expect, beforeEach, afterEach, beforeAll } from '@jest/globals';
import { createComponentFactory, Spectator, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { MultiSelectFilterEvent } from 'primeng/multiselect';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveContentTypeFieldComponent } from './dot-content-drive-content-type-field.component';

import { DEFAULT_PAGINATION } from '../../../../shared/constants';
import { MOCK_CONTENT_TYPES, SELECTED_CONTENT_TYPES } from '../../../../shared/mocks';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveContentTypeFieldComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFieldComponent>;
    let mockStore: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let mockContentTypeService: SpyObject<DotContentTypeService>;

        // Approach 1: Using spectator's triggerEventHandler (most reliable for PrimeNG)
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
                getContentTypesWithPagination: jest.fn().mockReturnValue(of({
                    contentTypes: MOCK_CONTENT_TYPES,
                    pagination: {
                        currentPage: MOCK_CONTENT_TYPES.length,
                        totalEntries: MOCK_CONTENT_TYPES.length * 2,
                        totalPages: 1,
                    }
                }))
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
        jest.clearAllTimers();
        jest.clearAllMocks();
    });

    describe('Component Initialization', () => {
        it('should initialize with correct initial state', () => {
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().filter).toBe('');
            expect(spectator.component.$state().loading).toBe(true);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });

        it('should trigger initial API request on component initialization through effect', () => {
            spectator.detectChanges();
            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                filter: ''
            });
        });

        it('should filter out forms and system types from loaded content types', async () => {
            spectator.detectChanges();
            const expectedContentTypes = MOCK_CONTENT_TYPES.filter(
                (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
            );

            expect(spectator.component.$state().contentTypes).toEqual(expectedContentTypes);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should set selected content types based on store filters', () => {
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            const expectedSelected = SELECTED_CONTENT_TYPES.filter((ct) =>
                ['blog', 'news'].includes(ct.variable)
            ).map((ct) => (ct));

            expect(spectator.component.$selectedContentTypes()).toEqual(expectedSelected);
        });

        it('should handle empty content type filters from store', () => {
            mockStore.getFilterValue.mockReturnValue(null);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });
    });

    // describe('Loading State Management', () => {
    //     it('should set loading to true when starting a request through Subject', () => {
    //         // Trigger a new request via onFilter which updates state and triggers effect
    //         spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
    //         spectator.detectChanges();

    //         // The Subject subscription should immediately set loading to true and clear content types
    //         expect(spectator.component.$state().loading).toBe(true);
    //         expect(spectator.component.$state().contentTypes).toEqual([]);
    //     });

    //     it('should set loading to false when request completes successfully', () => {
    //         spectator.detectChanges();

    //         // Clear the initial request mock calls
    //         jest.clearAllMocks();

    //         spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
    //         expect(spectator.component.$state().loading).toBe(true);

    //         // Advance timers to complete the debounced request
    //         jest.advanceTimersByTime(500);

    //         expect(spectator.component.$state().loading).toBe(false);
    //         expect(spectator.component.$state().contentTypes.length).toBeGreaterThan(0);
    //     });

    //     it('should set loading to false when request fails', () => {
    //         // Mock the service to return an error for the filter request
    //         mockContentTypeService.getContentTypes.mockReturnValue(
    //             throwError(() => new Error('Network error'))
    //         );

    //         spectator.detectChanges();
    //         jest.clearAllMocks();

    //         spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
    //         expect(spectator.component.$state().loading).toBe(true);

    //         jest.advanceTimersByTime(500);

    //         expect(spectator.component.$state().loading).toBe(false);
    //         expect(spectator.component.$state().contentTypes).toEqual([]);
    //     });

    //     it('should handle multiple rapid requests with Subject-based debouncing', () => {
    //         spectator.detectChanges();

    //         // Make multiple rapid requests
    //         spectator.component.onFilter({ filter: 'first' } as MultiSelectFilterEvent);
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(200);

    //         spectator.component.onFilter({ filter: 'second' } as MultiSelectFilterEvent);
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(200);

    //         spectator.component.onFilter({ filter: 'third' } as MultiSelectFilterEvent);
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         // Should only make one API call for the last request due to debouncing
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: 'third'
    //         });
    //     });
    // });

    // fdescribe('Content Types Loading Effect', () => {
    //     it('should trigger effect on component initialization', () => {
    //         spectator.detectChanges();

    //         jest.advanceTimersByTime(500);

    //         // The effect should be triggered on init
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: ''
    //         });
    //     });

    //     it('should call getContentTypes with current filter from state', () => {
    //         spectator.detectChanges();
    //         jest.clearAllMocks();

    //         // Update filter which should trigger the effect
    //         triggerMultiSelectOnFilter('blog');
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: 'blog'
    //         });
    //     });

    //     it('should handle baseType filters (currently disabled)', () => {
    //         // This tests the current implementation where baseType is explicitly undefined
    //         mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });

    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         // Due to the TODO in the component, type is always undefined
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: ''
    //         });
    //     });

    //     it('should trigger effect when filter signal changes', () => {
    //         spectator.detectChanges();
    //         jest.clearAllMocks();

    //         // Use the component's method to update the state, which properly triggers effects
    //         triggerMultiSelectOnFilter('updated-filter');
    //         spectator.detectChanges(); // Trigger change detection after state update

    //         jest.advanceTimersByTime(500);

    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: 'updated-filter'
    //         });
    //     });

    //     it('should use switchMap to cancel previous requests', () => {
    //         spectator.detectChanges();
    //         jest.clearAllMocks();

    //         // First request
    //         triggerMultiSelectOnFilter('first');
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(200);

    //         // Second request before first completes (should cancel first)
    //         triggerMultiSelectOnFilter('second');
    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         // Should only have been called once with the final filter value
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
    //         expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
    //             type: undefined,
    //             filter: 'second'
    //         });
    //     });

    //     it('should handle error responses gracefully', () => {
    //         mockContentTypeService.getContentTypes.mockReturnValue(
    //             throwError(() => new Error('Network error'))
    //         );

    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         // Should set empty array on error and loading to false (catchError returns [])
    //         expect(spectator.component.$state().contentTypes).toEqual([]);
    //         expect(spectator.component.$state().loading).toBe(false);
    //     });

    //     it('should preserve selected content types', () => {
    //         mockContentTypeService.getContentTypes.mockReturnValue(of([MOCK_CONTENT_TYPES[1]]));
    //         spectator.component.$selectedContentTypes.set([SELECTED_CONTENT_TYPES[0]]);
    //         spectator.detectChanges();

    //         triggerMultiSelectOnFilter('test');

    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         expect(spectator.component.$state().contentTypes).toEqual([
    //             { ...SELECTED_CONTENT_TYPES[0], selected: true },
    //             SELECTED_CONTENT_TYPES[1]
    //         ]);
    //     });

    //     it('should preserve selected content types and remove duplicates', () => {
    //         spectator.component.$selectedContentTypes.set([
    //             SELECTED_CONTENT_TYPES[0],
    //             SELECTED_CONTENT_TYPES[1]
    //         ]);
    //         spectator.detectChanges();

    //         triggerMultiSelectOnFilter('test');

    //         mockContentTypeService.getContentTypes.mockReturnValue(of([MOCK_CONTENT_TYPES[0]]));

    //         spectator.detectChanges();
    //         jest.advanceTimersByTime(500);

    //         expect(spectator.component.$state().contentTypes).toEqual([
    //             { ...SELECTED_CONTENT_TYPES[0], selected: true },
    //             { ...SELECTED_CONTENT_TYPES[1], selected: true }
    //         ]);
    //     });
    // });

    describe('onFilter Method', () => {
        it('should update filter keyword in state immediately', () => {
            triggerMultiSelectOnFilter('test filter');
            spectator.detectChanges();

            expect(spectator.component.$state().filter).toBe('test filter');
        });

        it('should trigger effect when filter keyword changes', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            triggerMultiSelectOnFilter('blog');
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should handle empty filter', () => {
            triggerMultiSelectOnFilter('');

            expect(spectator.component.$state().filter).toBe('');
        });

        it('should handle null or undefined filter values', () => {
            triggerMultiSelectOnFilter(null as string | null);

            expect(spectator.component.$state().filter).toBeNull();
        });
    });

    describe('onChange Method', () => {
        beforeEach(() => {
            // Set up component with valid content types
            spectator.component.$selectedContentTypes.set([
                SELECTED_CONTENT_TYPES[0], // blog
                SELECTED_CONTENT_TYPES[1] // news
            ]);
        });

        it('should patch filters when content types are selected', () => {
            triggerMultiSelectOnChange();

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog', 'news']
            });
        });

        it('should remove filter when no content types are selected', () => {
            spectator.component.$selectedContentTypes.set([]);

            triggerMultiSelectOnChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle null selected content types', () => {
            spectator.component.$selectedContentTypes.set(
                null as unknown as DotCMSContentType[]
            );

            triggerMultiSelectOnChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle undefined selected content types', () => {
            spectator.component.$selectedContentTypes.set(
                undefined as unknown as DotCMSContentType[]
            );

            triggerMultiSelectOnChange();
 
            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should extract variable names correctly from selected content types', () => {
            const customContentTypes = [
                { variable: 'custom1' } as DotCMSContentType,
                { variable: 'custom2' } as DotCMSContentType,
                { variable: 'custom3' } as DotCMSContentType
            ];

            spectator.component.$selectedContentTypes.set(customContentTypes);
            triggerMultiSelectOnChange( customContentTypes );

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['custom1', 'custom2', 'custom3']
            });
        });
    });

    describe('Template Integration', () => {
        beforeEach(() => {
            // Overpass the initial effect call beacuse we have a skip(1) in the filter subscription
            spectator.detectChanges();
        });

        it('should render p-multiSelect component', () => {
            const multiSelect = spectator.query('[data-testid="content-type-field"]');
            expect(multiSelect).toBeTruthy();
        });

        it('should show empty state message when not loading and no results', () => {
            patchState(spectator.component.$state, {
                contentTypes: [],
                loading: false
            });

            const emptyMessage = spectator.query('[data-testid="content-type-field"]');
            expect(emptyMessage).toBeTruthy();
        });

        it('should set selected content types when multiselect value changes', () => {
            const customContentTypes = [
                { variable: 'custom1' } as DotCMSContentType,
                { variable: 'custom2' } as DotCMSContentType,
                { variable: 'custom3' } as DotCMSContentType
            ];

            spectator.component.$selectedContentTypes.set(customContentTypes);
            triggerMultiSelectOnChange( customContentTypes );

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['custom1', 'custom2', 'custom3']
            });
        });

        it('should trigger a api call when multiselect filter changes', () => {
            const filter = 'test';

            triggerMultiSelectOnFilter(filter);
            jest.advanceTimersByTime(500);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                filter
            });
        });

        it('should trigger a api call with empty filter when multiselect panel is hidden', () => {
            triggerMultiSelectOnFilter("ANY_FILTER");
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                filter: 'ANY_FILTER'
            });

            triggerMultiSelectOnPanelHide();
            jest.advanceTimersByTime(500);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                filter: ''
            });
        });

        it('should handle multiple rapid filter changes through DOM events', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            // Simulate rapid typing in the filter
            triggerMultiSelectOnFilter('b');
            triggerMultiSelectOnFilter('bl');
            triggerMultiSelectOnFilter('blo');
            triggerMultiSelectOnFilter('blog');

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Should debounce and only make one API call
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should simulate user selecting and clearing items', () => {
            spectator.detectChanges();
            
            // Set up some content types
            spectator.component.$selectedContentTypes.set([SELECTED_CONTENT_TYPES[0]]);
            
            // User selects items
            triggerMultiSelectOnChange();
            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog']
            });

            jest.clearAllMocks();

            // User clears selection
            spectator.component.$selectedContentTypes.set([]);
            triggerMultiSelectOnClear();
            
            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });
    });

    describe('Error Handling', () => {
        describe('loadInitialContentTypes', () => {
            it('should handle empty content types response gracefully', () => {
                const MOCK_RESPONSE =  {
                    contentTypes: [],
                    pagination: {
                        currentPage: 0,
                        perPage: 20,
                        totalEntries: 0,
                        totalPages: 0
                    }
                };
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(of(MOCK_RESPONSE));
    
                spectator.detectChanges();
    
                // The component should handle null response and set empty array
                expect(spectator.component.$state().contentTypes).toEqual([]);
                expect(spectator.component.$state().loading).toBe(false);
            });
    
            it('should gracefully handle service errors with catchError', () => {
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    throwError(() => new Error('Service unavailable'))
                );
    
                spectator.detectChanges();
    
                // catchError should return empty array, and loading should be false
                expect(spectator.component.$state().contentTypes).toEqual([]);
                expect(spectator.component.$state().loading).toBe(false);
            });
        });

        it('should handle store filter retrieval errors', () => {
            mockStore.getFilterValue.mockImplementation(() => {
                throw new Error('Store error');
            });

            // Should not throw and component should still work
            expect(() => spectator.detectChanges()).not.toThrow();
        });
    });
});
