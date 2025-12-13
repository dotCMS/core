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

import { DotSiteService } from '@dotcms/data-access';
import { Site } from '@dotcms/dotcms-js';
import { DotPagination } from '@dotcms/dotcms-models';

import { DotSiteComponent } from './dot-site.component';

const mockSites: Site[] = [
    {
        hostname: 'example.com',
        type: 'host',
        identifier: 'site1',
        archived: false
    } as Site,
    {
        hostname: 'demo.com',
        type: 'host',
        identifier: 'site2',
        archived: false
    } as Site,
    {
        hostname: 'test.com',
        type: 'host',
        identifier: 'site3',
        archived: false
    } as Site
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
    siteControl = new FormControl<Site | null>(null);
    disabled = false;
}

describe('DotSiteComponent', () => {
    let spectator: Spectator<DotSiteComponent>;
    let siteService: SpyObject<DotSiteService>;

    const createComponent = createComponentFactory({
        component: DotSiteComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotSiteService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        siteService = spectator.inject(DotSiteService, true);
        siteService.getSitesWithPagination.mockReturnValue(
            of({
                sites: mockSites,
                pagination: mockPagination
            })
        );
    });

    describe('Component Initialization', () => {
        it('should bind props correctly to p-select', () => {
            spectator.setInput('placeholder', 'Select site');
            spectator.setInput('disabled', false);
            spectator.component.value.set(mockSites[0]);
            spectator.detectChanges();

            const select = spectator.query(Select);

            expect(select.options).toEqual(expect.arrayContaining(mockSites));
            expect(select.placeholder()).toBe('Select site');
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

        it('should sort sites alphabetically by hostname', () => {
            const unsortedSites: Site[] = [
                { hostname: 'zebra.com', type: 'host', identifier: 'site1', archived: false } as Site,
                { hostname: 'alpha.com', type: 'host', identifier: 'site2', archived: false } as Site,
                { hostname: 'beta.com', type: 'host', identifier: 'site3', archived: false } as Site
            ];

            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: unsortedSites,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();

            const sortedSites = spectator.component.$state.sites();
            expect(sortedSites[0].hostname).toBe('alpha.com');
            expect(sortedSites[1].hostname).toBe('beta.com');
            expect(sortedSites[2].hostname).toBe('zebra.com');
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

            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 2,
                per_page: 40
            });
        });

        it('should merge new sites without duplicates', () => {
            // Page 2 includes a duplicate (example.com) and new items (event.com, product.com)
            const page2Sites: Site[] = [
                { hostname: 'example.com', type: 'host', identifier: 'site1', archived: false } as Site, // Duplicate from page 1
                { hostname: 'event.com', type: 'host', identifier: 'site4', archived: false } as Site,
                { hostname: 'product.com', type: 'host', identifier: 'site5', archived: false } as Site
            ];

            // Reset and setup for page 2
            spectator = createComponent({ detectChanges: false });
            siteService = spectator.inject(DotSiteService, true);
            siteService.getSitesWithPagination
                .mockReturnValueOnce(
                    of({
                        sites: mockSites,
                        pagination: { ...mockPagination, currentPage: 1 }
                    })
                )
                .mockReturnValueOnce(
                    of({
                        sites: page2Sites,
                        pagination: { ...mockPagination, currentPage: 2 }
                    })
                );

            spectator.detectChanges();
            jest.clearAllMocks();

            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            const allSites = spectator.component.$state.sites();
            // Should have 5 items: example.com, demo.com, test.com (from page 1) + event.com, product.com (from page 2)
            // example.com should NOT be duplicated
            expect(allSites.length).toBe(5);
            expect(allSites.filter((s) => s.identifier === 'site1').length).toBe(1);
            expect(allSites.find((s) => s.identifier === 'site4')).toBeTruthy();
            expect(allSites.find((s) => s.identifier === 'site5')).toBeTruthy();
        });

        it('should not load duplicate pages', () => {
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Try to load page 2 again
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            // Should not make another call
            expect(siteService.getSitesWithPagination).not.toHaveBeenCalled();
        });

        it('should handle invalid lazy load events with NaN values', () => {
            const invalidEvent: Partial<SelectLazyLoadEvent> = { first: NaN, last: NaN };

            spectator.component.onLazyLoad(invalidEvent as SelectLazyLoadEvent);

            expect(siteService.getSitesWithPagination).not.toHaveBeenCalled();
        });

        it('should not load if currently loading', () => {
            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: mockSites,
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
            expect(siteService.getSitesWithPagination).not.toHaveBeenCalled();

            // Cleanup
            patchState(spectator.component.$state, { loading: false });
        });

        it('should not load beyond total entries', () => {
            // Create new component instance with limited total entries
            spectator = createComponent({ detectChanges: false });
            siteService = spectator.inject(DotSiteService, true);

            // First load pages 1 and 2 to set up the state
            siteService.getSitesWithPagination
                .mockReturnValueOnce(
                    of({
                        sites: mockSites,
                        pagination: { ...mockPagination, currentPage: 1, totalEntries: 50 }
                    })
                )
                .mockReturnValueOnce(
                    of({
                        sites: mockSites,
                        pagination: { ...mockPagination, currentPage: 2, totalEntries: 50 }
                    })
                );

            spectator.detectChanges();

            // Load page 2 to mark it as loaded
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();

            // Manually set totalRecords to 50 for this test.
            // Note: The component normally uses HARDCODED_TOTAL_ENTRIES (501) because the /api/v1/site endpoint
            // currently returns incorrect totalEntries in the pagination response (returns page size instead of actual total).
            // This is a temporary workaround until the backend is fixed to return the correct totalEntries value.
            patchState(spectator.component.$state, { totalRecords: 50 });

            jest.clearAllMocks();

            // Now try to load page 3, which would be beyond total of 50
            // The component should check and not load page 3
            spectator.component.onLazyLoad({ first: 80, last: 119 });
            spectator.detectChanges();

            // Page 3 would be beyond total of 50, so should not load
            expect(siteService.getSitesWithPagination).not.toHaveBeenCalled();
        });
    });

    describe('Filtering Functionality', () => {
        beforeEach(() => {
            spectator.detectChanges();
            jest.clearAllMocks();
        });

        it('should debounce filter changes', fakeAsync(() => {
            spectator.component.onFilterChange('example');

            // Should not call immediately
            expect(siteService.getSitesWithPagination).not.toHaveBeenCalled();

            // After 300ms, should call
            tick(300);
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'example'
            });
        }));

        it('should reset loaded pages when filtering', fakeAsync(() => {
            // Load page 2 first to mark it as loaded
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Apply filter - should reset loaded pages and clear sites
            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            // Should reset and load page 1 with filter
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'example'
            });

            // Verify that sites were cleared before filter load
            // The filter already loaded page 1, so now page 1 is in loadedPages
            // But if we try to load page 2 with filter, it should work since pages were reset
            jest.clearAllMocks();
            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: [
                        { hostname: 'example.com', type: 'host', identifier: 'site1', archived: false } as Site
                    ],
                    pagination: { ...mockPagination, totalEntries: 50 }
                })
            );
            spectator.component.onLazyLoad({ first: 40, last: 79 });
            spectator.detectChanges();
            // Should make another call for page 2 since pages were reset by filter
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 2,
                per_page: 40,
                filter: 'example'
            });
        }));

        it('should reset filter and reload when filter is cleared', fakeAsync(() => {
            // Apply a filter first
            spectator.component.onFilterChange('example');
            tick(300);
            jest.clearAllMocks();

            // Clear filter
            spectator.component.onFilterChange('');
            tick(300);

            expect(spectator.component.$state.filterValue()).toBe('');
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should cancel previous debounce timeout on new filter', fakeAsync(() => {
            spectator.component.onFilterChange('example');
            spectator.component.onFilterChange('demo');
            tick(300);

            // Should only call once with 'demo'
            expect(siteService.getSitesWithPagination).toHaveBeenCalledTimes(1);
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'demo'
            });
        }));

        it('should trim filter value before passing to service', fakeAsync(() => {
            spectator.component.onFilterChange('  example  ');
            tick(300);

            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                filter: 'example'
            });
        }));
    });

    describe('Component Inputs/Outputs', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should update value model signal', () => {
            const testValue = mockSites[0];
            spectator.component.value.set(testValue);
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(testValue);
        });

        it('should emit onChange output when value changes', () => {
            spectator.detectChanges();
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            const selectedSite = mockSites[0];
            // Trigger onChange event from p-select component to test binding chain: (onChange)="onSiteChange($event.value)"
            spectator.triggerEventHandler(Select, 'onChange', { value: selectedSite });
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(selectedSite);
            expect(onChangeSpy).toHaveBeenCalledWith(selectedSite);
        });

        it('should trigger ControlValueAccessor onChange when model signal changes', () => {
            const onChangeSpy = jest.fn();
            spectator.component.registerOnChange(onChangeSpy);

            const testValue = mockSites[0];
            spectator.component.value.set(testValue);
            spectator.detectChanges();

            expect(onChangeSpy).toHaveBeenCalledWith(testValue);
        });

        it('should emit null when value is cleared', () => {
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            spectator.component.onSiteChange(null);

            expect(spectator.component.value()).toBeNull();
            expect(onChangeSpy).toHaveBeenCalledWith(null);
        });
    });

    describe('Pinned Option Functionality', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show pinnedOption at the top of $options when writeValue is called', () => {
            const testValue = mockSites[0];
            const loadedSites = [mockSites[1], mockSites[2]];

            patchState(spectator.component.$state, { sites: loadedSites });
            spectator.component.writeValue(testValue);

            const options = spectator.component.$options();
            expect(options[0]).toEqual(testValue);
            expect(options.length).toBe(3);
        });

        it('should update pinnedOption when p-select onChange is triggered', () => {
            const testValue = mockSites[1];
            spectator.triggerEventHandler(Select, 'onChange', { value: testValue });
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.options[0]).toEqual(testValue);
        });

        it('should set pinnedOption to null when p-select onChange is triggered with null', () => {
            const select = spectator.query(Select);
            const testValue = mockSites[0];

            spectator.triggerEventHandler(Select, 'onChange', { value: testValue });
            spectator.detectChanges();
            expect(select.options[0]).toEqual(testValue);

            spectator.triggerEventHandler(Select, 'onChange', { value: null });
            spectator.detectChanges();
            expect(select.options[0]).not.toEqual(testValue);
        });

        it('should return only loaded options when pinnedOption is null', () => {
            const loadedSites = mockSites.slice(0, 2);
            patchState(spectator.component.$state, { sites: loadedSites, pinnedOption: null });

            const options = spectator.component.$options();
            expect(options).toEqual(loadedSites);
            expect(options.length).toBe(2);
        });

        it('should show pinnedOption at the top of $options', () => {
            const pinned = mockSites[0];
            const loadedSites = [mockSites[1], mockSites[2]];

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: loadedSites
            });

            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.options[0]).toEqual(pinned);
        });

        it('should filter out pinnedOption from loaded options to avoid duplicates', () => {
            const pinned = mockSites[0];
            // Loaded sites include the pinned option (duplicate)
            const loadedSites = [mockSites[0], mockSites[1], mockSites[2]];

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: loadedSites
            });

            spectator.detectChanges();

            const select = spectator.query(Select);

            // Should have pinned at top, then only demo.com and test.com (example.com filtered out)
            expect(select.options[0]).toEqual(pinned);
            expect(select.options.length).toBe(3);
            // Verify example.com only appears once (as pinned)
            const exampleCount = select.options.filter((s) => s.identifier === 'site1').length;
            expect(exampleCount).toBe(1);
        });

        it('should show pinnedOption when filtering if it matches the filter by hostname', fakeAsync(() => {
            const pinned: Site = {
                hostname: 'CustomExample.com',
                type: 'host',
                identifier: 'site99',
                archived: false
            } as Site;

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: mockSites
            });

            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const select = spectator.query(Select);
            // Pinned should appear at top if it matches filter
            expect(select.options[0]).toEqual(pinned);
        }));

        it('should not show pinnedOption when filtering if it does not match the filter', fakeAsync(() => {
            const pinned: Site = {
                hostname: 'Custom.com',
                type: 'host',
                identifier: 'site99',
                archived: false
            } as Site;

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: mockSites
            });

            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const options = spectator.component.$options();
            // Pinned should not appear if it doesn't match filter
            expect(options.find((s) => s.identifier === 'site99')).toBeFalsy();
            // Should only show filtered results
            expect(options.length).toBeGreaterThan(0);
        }));

        it('should show pinnedOption when filter is cleared', fakeAsync(() => {
            const pinned = mockSites[0];

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: mockSites
            });

            // Apply filter first
            spectator.component.onFilterChange('demo');
            tick(300);
            spectator.detectChanges();

            // Clear filter
            spectator.component.onFilterChange('');
            tick(300);
            spectator.detectChanges();

            const select = spectator.query(Select);
            // Pinned should appear at top when filter is cleared
            expect(select.options[0]).toEqual(pinned);
        }));

        it('should handle case-insensitive filter matching for pinnedOption', fakeAsync(() => {
            const pinned: Site = {
                hostname: 'EXAMPLE.COM',
                type: 'host',
                identifier: 'site99',
                archived: false
            } as Site;

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                sites: mockSites
            });

            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const select = spectator.query(Select);
            // Pinned should match case-insensitively
            expect(select.options[0]).toEqual(pinned);
        }));
    });

    describe('Edge Cases', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should handle empty sites array', () => {
            // Create new component instance with empty response
            spectator = createComponent({ detectChanges: false });
            siteService = spectator.inject(DotSiteService, true);
            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );

            spectator.detectChanges();

            expect(spectator.component.$state.sites().length).toBe(0);
            // TODO: The component uses HARDCODED_TOTAL_ENTRIES (501) because the /api/v1/site endpoint
            // currently returns incorrect totalEntries in the pagination response (returns page size instead of actual total).
            // This is a temporary workaround until the backend is fixed to return the correct totalEntries value.
            expect(spectator.component.$state.totalRecords()).toBe(501);
        });

        it('should reset loading state on service error', () => {
            siteService.getSitesWithPagination.mockReturnValue(
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
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should not add selected value to list when filtering', fakeAsync(() => {
            const filteredSites: Site[] = [
                { hostname: 'example.com', type: 'host', identifier: 'site1', archived: false } as Site
            ];

            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: filteredSites,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();

            // Set a value that doesn't match the filter
            // Use writeValue to properly set both value and pinnedOption
            spectator.component.writeValue(mockSites[1]);
            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const sites = spectator.component.$state.sites();
            // Should not have the selected value if it doesn't match filter
            expect(sites.find((s) => s.identifier === 'site2')).toBeFalsy();
            // Verify pinnedOption is still set even though it doesn't match filter
            expect(spectator.component.$state.pinnedOption()).toEqual(mockSites[1]);
            // Verify pinnedOption doesn't appear in options when it doesn't match filter
            const options = spectator.component.$options();
            expect(options.find((s) => s.identifier === 'site2')).toBeFalsy();
        }));

        it('should handle lazy load event with undefined last', () => {
            const event: Partial<SelectLazyLoadEvent> = { first: 0 };

            spectator.component.onLazyLoad(event as SelectLazyLoadEvent);
            spectator.detectChanges();

            // Should use pageSize when last is undefined
            expect(siteService.getSitesWithPagination).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        });

        it('should handle writeValue when sites list is empty', () => {
            // Create a new component instance with empty sites
            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );

            spectator = createComponent({ detectChanges: false });
            siteService = spectator.inject(DotSiteService, true);
            siteService.getSitesWithPagination.mockReturnValue(
                of({
                    sites: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );
            spectator.detectChanges();

            const newSite: Site = {
                hostname: 'Custom.com',
                type: 'host',
                identifier: 'site99',
                archived: false
            } as Site;

            spectator.component.writeValue(newSite);
            spectator.detectChanges();

            // Verify pinnedOption is set
            expect(spectator.component.$state.pinnedOption()).toEqual(newSite);
            // Verify the value appears in $options (which combines pinnedOption with sites)
            const options = spectator.component.$options();
            expect(options.find((s) => s.identifier === 'site99')).toBeTruthy();
        });

        it('should ensure site is in list when writeValue is called', () => {
            const newSite: Site = {
                hostname: 'Custom.com',
                type: 'host',
                identifier: 'site99',
                archived: false
            } as Site;

            spectator.component.writeValue(newSite);
            spectator.detectChanges();

            // Verify the site is added to the sites list
            const sites = spectator.component.$state.sites();
            expect(sites.find((s) => s.identifier === 'site99')).toBeTruthy();
        });
    });
});

describe('DotSiteComponent - ControlValueAccessor Integration', () => {
    const createHost = createHostFactory({
        component: DotSiteComponent,
        host: FormHostComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotSiteService),
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        detectChanges: false
    });

    let hostSpectator: SpectatorHost<DotSiteComponent, FormHostComponent>;
    let hostComponent: FormHostComponent;
    let hostSiteService: SpyObject<DotSiteService>;

    beforeEach(() => {
        hostSpectator = createHost(
            `<dot-site [formControl]="siteControl" [disabled]="disabled"></dot-site>`
        );
        hostComponent = hostSpectator.hostComponent;
        hostSiteService = hostSpectator.inject(DotSiteService, true);

        hostSiteService.getSitesWithPagination.mockReturnValue(
            of({
                sites: mockSites,
                pagination: mockPagination
            })
        );

        hostSpectator.detectChanges();
    });

    it('should write value to component from FormControl', () => {
        const testValue = mockSites[0];
        hostComponent.siteControl.setValue(testValue);
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toEqual(testValue);
        // Verify pinnedOption is set when FormControl sets value
        expect(hostSpectator.component.$state.pinnedOption()).toEqual(testValue);
    });

    it('should set pinnedOption when writeValue is called', () => {
        const testValue = mockSites[0];
        hostSpectator.component.writeValue(testValue);

        expect(hostSpectator.component.$state.pinnedOption()).toEqual(testValue);
    });

    it('should set pinnedOption to null when writeValue is called with null', () => {
        hostSpectator.component.writeValue(null);

        expect(hostSpectator.component.$state.pinnedOption()).toBeNull();
    });

    it('should show pinnedOption at the top of $options when writeValue is called', () => {
        const testValue = mockSites[0];
        const loadedSites = [mockSites[1], mockSites[2]];

        patchState(hostSpectator.component.$state, { sites: loadedSites });
        hostComponent.siteControl.setValue(testValue);
        hostSpectator.detectChanges();

        const options = hostSpectator.component.$options();
        expect(options[0]).toEqual(testValue);
        expect(options.length).toBe(3);
    });

    it('should handle null value from FormControl', () => {
        hostComponent.siteControl.setValue(null);
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toBeNull();
        // Verify pinnedOption is set to null
        expect(hostSpectator.component.$state.pinnedOption()).toBeNull();
    });

    it('should ensure value is in sites list when FormControl sets value', () => {
        const newSite: Site = {
            hostname: 'Custom.com',
            type: 'host',
            identifier: 'site99',
            archived: false
        } as Site;

        hostComponent.siteControl.setValue(newSite);
        hostSpectator.detectChanges();

        // Verify pinnedOption is set
        expect(hostSpectator.component.$state.pinnedOption()).toEqual(newSite);
        // Verify the value appears in $options (which combines pinnedOption with sites)
        const options = hostSpectator.component.$options();
        expect(options.find((s) => s.identifier === 'site99')).toBeTruthy();
    });

    it('should propagate value changes from component to FormControl', () => {
        const testValue = mockSites[0];

        // Trigger onChange event from p-select component
        hostSpectator.triggerEventHandler(Select, 'onChange', { value: testValue });
        hostSpectator.detectChanges();

        expect(hostComponent.siteControl.value).toEqual(testValue);
    });

    it('should mark FormControl as touched when user interacts', () => {
        const testValue = mockSites[0];

        expect(hostComponent.siteControl.touched).toBe(false);

        // Trigger onChange event from p-select component (user interaction)
        hostSpectator.triggerEventHandler(Select, 'onChange', { value: testValue });
        hostSpectator.detectChanges();

        expect(hostComponent.siteControl.touched).toBe(true);
    });

    it('should set disabled state via FormControl', () => {
        hostComponent.siteControl.disable();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.$isDisabled()).toBe(true);
        expect(hostSpectator.component.$disabled()).toBe(true);

        const select = hostSpectator.query(Select);
        expect(select.disabled()).toBe(true);
    });

    it('should combine input disabled and FormControl disabled state', () => {
        // Test with both input disabled=false and FormControl enabled
        hostComponent.disabled = false;
        hostComponent.siteControl.enable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(false);

        // Test with input disabled=true (should override FormControl enabled)
        hostComponent.disabled = true;
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(true);

        // Test with input disabled=false but FormControl disabled (should be disabled)
        hostComponent.disabled = false;
        hostComponent.siteControl.disable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(true);
    });
});
