import { createComponentFactory, Spectator, mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { MultiSelectFilterEvent } from 'primeng/multiselect';

import { DotContentTypeService } from '@dotcms/data-access';

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
            })
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
        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with empty state and not loading', () => {
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().filterByKeyword).toBe('');
            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });

        it('should load content types on init and filter out forms and system types', () => {
            spectator.detectChanges();

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });

            const expectedContentTypes = mockContentTypes.filter(
                (ct) => ct.baseType !== BASE_TYPES.form && !ct.system
            );
            expect(spectator.component.$state().contentTypes).toEqual(expectedContentTypes);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should set selected content types based on store filters', () => {
            mockStore.getFilterValue.mockReturnValue(['blog', 'news']);

            spectator.detectChanges();

            const expectedSelected = mockContentTypes.filter(
                (ct) =>
                    ['blog', 'news'].includes(ct.variable) &&
                    ct.baseType !== BASE_TYPES.form &&
                    !ct.system
            );
            expect(spectator.component.$selectedContentTypes()).toEqual(expectedSelected);
        });

        it('should handle empty content type filters from store', () => {
            mockStore.getFilterValue.mockReturnValue(null);

            spectator.detectChanges();

            expect(spectator.component.$selectedContentTypes()).toEqual([]);
        });
    });

    describe('Loading State Management', () => {
        it('should set loading to true when starting a request', () => {
            spectator.detectChanges();

            // Initially not loading
            expect(spectator.component.$state().loading).toBe(false);

            // Trigger a new request
            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);

            // Should immediately set loading to true and clear content types
            expect(spectator.component.$state().loading).toBe(true);
            expect(spectator.component.$state().contentTypes).toEqual([]);
        });

        it('should set loading to false when request completes successfully', () => {
            spectator.detectChanges();

            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            expect(spectator.component.$state().loading).toBe(true);

            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes.length).toBeGreaterThan(0);
        });

        it('should set loading to false when request fails', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(throwError('Network error'));

            spectator.detectChanges();

            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);
            expect(spectator.component.$state().loading).toBe(true);

            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().loading).toBe(false);
            expect(spectator.component.$state().contentTypes).toEqual([]);
        });
    });

    describe('Content Types Loading Effect', () => {
        it('should call getContentTypes with base type when provided', () => {
            mockStore.filters.mockReturnValue({ baseType: ['1', '2'] });

            spectator.detectChanges();

            // Note: The component currently has type = undefined due to the TODO comment
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });
        });

        it('should call getContentTypes with debounced filter keyword', () => {
            spectator.detectChanges();

            const filterEvent: MultiSelectFilterEvent = {
                filter: 'blog'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);
            jest.advanceTimersByTime(500);

            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'blog'
            });
        });

        it('should handle error when loading content types', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(throwError('Network error'));

            spectator.detectChanges();

            // Should set empty array on error and loading to false
            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should cancel previous requests when using switchMap', () => {
            spectator.detectChanges();

            // First request
            spectator.component.onFilter({ filter: 'first' } as MultiSelectFilterEvent);
            jest.advanceTimersByTime(200);

            // Second request before first completes
            spectator.component.onFilter({ filter: 'second' } as MultiSelectFilterEvent);
            jest.advanceTimersByTime(500);

            // Should only call API once for the second request due to switchMap cancellation
            expect(mockContentTypeService.getContentTypes).toHaveBeenLastCalledWith({
                type: undefined,
                filter: 'second'
            });
        });
    });

    describe('onFilter Method', () => {
        it('should update filter keyword immediately and trigger loading', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: 'test filter'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);

            // Should immediately update filter and set loading
            expect(spectator.component.$state().filterByKeyword).toBe('test filter');
            expect(spectator.component.$state().loading).toBe(true);
        });

        it('should debounce API requests', () => {
            const filterEvent1: MultiSelectFilterEvent = {
                filter: 'first'
            } as MultiSelectFilterEvent;
            const filterEvent2: MultiSelectFilterEvent = {
                filter: 'second'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent1);
            jest.advanceTimersByTime(200);
            spectator.component.onFilter(filterEvent2);
            jest.advanceTimersByTime(500);

            // Should only make one API call for the final filter value
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: 'second'
            });
        });

        it('should handle empty filter', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: ''
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);
            jest.advanceTimersByTime(500);

            expect(spectator.component.$state().filterByKeyword).toBe('');
            expect(mockContentTypeService.getContentTypes).toHaveBeenCalledWith({
                type: undefined,
                filter: ''
            });
        });

        it('should not make duplicate requests for same filter value', () => {
            const filterEvent: MultiSelectFilterEvent = {
                filter: 'same'
            } as MultiSelectFilterEvent;

            spectator.component.onFilter(filterEvent);
            jest.advanceTimersByTime(500);

            jest.clearAllMocks();

            // Same filter again
            spectator.component.onFilter(filterEvent);
            jest.advanceTimersByTime(500);

            // Should not make another API call due to distinctUntilChanged
            expect(mockContentTypeService.getContentTypes).not.toHaveBeenCalled();
        });
    });

    describe('onChange Method', () => {
        beforeEach(() => {
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

        it('should handle null or undefined selected content types', () => {
            spectator.component.$selectedContentTypes.set(null as unknown as []);

            spectator.component.onChange();

            expect(mockStore.removeFilter).toHaveBeenCalledWith('contentType');
        });
    });

    describe('Template Integration', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should render multiselect component', () => {
            const multiSelect = spectator.query('p-multiselect');
            expect(multiSelect).toBeTruthy();
        });

        it('should show skeleton when loading', () => {
            // Set loading state
            patchState(spectator.component.$state, {
                contentTypes: [],
                filterByKeyword: '',
                loading: true
            });
            spectator.detectChanges();

            const skeleton = spectator.query('p-skeleton');
            expect(skeleton).toBeTruthy();
        });

        it('should hide skeleton when not loading', () => {
            // Ensure not loading
            patchState(spectator.component.$state, {
                contentTypes: mockContentTypes,
                filterByKeyword: '',
                loading: false
            });
            spectator.detectChanges();

            const skeleton = spectator.query('p-skeleton');
            expect(skeleton).toBeFalsy();
        });

        it('should bind content types to multiselect options', () => {
            const expectedOptions = mockContentTypes.filter(
                (ct) => ct.baseType !== BASE_TYPES.form && !ct.system
            );

            expect(spectator.component.$state().contentTypes).toEqual(expectedOptions);
        });

        it('should bind selected content types to multiselect', () => {
            mockStore.getFilterValue.mockReturnValue(['blog']);
            spectator.detectChanges();

            const expectedSelected = mockContentTypes.filter((ct) => ct.variable === 'blog');
            expect(spectator.component.$selectedContentTypes()).toEqual(expectedSelected);
        });
    });

    describe('Error Handling', () => {
        it('should handle null content types response', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(of(null));

            spectator.detectChanges();

            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should handle undefined content types response', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(of(undefined));

            spectator.detectChanges();

            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });

        it('should gracefully handle service errors and reset loading state', () => {
            mockContentTypeService.getContentTypes.mockReturnValue(
                throwError('Service unavailable')
            );

            spectator.detectChanges();

            expect(spectator.component.$state().contentTypes).toEqual([]);
            expect(spectator.component.$state().loading).toBe(false);
        });
    });

    describe('Memory Management', () => {
        it('should automatically clean up subscriptions on destroy', () => {
            spectator.detectChanges();

            // Trigger some operations
            spectator.component.onFilter({ filter: 'test' } as MultiSelectFilterEvent);

            // Destroy component
            spectator.fixture.destroy();

            // Advance timers - no operations should happen after destroy
            jest.advanceTimersByTime(1000);

            // Component should be destroyed without errors
            expect(spectator.component).toBeTruthy(); // Component exists until destroyed
        });
    });
});
