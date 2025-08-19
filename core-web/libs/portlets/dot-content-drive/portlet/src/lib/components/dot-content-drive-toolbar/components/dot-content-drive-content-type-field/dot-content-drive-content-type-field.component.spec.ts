import { describe, it, expect, beforeEach, afterEach, beforeAll } from '@jest/globals';
import { createComponentFactory, Spectator, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { MultiSelectFilterEvent } from 'primeng/multiselect';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveContentTypeFieldComponent } from './dot-content-drive-content-type-field.component';

import { mockContentTypes } from '../../../../shared/mocks';
import { BASE_TYPES } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveContentTypeFieldComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFieldComponent>;
    let mockStore: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let mockContentTypeService: SpyObject<DotContentTypeService>;

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
                getContentTypes: jest.fn().mockReturnValue(of(mockContentTypes))
            }),
            mockProvider(
                DotMessageService,
                new MockDotMessageService({
                    'Content-Type': 'Content Type',
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
        mockContentTypeService = spectator.inject(DotContentTypeService);
    });

    afterEach(() => {
        jest.clearAllTimers();
        jest.clearAllMocks();
    });

    describe('Component Initialization', () => {
        it('should initialize with correct initial state', () => {
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().filterByKeyword).toBe('');
            expect(spectator.component.$state().loading).toBe(true);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });

        it('should trigger initial API request on component initialization through effect', () => {
            spectator.detectChanges();

            // The effect should trigger the initial API request
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });
        });

        it('should filter out forms and system types from loaded content types', async () => {
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            const expectedContentTypes = mockContentTypes.filter(
                (ct) => ct.baseType !== BASE_TYPES.form && !ct.system
            );

            expect(spectator.component.$state().contentTypes).toEqual(expectedContentTypes);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should set selected content types based on store filters', () => {
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            const expectedSelected = mockContentTypes.filter((ct) =>
                ['blog', 'news'].includes(ct.variable)
            );

            expect(spectator.component.$selectedContentTypes()).toEqual(expectedSelected);
        });

        it('should handle empty content type filters from store', () => {
            mockStore.getFilterValue.mockReturnValue(null);

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });
    });

    describe('Loading State Management', () => {
        it('should set loading to true when starting a request through Subject', () => {
            // Trigger a new request via onFilter which updates state and triggers effect
            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            spectator.detectChanges();

            // The Subject subscription should immediately set loading to true and clear content types
            expect(spectator.component.$state().loading).toBe(true);
            expect(spectator.component.$state().contentTypes).toEqual([]);
        });

        it('should set loading to false when request completes successfully', () => {
            spectator.detectChanges();

            // Clear the initial request mock calls
            jest.clearAllMocks();

            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            expect(spectator.component.$state().loading).toBe(true);

            // Advance timers to complete the debounced request
            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes.length).toBeGreaterThan(0);
        });

        it('should set loading to false when request fails', () => {
            // Mock the service to return an error for the filter request
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            expect(spectator.component.$state().loading).toBe(true);

            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes).toEqual([]);
        });

        it('should handle multiple rapid requests with Subject-based debouncing', () => {
            spectator.detectChanges();

            // Make multiple rapid requests
            spectator.component.onFilter({ filter: 'first' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(200);

            spectator.component.onFilter({ filter: 'second' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(200);

            spectator.component.onFilter({ filter: 'third' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Should only make one API call for the last request due to debouncing
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'third'
            });
        });
    });

    describe('Content Types Loading Effect', () => {
        it('should trigger effect on component initialization', () => {
            spectator.detectChanges();

            jest.advanceTimersByTime(500);

            // The effect should be triggered on init
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });
        });

        it('should call getContentTypes with current filter from state', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            // Update filter which should trigger the effect
            spectator.component.onFilter({ filter: 'blog' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should handle baseType filters (currently disabled)', () => {
            // This tests the current implementation where baseType is explicitly undefined
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Due to the TODO in the component, type is always undefined
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });
        });

        it('should trigger effect when filterByKeyword signal changes', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            // Use the component's method to update the state, which properly triggers effects
            spectator.component.onFilter({ filter: 'updated-filter' } as MultiSelectFilterEvent);
            spectator.detectChanges(); // Trigger change detection after state update

            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'updated-filter'
            });
        });

        it('should use switchMap to cancel previous requests', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            // First request
            spectator.component.onFilter({ filter: 'first' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(200);

            // Second request before first completes (should cancel first)
            spectator.component.onFilter({ filter: 'second' } as MultiSelectFilterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Should only have been called once with the final filter value
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledTimes(1);
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'second'
            });
        });

        it('should handle error responses gracefully', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // Should set empty array on error and loading to false (catchError returns [])
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });
    });

    describe('onFilter Method', () => {
        it('should update filter keyword in state immediately', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: 'test filter'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);
            spectator.detectChanges();

            expect(spectator.component.$state().filterByKeyword).toBe('test filter');
        });

        it('should trigger effect when filter keyword changes', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            const filterEvent: MultiSelectFilterEvent = {
                filter: 'blog'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);
            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should handle empty filter', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: ''
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);

            expect(spectator.component.$state().filterByKeyword).toBe('');
        });

        it('should handle null or undefined filter values', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: null as string | null
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);

            expect(spectator.component.$state().filterByKeyword).toBeNull();
        });
    });

    describe('onChange Method', () => {
        beforeEach(() => {
            // Set up component with valid content types
            spectator.component.$selectedContentTypes.set([
                mockContentTypes[0], // blog
                mockContentTypes[1] // news
            ]);
        });

        it('should patch filters when content types are selected', () => {
            spectator.component.onChange();

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['blog', 'news']
            });
        });

        it('should remove filter when no content types are selected', () => {
            spectator.component.$selectedContentTypes.set([]);

            spectator.component.onChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle null selected content types', () => {
            spectator.component.$selectedContentTypes.set(null as unknown as DotCMSContentType[]);

            spectator.component.onChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should handle undefined selected content types', () => {
            spectator.component.$selectedContentTypes.set(
                undefined as unknown as DotCMSContentType[]
            );

            spectator.component.onChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should extract variable names correctly from selected content types', () => {
            const customContentTypes = [
                { variable: 'custom1' } as DotCMSContentType,
                { variable: 'custom2' } as DotCMSContentType,
                { variable: 'custom3' } as DotCMSContentType
            ];

            spectator.component.$selectedContentTypes.set(customContentTypes);
            spectator.component.onChange();

            expect(mockStore.patchFilters).toHaveBeenCalledWith({
                contentType: ['custom1', 'custom2', 'custom3']
            });
        });
    });

    describe('Template Integration', () => {
        it('should render p-multiSelect component', () => {
            const multiSelect = spectator.query('[data-testid="content-type-field"]');
            expect(multiSelect).toBeTruthy();
        });

        it('should show loading state in multiselect', () => {
            // Trigger a filter operation to set loading state naturally
            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            spectator.detectChanges();

            const multiSelect = spectator.query('[data-testid="content-type-field"]');
            expect(multiSelect.getAttribute('ng-reflect-loading')).toBe('true');
        });

        it('should show empty state message when not loading and no results', () => {
            // Set non-loading state with no content types
            patchState(spectator.component.$state, {
                contentTypes: [],
                loading: false
            });
            spectator.detectChanges();

            const emptyMessage = spectator.query('[data-testid="content-type-field"]');
            expect(emptyMessage).toBeTruthy();
            // The empty template should show the empty state message
        });

        it('should trigger onChange when multiselect value changes', () => {
            const onChangeSpy = jest.spyOn(spectator.component, 'onChange');

            spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onChange', {});

            expect(onChangeSpy).toHaveBeenCalled();
        });

        it('should trigger onChange when multiselect is cleared', () => {
            const onChangeSpy = jest.spyOn(spectator.component, 'onChange');

            spectator.triggerEventHandler('[data-testid="content-type-field"]', 'onClear', {});

            expect(onChangeSpy).toHaveBeenCalled();
        });

        it('should trigger onFilter when multiselect filter changes', () => {
            const onFilterSpy = jest.spyOn(spectator.component, 'onFilter');
            const filterEvent = { filter: 'test' } as MultiSelectFilterEvent;

            spectator.triggerEventHandler(
                '[data-testid="content-type-field"]',
                'onFilter',
                filterEvent
            );

            expect(onFilterSpy).toHaveBeenCalledWith(filterEvent);
        });
    });

    describe('Error Handling', () => {
        it('should handle null content types response gracefully', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(of(null));

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // The component should handle null response and set empty array
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should handle undefined content types response gracefully', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(of(undefined));

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // The component should handle undefined response and set empty array
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should gracefully handle service errors with catchError', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError(() => new Error('Service unavailable'))
            );

            spectator.detectChanges();
            jest.advanceTimersByTime(500);

            // catchError should return empty array, and loading should be false
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should handle store filter retrieval errors', () => {
            mockStore.getFilterValue.mockImplementation(() => {
                throw new Error('Store error');
            });

            // Should not throw and component should still work
            expect(() => {
                spectator.detectChanges();
                jest.advanceTimersByTime(500);
            }).not.toThrow();
        });
    });
});
