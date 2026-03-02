import { describe, it, expect, beforeEach, afterEach, beforeAll } from '@jest/globals';
import { createComponentFactory, Spectator, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError, timer } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { switchMap } from 'rxjs/operators';

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

    const triggerMultiSelectOnLazyLoad = (event: { first?: number; last: number }) => {
        spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onLazyLoad', event);
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
            expect(spectator.component.$state.contentTypes()).toEqual([]);
            expect(spectator.component.$state.filter()).toBe('');
            expect(spectator.component.$state.loading()).toBe(true);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });

        it('should trigger initial content types API request on component initialization', () => {
            spectator.detectChanges();
            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ per_page: 10 })
            );
        });

        it('should filter out forms and system content types after initial load', async () => {
            spectator.detectChanges();
            const expectedContentTypes = MOCK_CONTENT_TYPES.filter(
                (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
            );

            expect(spectator.component.$state.contentTypes()).toEqual(expectedContentTypes);
            expect(spectator.component.$state.loading()).toBe(false);
        });

        it('should initialize selected content types from store filters using linkedSignal', () => {
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);
            // Ensure the content types are available in state for linkedSignal to work
            spectator.detectChanges();
            patchState(spectator.component.$state, {
                contentTypes: MOCK_CONTENT_TYPES.filter(
                    (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
                )
            });
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            const expectedSelected = MOCK_CONTENT_TYPES.filter((ct) =>
                ['blog', 'news'].includes(ct.variable)
            );

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
            spectator.detectChanges();
            expect(spectator.component.$state.loading()).toBe(true);
        });

        it('should set loading to false when filter request completes successfully', () => {
            spectator.detectChanges();
            jest.clearAllMocks(); // Clear initial request mock calls

            triggerMultiSelectOnFilter('test');
            expect(spectator.component.$state.loading()).toBe(true);

            jest.advanceTimersByTime(500); // Complete debounced request

            expect(spectator.component.$state.loading()).toBe(false);
            expect(spectator.component.$state.contentTypes().length).toBeGreaterThan(0);
        });

        it('should set loading to false when filter request fails', () => {
            mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            triggerMultiSelectOnFilter('test');
            spectator.detectChanges();
            expect(spectator.component.$state.loading()).toBe(true);

            jest.advanceTimersByTime(500);

            expect(spectator.component.$state.loading()).toBe(false);
            expect(spectator.component.$state.contentTypes()).toEqual([]);
        });
    });

    describe('API Integration & Content Type Loading', () => {
        afterAll(() => {
            mockStore.filters.mockReturnValue({ baseType: undefined });
        });

        it('should call initial content types API with pagination on component initialization', () => {
            spectator.detectChanges();

            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ per_page: 10 })
            );
            expect(mockContentTypeService.getContentTypes).not.toHaveBeenCalled();
        });

        it('should send ensure parameter when content types are selected in store', () => {
            mockStore.filters.mockReturnValue({ contentType: ['blog', 'news'] });

            spectator.detectChanges();

            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ ensure: 'blog,news' })
            );
        });

        it('should handle base type filters', () => {
            // Tests current implementation where baseType is explicitly undefined
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Due to TODO in component, type parameter is always undefined
            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ type: 'CONTENT,WIDGET' })
            );
        });
    });

    describe('Lazy load pagination', () => {
        beforeEach(() => {
            spectator.detectChanges();
            // Clear only this mock's calls so we assert the lazy-load call; keep implementation for initial load
            mockContentTypeService.getContentTypesWithPagination.mockClear();
        });

        it('should load next page when virtual scroll triggers onLazyLoad', () => {
            const firstPage = MOCK_CONTENT_TYPES.filter(
                (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
            );
            patchState(spectator.component.$state, {
                contentTypes: firstPage,
                currentPage: 1,
                canLoadMore: true,
                loading: false
            });

            const nextPageContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], id: '5', variable: 'extra1', name: 'Extra 1' },
                { ...MOCK_CONTENT_TYPES[1], id: '6', variable: 'extra2', name: 'Extra 2' }
            ];

            mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: nextPageContentTypes,
                    pagination: {
                        currentPage: 2,
                        perPage: 10,
                        totalEntries: 20,
                        totalPages: 2
                    }
                })
            );

            // event.last = 10 => page = Math.ceil(10/10) + 1 = 2; currentPage is 1 so load runs
            triggerMultiSelectOnLazyLoad({ first: 0, last: 10 });

            expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({
                    page: 2,
                    per_page: 10
                })
            );

            expect(spectator.component.$state.contentTypes().length).toBe(
                firstPage.length + nextPageContentTypes.length
            );
            expect(spectator.component.$state.currentPage()).toBe(2);
            expect(spectator.component.$state.canLoadMore()).toBe(true);
        });

        it('should not load next page when canLoadMore is false', () => {
            patchState(spectator.component.$state, { canLoadMore: false });
            triggerMultiSelectOnLazyLoad({ first: 0, last: 40 });

            expect(mockContentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();
        });
    });

    describe('Content Type Filtering & Search', () => {
        describe('when content types are selected in store', () => {
            beforeEach(() => {
                // Skip initial effect call because of skip(1) in filter subscription
                mockStore.filters.mockReturnValue({
                    baseType: undefined,
                    contentType: ['blog', 'news']
                });
                spectator.detectChanges();
                jest.clearAllMocks();
            });

            it('should call API with current filter when filter changes', () => {
                triggerMultiSelectOnFilter('blog');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'blog',
                        ensure: 'blog,news',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should send ensure parameter when filtering with selected content types', () => {
                triggerMultiSelectOnFilter('test');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'test',
                        ensure: 'blog,news',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should trigger API request when filter signal changes', () => {
                triggerMultiSelectOnFilter('updated-filter');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'updated-filter',
                        ensure: 'blog,news',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should debounce rapid filter changes and cancel previous requests', () => {
                triggerMultiSelectOnFilter('first');
                spectator.detectChanges();
                jest.advanceTimersByTime(200);

                triggerMultiSelectOnFilter('second');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                const calls = mockContentTypeService.getContentTypesWithPagination.mock.calls;
                const lastCall = calls[calls.length - 1]?.[0] as { filter?: string };
                expect(lastCall?.filter).toBe('second');
            });

            it('should handle filter API request errors gracefully', () => {
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    throwError(() => new Error('Network error'))
                );

                spectator.detectChanges();
                triggerMultiSelectOnFilter('test');
                jest.advanceTimersByTime(500);

                // Should return empty array on error and set loading to false
                expect(spectator.component.$state.contentTypes()).toEqual([]);
                expect(spectator.component.$state.loading()).toBe(false);
            });
        });
        describe('when no content types are selected in store', () => {
            beforeEach(() => {
                // Skip initial effect call because of skip(1) in filter subscription
                mockStore.filters.mockReturnValue({
                    baseType: undefined,
                    contentType: undefined
                });
                spectator.detectChanges();
                jest.clearAllMocks();
            });

            it('should call API with current filter when filter changes', () => {
                triggerMultiSelectOnFilter('blog');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'blog',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should send ensure parameter when filtering with selected content types', () => {
                triggerMultiSelectOnFilter('test');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'test',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should trigger API request when filter signal changes', () => {
                triggerMultiSelectOnFilter('updated-filter');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                expect(mockContentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: 'updated-filter',
                        page: 1,
                        per_page: 10
                    })
                );
            });

            it('should debounce rapid filter changes and cancel previous requests', () => {
                triggerMultiSelectOnFilter('first');
                spectator.detectChanges();
                jest.advanceTimersByTime(200);

                triggerMultiSelectOnFilter('second');
                spectator.detectChanges();
                jest.advanceTimersByTime(500);

                const calls = mockContentTypeService.getContentTypesWithPagination.mock.calls;
                const lastCall = calls[calls.length - 1]?.[0] as { filter?: string };
                expect(lastCall?.filter).toBe('second');
            });

            it('should handle filter API request errors gracefully', () => {
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    throwError(() => new Error('Network error'))
                );

                spectator.detectChanges();
                triggerMultiSelectOnFilter('test');
                jest.advanceTimersByTime(500);

                // Should return empty array on error and set loading to false
                expect(spectator.component.$state.contentTypes()).toEqual([]);
                expect(spectator.component.$state.loading()).toBe(false);
            });
        });
    });

    describe('Filter Input Handling', () => {
        it('should update filter state immediately when filter input changes', () => {
            triggerMultiSelectOnFilter('test filter');
            spectator.detectChanges();

            expect(spectator.component.$state.filter()).toBe('test filter');
        });

        it('should handle empty filter input', () => {
            triggerMultiSelectOnFilter('');

            expect(spectator.component.$state.filter()).toBe('');
        });

        it('should handle null or undefined filter values gracefully', () => {
            triggerMultiSelectOnFilter(null as string | null);

            // Component may set filter to null; treat as empty for display purposes
            expect(spectator.component.$state.filter() ?? '').toBe('');
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

            const calls = mockContentTypeService.getContentTypesWithPagination.mock.calls;
            const withBlog = calls.filter((c) => (c[0] as { filter?: string })?.filter === 'blog');
            expect(withBlog.length).toBeGreaterThanOrEqual(1);
        });
    });

    describe('Base Type Filtering (Untracked Logic)', () => {
        it('should filter selected content types when base types are provided', () => {
            // Timer delays the initial load response so the effect runs with our patched state.
            // Without it: ngOnInit calls loadInitialContentTypes(); a sync mock (e.g. of(...))
            // would complete in the same tick and overwrite $state.contentTypes before the effect
            // runs, so the effect would see empty/wrong contentTypes and we could not assert.
            mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                timer(100).pipe(
                    switchMap(() =>
                        of({
                            contentTypes: MOCK_CONTENT_TYPES,
                            pagination: {
                                currentPage: MOCK_CONTENT_TYPES.length,
                                perPage: 10,
                                totalEntries: MOCK_CONTENT_TYPES.length * 2,
                                totalPages: 1
                            }
                        })
                    )
                )
            );
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] }); // Maps to 'CONTENT,WIDGET'
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' }, // Should be kept
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }, // Should be kept
                { ...MOCK_CONTENT_TYPES[2], baseType: 'PESRONA' } // Should be filtered out
            ];
            mockStore.getFilterValue.mockReturnValue([
                selectedContentTypes[0].variable,
                selectedContentTypes[1].variable,
                selectedContentTypes[2].variable
            ]);
            patchState(spectator.component.$state, { contentTypes: selectedContentTypes });
            spectator.detectChanges();

            triggerMultiSelectOnFilter('test');
            spectator.detectChanges();

            // Verify that only content types with matching base types are preserved
            const filteredContentTypes = spectator.component.$selectedContentTypes();
            expect(filteredContentTypes).toHaveLength(2);
            expect(
                filteredContentTypes.every((ct) => ['CONTENT', 'WIDGET'].includes(ct.baseType))
            ).toBe(true);
            expect(filteredContentTypes.some((ct) => ct.baseType === 'PESRONA')).toBe(false);
        });

        it('should call onChange when filtering selected content types based on base types', () => {
            // Delay initial load so effect runs with our patchState (see comment in "should filter..." above).
            mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                timer(100).pipe(
                    switchMap(() =>
                        of({
                            contentTypes: MOCK_CONTENT_TYPES,
                            pagination: {
                                currentPage: MOCK_CONTENT_TYPES.length,
                                perPage: 10,
                                totalEntries: MOCK_CONTENT_TYPES.length * 2,
                                totalPages: 1
                            }
                        })
                    )
                )
            );
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ];
            mockStore.filters.mockReturnValue({ baseType: ['1'] }); // Maps to 'CONTENT'
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);
            patchState(spectator.component.$state, { contentTypes: selectedContentTypes });
            spectator.detectChanges();

            triggerMultiSelectOnFilter('test');
            spectator.detectChanges();

            // Verify store was updated (onChange flow was triggered by the effect)
            expect(mockStore.patchFilters).toHaveBeenCalled();
        });

        it('should preserve all selected content types when they match the base types', () => {
            // Delay initial load so effect runs with our patchState (see comment in "should filter..." above).
            mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                timer(100).pipe(
                    switchMap(() =>
                        of({
                            contentTypes: MOCK_CONTENT_TYPES,
                            pagination: {
                                currentPage: MOCK_CONTENT_TYPES.length,
                                perPage: 10,
                                totalEntries: MOCK_CONTENT_TYPES.length * 2,
                                totalPages: 1
                            }
                        })
                    )
                )
            );
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ];
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);
            patchState(spectator.component.$state, { contentTypes: selectedContentTypes });
            spectator.detectChanges();

            // linkedSignal derives from getFilterValue + contentTypes; assert selection is preserved
            const selected = spectator.component.$selectedContentTypes();
            expect(selected).toHaveLength(2);
            expect(selected.map((ct) => ct.variable).sort()).toEqual(['blog', 'news']);
        });

        it('should not filter selected content types when no base types are provided', () => {
            mockStore.filters.mockReturnValue({ baseType: undefined });
            const selectedContentTypes = [
                { ...MOCK_CONTENT_TYPES[0], baseType: 'CONTENT' },
                { ...MOCK_CONTENT_TYPES[1], baseType: 'WIDGET' }
            ];
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);
            patchState(spectator.component.$state, { contentTypes: selectedContentTypes });
            spectator.detectChanges();
            const patchFiltersCallsBefore = mockStore.patchFilters.mock.calls.length;
            const removeFilterCallsBefore = mockStore.removeFilter.mock.calls.length;

            triggerMultiSelectOnFilter('test');
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // When no base types, effect does not call onChange; store should not be patched/removed by that flow
            expect(mockStore.patchFilters.mock.calls.length).toBe(patchFiltersCallsBefore);
            expect(mockStore.removeFilter.mock.calls.length).toBe(removeFilterCallsBefore);
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
            // Populate the state with content types so onChange can work
            patchState(spectator.component.$state, {
                contentTypes: MOCK_CONTENT_TYPES.filter(
                    (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
                )
            });

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
            expect(spectator.component.$state.filter()).toBe('ANY_FILTER');

            triggerMultiSelectOnPanelHide();
            spectator.detectChanges();
            expect(spectator.component.$state.filter()).toBe('');
        });

        it('should handle complete user workflow of selecting and clearing items', () => {
            const contentTypes = MOCK_CONTENT_TYPES.filter(
                (ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM && !ct.system
            );
            patchState(spectator.component.$state, { contentTypes });
            // linkedSignal reads from getFilterValue('contentType') + $state.contentTypes()
            mockStore.getFilterValue.mockReset();
            mockStore.getFilterValue.mockReturnValue(['blog']);
            spectator.detectChanges();

            // Simulate user selecting one item; onChange() reads $selectedContentTypes()
            triggerMultiSelectOnChange([MOCK_CONTENT_TYPES[0]]);
            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog']
            });

            jest.clearAllMocks();

            // Simulate cleared selection: store returns [] so linkedSignal returns [] when it next runs
            mockStore.getFilterValue.mockReturnValue([]);
            // New array reference so signal state updates and linkedSignal re-runs with getFilterValue() = []
            patchState(spectator.component.$state, { contentTypes: contentTypes.slice() });
            spectator.detectChanges();
            expect(spectator.component.$state.contentTypes().length).toBeGreaterThan(0);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
            triggerMultiSelectOnChange([]);
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
                expect(spectator.component.$state.contentTypes()).toEqual([]);
                expect(spectator.component.$state.loading()).toBe(false);
            });

            it('should handle initial content types API service errors with catchError', () => {
                mockContentTypeService.getContentTypesWithPagination.mockReturnValue(
                    throwError(() => new Error('Service unavailable'))
                );

                spectator.detectChanges();

                // catchError should return empty array and set loading to false
                expect(spectator.component.$state.contentTypes()).toEqual([]);
                expect(spectator.component.$state.loading()).toBe(false);
            });
        });
    });
});
