import {
    createComponentFactory,
    createHostFactory,
    mockProvider,
    Spectator,
    SpectatorHost,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { Select, SelectLazyLoadEvent } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType, DotPagination } from '@dotcms/dotcms-models';

import { DotContentTypeComponent } from './dot-content-type.component';

const mockContentTypes: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'Blog'
    } as DotCMSContentType,
    {
        id: '2',
        name: 'News',
        variable: 'News'
    } as DotCMSContentType,
    {
        id: '3',
        name: 'Article',
        variable: 'Article'
    } as DotCMSContentType
];

const mockPagination: DotPagination = {
    currentPage: 1,
    perPage: 40,
    totalEntries: 100
};

@Component({
    selector: 'dot-test-host',
    template: '',
    standalone: false
})
class FormHostComponent {
    contentTypeControl = new FormControl<DotCMSContentType | null>(null);
    disabled = false;
}

describe('DotContentTypeComponent', () => {
    let spectator: Spectator<DotContentTypeComponent>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const createComponent = createComponentFactory({
        component: DotContentTypeComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotContentTypeService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        contentTypeService = spectator.inject(DotContentTypeService, true);
        contentTypeService.getContentTypesWithPagination.mockReturnValue(
            of({
                contentTypes: mockContentTypes,
                pagination: mockPagination
            })
        );
    });

    describe('Component Initialization', () => {
        it('should bind props correctly to p-select', () => {
            spectator.setInput('placeholder', 'Select content type');
            spectator.setInput('disabled', false);
            spectator.component.value.set(mockContentTypes[0]);
            spectator.detectChanges();

            const select = spectator.query(Select);

            expect(select.options).toEqual(expect.arrayContaining(mockContentTypes));
            expect(select.placeholder()).toBe('Select content type');
            expect(select.disabled()).toBe(false);
            expect(select.loading).toBe(false);
            expect(select.virtualScroll).toBe(true);
            expect(select.lazy).toBe(true);
            expect(select.filter).toBe(true);
        });

        it('should update disabled binding on p-select', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.disabled()).toBe(true);

            spectator.setInput('disabled', false);
            spectator.detectChanges();
            expect(select.disabled()).toBe(false);

            spectator.component.setDisabledState(true);
            spectator.detectChanges();
            expect(select.disabled()).toBe(true);
        });

        it('should bind placeholder to p-select', () => {
            spectator.setInput('placeholder', 'Choose a type');
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.placeholder()).toBe('Choose a type');

            spectator.setInput('placeholder', 'Select content');
            spectator.detectChanges();
            expect(select.placeholder()).toBe('Select content');
        });

        it('should sort content types alphabetically', () => {
            const unsortedContentTypes: DotCMSContentType[] = [
                { id: '1', name: 'Zebra', variable: 'Zebra' } as DotCMSContentType,
                { id: '2', name: 'Alpha', variable: 'Alpha' } as DotCMSContentType,
                { id: '3', name: 'Beta', variable: 'Beta' } as DotCMSContentType
            ];

            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: unsortedContentTypes,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();

            const sortedTypes = spectator.component.$state.contentTypes();
            expect(sortedTypes[0].name).toBe('Alpha');
            expect(sortedTypes[1].name).toBe('Beta');
            expect(sortedTypes[2].name).toBe('Zebra');
        });
    });

    describe('Lazy Loading', () => {
        beforeEach(() => {
            spectator.detectChanges();
            jest.clearAllMocks();
        });

        it('should handle lazy load events from PrimeNG Select', () => {
            spectator.detectChanges();
            jest.clearAllMocks();

            const lazyLoadEvent = { first: 40, last: 79 };

            spectator.triggerEventHandler(Select, 'onLazyLoad', lazyLoadEvent);
            spectator.detectChanges();

            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 2,
                per_page: 40
            });
        });

        it('should merge new content types without duplicates', () => {
            // Page 2 includes a duplicate (Blog) and new items (Event, Product)
            const page2ContentTypes: DotCMSContentType[] = [
                { id: '1', name: 'Blog', variable: 'Blog' } as DotCMSContentType, // Duplicate from page 1
                { id: '4', name: 'Event', variable: 'Event' } as DotCMSContentType,
                { id: '5', name: 'Product', variable: 'Product' } as DotCMSContentType
            ];

            // Reset and setup for page 2
            spectator = createComponent({ detectChanges: false });
            contentTypeService = spectator.inject(DotContentTypeService, true);
            contentTypeService.getContentTypesWithPagination
                .mockReturnValueOnce(
                    of({
                        contentTypes: mockContentTypes,
                        pagination: { ...mockPagination, currentPage: 1 }
                    })
                )
                .mockReturnValueOnce(
                    of({
                        contentTypes: page2ContentTypes,
                        pagination: { ...mockPagination, currentPage: 2 }
                    })
                );

            spectator.detectChanges();
            jest.clearAllMocks();

            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            const allContentTypes = spectator.component.$state.contentTypes();
            // Should have 5 items: Blog, News, Article (from page 1) + Event, Product (from page 2)
            // Blog should NOT be duplicated
            expect(allContentTypes.length).toBe(5);
            expect(allContentTypes.filter((ct) => ct.variable === 'Blog').length).toBe(1);
            expect(allContentTypes.find((ct) => ct.variable === 'Event')).toBeTruthy();
            expect(allContentTypes.find((ct) => ct.variable === 'Product')).toBeTruthy();
        });

        it('should not load duplicate pages', () => {
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Try to load page 2 again
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            // Should not make another call
            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();
        });

        it('should handle invalid lazy load events with NaN values', () => {
            const invalidEvent: Partial<SelectLazyLoadEvent> = { first: NaN, last: NaN };

            spectator.component.onLazyLoad(invalidEvent as SelectLazyLoadEvent);

            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();
        });

        it('should not load if currently loading', () => {
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: mockContentTypes,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            // Manually set loading state to true
            patchState(spectator.component.$state, { loading: true });

            // Try to load while still loading
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            // Should not make another call while loading
            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();

            // Cleanup
            patchState(spectator.component.$state, { loading: false });
        });

        it('should not load beyond total entries', () => {
            // Create new component instance with limited total entries
            spectator = createComponent({ detectChanges: false });
            contentTypeService = spectator.inject(DotContentTypeService, true);
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: mockContentTypes.slice(0, 2),
                    pagination: { ...mockPagination, totalEntries: 50 }
                })
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            // Component should know total is 50, page 2 would be beyond that
            // But the component loads page 2 if last index is 79 (which is < 50 in total)
            // Actually, page 2 calculation: Math.floor(79 / 40) + 1 = 2
            // maxPage would be Math.ceil(50 / 40) = 2, so page 2 is valid
            // So we need to test with a higher page
            spectator.component.onLazyLoad({ first: 80, last: 119 });
            spectator.detectChanges();

            // Page 3 would be beyond total of 50, so should not load
            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();
        });
    });

    describe('Filtering Functionality', () => {
        beforeEach(() => {
            spectator.detectChanges();
            jest.clearAllMocks();
        });

        it('should debounce filter changes', fakeAsync(() => {
            spectator.component.onFilterChange('blog');

            // Should not call immediately
            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();

            // After 300ms, should call
            tick(300);
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'blog'
            });
        }));

        it('should reset loaded pages when filtering', fakeAsync(() => {
            // Load page 2 first to mark it as loaded
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Apply filter - should reset loaded pages and clear content types
            spectator.component.onFilterChange('blog');
            tick(300);
            spectator.detectChanges();

            // Should reset and load page 1 with filter
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'blog'
            });

            // Verify that content types were cleared before filter load
            // The filter already loaded page 1, so now page 1 is in loadedPages
            // But if we try to load page 2 with filter, it should work since pages were reset
            jest.clearAllMocks();
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: [
                        { id: '1', name: 'Blog', variable: 'Blog' } as DotCMSContentType
                    ],
                    pagination: { ...mockPagination, totalEntries: 50 }
                })
            );
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            // Should make another call for page 2 since pages were reset by filter
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 2,
                per_page: 40,
                filter: 'blog'
            });
        }));

        it('should reset filter and reload when filter is cleared', fakeAsync(() => {
            // Apply a filter first
            spectator.component.onFilterChange('blog');
            tick(300);
            jest.clearAllMocks();

            // Clear filter
            spectator.component.onFilterChange('');
            tick(300);

            expect(spectator.component.$state.filterValue()).toBe('');
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should cancel previous debounce timeout on new filter', fakeAsync(() => {
            spectator.component.onFilterChange('blog');
            spectator.component.onFilterChange('news');
            tick(300);

            // Should only call once with 'news'
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledTimes(1);
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'news'
            });
        }));

        it('should trim filter value before passing to service', fakeAsync(() => {
            spectator.component.onFilterChange('  blog  ');
            tick(300);

            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'blog'
            });
        }));
    });

    describe('Component Inputs/Outputs', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should update value model signal', () => {
            const testValue = mockContentTypes[0];
            spectator.component.value.set(testValue);
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(testValue);
        });

        it('should emit onChange output when value changes', () => {
            spectator.detectChanges();
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            const selectedContentType = mockContentTypes[0];
            // Trigger onChange event from p-select component to test binding chain: (onChange)="onContentTypeChange($event.value)"
            spectator.triggerEventHandler(Select, 'onChange', { value: selectedContentType });
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(selectedContentType);
            expect(onChangeSpy).toHaveBeenCalledWith(selectedContentType);
        });

        it('should trigger ControlValueAccessor onChange when model signal changes', () => {
            const onChangeSpy = jest.fn();
            spectator.component.registerOnChange(onChangeSpy);

            const testValue = mockContentTypes[0];
            spectator.component.value.set(testValue);
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith(testValue);
        });

        it('should emit null when value is cleared', () => {
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            spectator.component.onContentTypeChange(null);

            expect(spectator.component.value()).toBeNull();
            expect(onChangeSpy).toHaveBeenCalledWith(null);
        });
    });

    describe('Edge Cases', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should handle null value in onContentTypeChange', () => {
            spectator.component.onContentTypeChange(null);

            expect(spectator.component.value()).toBeNull();
        });

        it('should handle empty content types array', () => {
            // Create new component instance with empty response
            spectator = createComponent({ detectChanges: false });
            contentTypeService = spectator.inject(DotContentTypeService, true);
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );

            spectator.detectChanges();

            expect(spectator.component.$state.contentTypes().length).toBe(0);
            expect(spectator.component.$state.totalRecords()).toBe(0);
        });

        it('should reset loading state on service error', () => {
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                throwError(() => new Error('API Error'))
            );

            spectator.detectChanges();

            expect(spectator.component.$state.loading()).toBe(false);
        });

        it('should handle filter with only whitespace', fakeAsync(() => {
            spectator.component.onFilterChange('   ');
            tick(300);

            // Should reset filter instead of calling with whitespace
            expect(spectator.component.$state.filterValue()).toBe('');
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should not add selected value to list when filtering', fakeAsync(() => {
            const filteredContentTypes: DotCMSContentType[] = [
                { id: '1', name: 'Blog', variable: 'Blog' } as DotCMSContentType
            ];

            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: filteredContentTypes,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();

            // Set a value that doesn't match the filter
            spectator.component.value.set(mockContentTypes[1]);
            spectator.component.onFilterChange('blog');
            tick(300);
            spectator.detectChanges();

            const contentTypes = spectator.component.$state.contentTypes();
            // Should not have the selected value if it doesn't match filter
            expect(contentTypes.find((ct) => ct.variable === 'News')).toBeFalsy();
        }));

        it('should handle lazy load event with undefined last', () => {
            const event: Partial<SelectLazyLoadEvent> = { first: 0 };

            spectator.component.onLazyLoad(event as SelectLazyLoadEvent);
            spectator.detectChanges();

            // Should use pageSize when last is undefined
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        });

        it('should handle writeValue when contentTypes list is empty', () => {
            // Create a new component instance with empty content types
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );

            spectator = createComponent({ detectChanges: false });
            contentTypeService = spectator.inject(DotContentTypeService, true);
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );
            spectator.detectChanges();

            const newContentType: DotCMSContentType = {
                id: '99',
                name: 'Custom',
                variable: 'Custom'
            } as DotCMSContentType;

            spectator.component.writeValue(newContentType);
            spectator.detectChanges();

            const contentTypes = spectator.component.$state.contentTypes();
            expect(contentTypes.find((ct) => ct.variable === 'Custom')).toBeTruthy();
        });
    });
});

describe('DotContentTypeComponent - ControlValueAccessor Integration', () => {
    const createHost = createHostFactory({
        component: DotContentTypeComponent,
        host: FormHostComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotContentTypeService),
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        detectChanges: false
    });

    let hostSpectator: SpectatorHost<DotContentTypeComponent, FormHostComponent>;
    let hostComponent: FormHostComponent;
    let hostContentTypeService: SpyObject<DotContentTypeService>;

    beforeEach(() => {
        hostSpectator = createHost(
            `<dot-content-type [formControl]="contentTypeControl" [disabled]="disabled"></dot-content-type>`
        );
        hostComponent = hostSpectator.hostComponent;
        hostContentTypeService = hostSpectator.inject(DotContentTypeService, true);

        hostContentTypeService.getContentTypesWithPagination.mockReturnValue(
            of({
                contentTypes: mockContentTypes,
                pagination: mockPagination
            })
        );

        hostSpectator.detectChanges();
    });

    it('should write value to component from FormControl', () => {
        const testValue = mockContentTypes[0];
        hostComponent.contentTypeControl.setValue(testValue);
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toEqual(testValue);
    });

    it('should handle null value from FormControl', () => {
        hostComponent.contentTypeControl.setValue(null);
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toBeNull();
    });

    it('should ensure value is in contentTypes list when FormControl sets value', () => {
        const newContentType: DotCMSContentType = {
            id: '99',
            name: 'Custom',
            variable: 'Custom'
        } as DotCMSContentType;

        hostComponent.contentTypeControl.setValue(newContentType);
        hostSpectator.detectChanges();

        const contentTypes = hostSpectator.component.$state.contentTypes();
        expect(contentTypes.find((ct) => ct.variable === 'Custom')).toBeTruthy();
    });

    it('should propagate value changes from component to FormControl', () => {
        const testValue = mockContentTypes[0];

        // Trigger onChange event from p-select component
        hostSpectator.triggerEventHandler(Select, 'onChange', { value: testValue });
        hostSpectator.detectChanges();

        expect(hostComponent.contentTypeControl.value).toEqual(testValue);
    });

    it('should mark FormControl as touched when user interacts', () => {
        const testValue = mockContentTypes[0];

        expect(hostComponent.contentTypeControl.touched).toBe(false);

        // Trigger onChange event from p-select component (user interaction)
        hostSpectator.triggerEventHandler(Select, 'onChange', { value: testValue });
        hostSpectator.detectChanges();

        expect(hostComponent.contentTypeControl.touched).toBe(true);
    });

    it('should set disabled state via FormControl', () => {
        hostComponent.contentTypeControl.disable();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.$isDisabled()).toBe(true);
        expect(hostSpectator.component.$disabled()).toBe(true);

        const select = hostSpectator.query(Select);
        expect(select.disabled()).toBe(true);
    });

    it('should combine input disabled and FormControl disabled state', () => {
        // Test with both input disabled=false and FormControl enabled
        hostComponent.disabled = false;
        hostComponent.contentTypeControl.enable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(false);

        // Test with input disabled=true (should override FormControl enabled)
        hostComponent.disabled = true;
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(true);

        // Test with input disabled=false but FormControl disabled (should be disabled)
        hostComponent.disabled = false;
        hostComponent.contentTypeControl.disable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(true);
    });

    it('should update component when FormControl value changes', () => {
        hostComponent.contentTypeControl.setValue(mockContentTypes[0]);
        hostSpectator.detectChanges();
        expect(hostSpectator.component.value()).toEqual(mockContentTypes[0]);

        hostComponent.contentTypeControl.setValue(mockContentTypes[1]);
        hostSpectator.detectChanges();
        expect(hostSpectator.component.value()).toEqual(mockContentTypes[1]);
    });

    it('should update FormControl when user selects a different value', () => {
        hostComponent.contentTypeControl.setValue(mockContentTypes[0]);
        hostSpectator.detectChanges();

        // User selects a different value through p-select
        hostSpectator.triggerEventHandler(Select, 'onChange', { value: mockContentTypes[1] });
        hostSpectator.detectChanges();

        expect(hostComponent.contentTypeControl.value).toEqual(mockContentTypes[1]);
    });
});
