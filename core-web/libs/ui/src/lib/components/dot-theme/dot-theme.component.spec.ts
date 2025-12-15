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
import { fakeAsync, tick, flush } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { Select, SelectLazyLoadEvent } from 'primeng/select';

import { DotThemesService } from '@dotcms/data-access';
import { DotPagination, DotTheme } from '@dotcms/dotcms-models';

import { DotThemeComponent } from './dot-theme.component';

const mockThemes: DotTheme[] = [
    {
        identifier: 'theme1',
        inode: 'theme1',
        path: '/application/themes/theme1/',
        title: 'Example Theme',
        themeThumbnail: null,
        name: 'example-theme',
        hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
    },
    {
        identifier: 'theme2',
        inode: 'theme2',
        path: '/application/themes/theme2/',
        title: 'Demo Theme',
        themeThumbnail: null,
        name: 'demo-theme',
        hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
    },
    {
        identifier: 'theme3',
        inode: 'theme3',
        path: '/application/themes/theme3/',
        title: 'Test Theme',
        themeThumbnail: null,
        name: 'test-theme',
        hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
    }
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
    themeControl = new FormControl<string | null>(null);
}

describe('DotThemeComponent', () => {
    let spectator: Spectator<DotThemeComponent>;
    let themeService: SpyObject<DotThemesService>;

    const createComponent = createComponentFactory({
        component: DotThemeComponent,
        imports: [ReactiveFormsModule],
        providers: [
            mockProvider(DotThemesService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        themeService = spectator.inject(DotThemesService, true);
        themeService.getThemes.mockReturnValue(
            of({
                themes: mockThemes,
                pagination: mockPagination
            })
        );
        themeService.get.mockImplementation((inode: string) =>
            of(mockThemes.find((t) => t.inode === inode) || mockThemes[0])
        );
    });

    describe('Component Initialization', () => {
        it('should initialize p-select with correct configuration', () => {
            spectator.component.value.set('theme1');
            spectator.detectChanges();

            const select = spectator.query(Select);

            expect(spectator.component.$options()).toEqual(expect.arrayContaining(mockThemes));
            expect(select.loading).toBe(false);
            expect(select.virtualScroll).toBe(true);
            expect(select.lazy).toBe(true);
            expect(select.filter).toBe(true);
        });

        it('should update disabled state via ControlValueAccessor', () => {
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.disabled()).toBe(false);

            spectator.component.setDisabledState(true);
            spectator.detectChanges();
            expect(select.disabled()).toBe(true);

            spectator.component.setDisabledState(false);
            spectator.detectChanges();
            expect(select.disabled()).toBe(false);
        });

        it('should sort themes alphabetically by title', () => {
            const unsortedThemes: DotTheme[] = [
                {
                    identifier: 'theme1',
                    inode: 'theme1',
                    path: '/application/themes/theme1/',
                    title: 'Zebra Theme',
                    themeThumbnail: null,
                    name: 'zebra-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                },
                {
                    identifier: 'theme2',
                    inode: 'theme2',
                    path: '/application/themes/theme2/',
                    title: 'Alpha Theme',
                    themeThumbnail: null,
                    name: 'alpha-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                },
                {
                    identifier: 'theme3',
                    inode: 'theme3',
                    path: '/application/themes/theme3/',
                    title: 'Beta Theme',
                    themeThumbnail: null,
                    name: 'beta-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                }
            ];

            themeService.getThemes.mockReturnValue(
                of({
                    themes: unsortedThemes,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();

            const sortedThemes = spectator.component.$state.themes();
            expect(sortedThemes[0].title).toBe('Alpha Theme');
            expect(sortedThemes[1].title).toBe('Beta Theme');
            expect(sortedThemes[2].title).toBe('Zebra Theme');
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

            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();

            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 2,
                per_page: 40
            });
        });

        it('should merge new themes without duplicates', () => {
            // Page 2 includes a duplicate (theme1) and new items (theme4, theme5)
            const page2Themes: DotTheme[] = [
                {
                    identifier: 'theme1',
                    inode: 'theme1',
                    path: '/application/themes/theme1/',
                    title: 'Example Theme',
                    themeThumbnail: null,
                    name: 'example-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                },
                {
                    identifier: 'theme4',
                    inode: 'theme4',
                    path: '/application/themes/theme4/',
                    title: 'Event Theme',
                    themeThumbnail: null,
                    name: 'event-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                },
                {
                    identifier: 'theme5',
                    inode: 'theme5',
                    path: '/application/themes/theme5/',
                    title: 'Product Theme',
                    themeThumbnail: null,
                    name: 'product-theme',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                }
            ];

            // Reset and setup for page 2
            spectator = createComponent({ detectChanges: false });
            themeService = spectator.inject(DotThemesService, true);
            themeService.getThemes
                .mockReturnValueOnce(
                    of({
                        themes: mockThemes,
                        pagination: { ...mockPagination, currentPage: 1 }
                    })
                )
                .mockReturnValueOnce(
                    of({
                        themes: page2Themes,
                        pagination: { ...mockPagination, currentPage: 2 }
                    })
                );
            themeService.get.mockImplementation((inode: string) =>
                of(mockThemes.find((t) => t.inode === inode) || mockThemes[0])
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();

            const allThemes = spectator.component.$state.themes();
            // Should have 5 items: theme1, theme2, theme3 (from page 1) + theme4, theme5 (from page 2)
            // theme1 should NOT be duplicated
            expect(allThemes.length).toBe(5);
            expect(allThemes.filter((t) => t.identifier === 'theme1').length).toBe(1);
            expect(allThemes.find((t) => t.identifier === 'theme4')).toBeTruthy();
            expect(allThemes.find((t) => t.identifier === 'theme5')).toBeTruthy();
        });

        it('should not load duplicate pages', () => {
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Try to load page 2 again
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();

            // Should not make another call
            expect(themeService.getThemes).not.toHaveBeenCalled();
        });

        it('should handle invalid lazy load events with NaN values', () => {
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: NaN, last: NaN } as SelectLazyLoadEvent);

            expect(themeService.getThemes).not.toHaveBeenCalled();
        });

        it('should not load if currently loading', () => {
            themeService.getThemes.mockReturnValue(
                of({
                    themes: mockThemes,
                    pagination: mockPagination
                })
            );

            spectator.detectChanges();
            jest.clearAllMocks();

            // Manually set loading state to true
            patchState(spectator.component.$state, { loading: true });

            // Try to load while still loading
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();

            // Should not make another call while loading
            expect(themeService.getThemes).not.toHaveBeenCalled();

            // Cleanup
            patchState(spectator.component.$state, { loading: false });
        });

        it('should not load beyond total entries', () => {
            // Create new component instance with limited total entries
            spectator = createComponent({ detectChanges: false });
            themeService = spectator.inject(DotThemesService, true);

            // First load pages 1 and 2 to set up the state
            themeService.getThemes
                .mockReturnValueOnce(
                    of({
                        themes: mockThemes,
                        pagination: { ...mockPagination, currentPage: 1, totalEntries: 50 }
                    })
                )
                .mockReturnValueOnce(
                    of({
                        themes: mockThemes,
                        pagination: { ...mockPagination, currentPage: 2, totalEntries: 50 }
                    })
                );
            themeService.get.mockImplementation((inode: string) =>
                of(mockThemes.find((t) => t.inode === inode) || mockThemes[0])
            );

            spectator.detectChanges();

            // Load page 2 to mark it as loaded
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();

            // Manually set totalRecords to 50 for this test
            patchState(spectator.component.$state, { totalRecords: 50 });

            jest.clearAllMocks();

            // Now try to load page 3, which would be beyond total of 50
            // The component should check and not load page 3
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 80, last: 119 });
            spectator.detectChanges();

            // Page 3 would be beyond total of 50, so should not load
            expect(themeService.getThemes).not.toHaveBeenCalled();
        });
    });

    describe('Filtering Functionality', () => {
        beforeEach(() => {
            spectator.detectChanges();
            jest.clearAllMocks();
        });

        it('should debounce filter changes', fakeAsync(() => {
            spectator.detectChanges();

            // Get the Select component and open the dropdown
            const select = spectator.query(Select);
            select.show();
            spectator.detectChanges();
            tick(); // Allow overlay to render

            // Query document.body since overlay is appended there
            const input = document.body.querySelector<HTMLInputElement>('input[type="text"][role="searchbox"]');
            expect(input).toBeTruthy();

            // Set the value and trigger the actual DOM input event
            input.value = 'example';
            spectator.dispatchFakeEvent(input, 'input');
            spectator.detectChanges();

            // Should not call immediately
            expect(themeService.getThemes).not.toHaveBeenCalled();

            // Flush animation microtasks before ticking debounce timer
            // This handles PrimeNG Motion promises that may be pending
            try {
                flush();
            } catch {
                // Ignore animation errors - they don't affect the debounce test
            }

            // After 300ms, should call
            tick(300);
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                searchParam: 'example'
            });
        }));

        it('should reset loaded pages when filtering', fakeAsync(() => {
            // Load page 2 first to mark it as loaded
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();
            jest.clearAllMocks();

            // Apply filter - should reset loaded pages and clear themes
            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            // Should reset and load page 1 with filter
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                searchParam: 'example'
            });

            // Verify that themes were cleared before filter load
            jest.clearAllMocks();
            themeService.getThemes.mockReturnValue(
                of({
                    themes: [mockThemes[0]],
                    pagination: { ...mockPagination, totalEntries: 50 }
                })
            );
            spectator.triggerEventHandler(Select, 'onLazyLoad', { first: 40, last: 79 });
            spectator.detectChanges();
            // Should make another call for page 2 since pages were reset by filter
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 2,
                per_page: 40,
                searchParam: 'example'
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

            // Get the Select component and open the dropdown
            const select = spectator.query(Select);
            select.show();
            spectator.detectChanges();
            tick(); // Allow overlay to render

            // Query document.body since overlay is appended there
            const input = document.body.querySelector<HTMLInputElement>('input[type="text"][role="searchbox"]');

            expect(input.value).toBe('');
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should cancel previous debounce timeout on new filter', fakeAsync(() => {
            spectator.component.onFilterChange('example');
            spectator.component.onFilterChange('demo');
            tick(300);

            // Should only call once with 'demo'
            expect(themeService.getThemes).toHaveBeenCalledTimes(1);
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                searchParam: 'demo'
            });
        }));

        it('should trim filter value before passing to service', fakeAsync(() => {
            spectator.component.onFilterChange('  example  ');
            tick(300);

            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40,
                searchParam: 'example'
            });
        }));
    });

    describe('Component Inputs', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should update value model signal', () => {
            const testValue = 'theme1';
            spectator.component.value.set(testValue);
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(testValue);
        });

        it('should bind placeholder input to p-select', () => {
            spectator.setInput('placeholder', 'Custom placeholder');
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.placeholder()).toBe('Custom placeholder');
        });

        it('should bind disabled input to p-select', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.disabled()).toBe(true);

            spectator.setInput('disabled', false);
            spectator.detectChanges();
            expect(select.disabled()).toBe(false);
        });

        it('should bind class input to p-select', () => {
            spectator.setInput('class', 'custom-class');
            spectator.detectChanges();

            const selectElement = spectator.query('p-select');
            expect(selectElement).toHaveClass('custom-class');
        });

        it('should bind id input to p-select inputId', () => {
            spectator.setInput('id', 'custom-id');
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.inputId).toBe('custom-id');
        });

        it('should use default values when inputs are not provided', () => {
            spectator.detectChanges();

            expect(spectator.component.placeholder()).toBe('');
            expect(spectator.component.class()).toBe('w-full');
            expect(spectator.component.id()).toBe('');
        });
    });

    describe('Component Outputs', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should emit onChange output when value changes', () => {
            spectator.detectChanges();
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            const selectedTheme = mockThemes[0];
            // Call onThemeChange directly (bound from template: (onChange)="onThemeChange($event.value)")
            spectator.triggerEventHandler(Select, 'onChange', { value: selectedTheme });
            spectator.detectChanges();

            expect(spectator.component.value()).toEqual(selectedTheme.identifier);
            expect(onChangeSpy).toHaveBeenCalledWith(selectedTheme.identifier);
        });

        it('should emit null when value is cleared', () => {
            const onChangeSpy = jest.spyOn(spectator.component.onChange, 'emit');

            spectator.triggerEventHandler(Select, 'onChange', { value: null });

            expect(spectator.component.value()).toBeNull();
            expect(onChangeSpy).toHaveBeenCalledWith(null);
        });

        it('should emit onShow output when select overlay is shown', () => {
            const onShowSpy = jest.spyOn(spectator.component.onShow, 'emit');

            spectator.triggerEventHandler(Select, 'onShow', {});

            expect(onShowSpy).toHaveBeenCalled();
        });

        it('should emit onHide output when select overlay is hidden', () => {
            const onHideSpy = jest.spyOn(spectator.component.onHide, 'emit');

            spectator.triggerEventHandler(Select, 'onHide', {});

            expect(onHideSpy).toHaveBeenCalled();
        });
    });

    describe('Pinned Option Functionality', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show pinnedOption at the top of $options when writeValue is called', fakeAsync(() => {
            const testValue = mockThemes[1].identifier;
            const loadedThemes = [mockThemes[0], mockThemes[2]];

            patchState(spectator.component.$state, { themes: loadedThemes });
            spectator.component.writeValue(testValue);
            spectator.detectChanges();
            tick();
            spectator.detectChanges();

            const options = spectator.component.$options();
            expect(options[0]).toEqual(mockThemes[1]);
            expect(options.length).toBe(3);

            // Verify $options() binding to p-select
            const select = spectator.query(Select);
            expect(select.options).toEqual(options);
            expect(select.options[0]).toEqual(mockThemes[1]);
        }));

        it('should update pinnedOption when p-select onChange is triggered', () => {
            const testValue = mockThemes[1];
            spectator.triggerEventHandler(Select, 'onChange', { value: testValue });
            spectator.detectChanges();

            expect(spectator.component.$state.pinnedOption()).toEqual(testValue);
            expect(spectator.component.value()).toEqual(testValue.identifier);
        });

        it('should set pinnedOption to null when p-select onChange is triggered with null', () => {
            const testValue = mockThemes[0];

            spectator.triggerEventHandler(Select, 'onChange', { value: testValue });
            spectator.detectChanges();
            expect(spectator.component.$state.pinnedOption()).toEqual(testValue);

            spectator.triggerEventHandler(Select, 'onChange', { value: null });
            spectator.detectChanges();
            expect(spectator.component.$state.pinnedOption()).toBeNull();
            expect(spectator.component.value()).toBeNull();
        });

        it('should return only loaded options when pinnedOption is null', () => {
            const loadedThemes = mockThemes.slice(0, 2);
            patchState(spectator.component.$state, { themes: loadedThemes, pinnedOption: null });

            const options = spectator.component.$options();
            expect(options).toEqual(loadedThemes);
            expect(options.length).toBe(2);
        });

        it('should show pinnedOption at the top of $options', () => {
            const pinned = mockThemes[0];
            const loadedThemes = [mockThemes[1], mockThemes[2]];

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                themes: loadedThemes
            });

            spectator.detectChanges();

            const options = spectator.component.$options();
            expect(options[0]).toEqual(pinned);
        });

        it('should filter out pinnedOption from loaded options to avoid duplicates', () => {
            const pinned = mockThemes[0];
            // Loaded themes include the pinned option (duplicate)
            const loadedThemes = [mockThemes[0], mockThemes[1], mockThemes[2]];

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                themes: loadedThemes
            });

            spectator.detectChanges();

            const select = spectator.query(Select);
            // Should have pinned at top, then only theme2 and theme3 (theme1 filtered out)
            expect(select.options[0]).toEqual(pinned);
            expect(select.options.length).toBe(3);

            // Verify theme1 only appears once (as pinned)
            const theme1Count = select.options.filter((t) => t.identifier === 'theme1').length;
            expect(theme1Count).toBe(1);
        });

        it('should show pinnedOption when filtering if it matches the filter by title', fakeAsync(() => {
            const pinned: DotTheme = {
                identifier: 'theme99',
                inode: 'theme99',
                path: '/application/themes/theme99/',
                title: 'CustomExample Theme',
                themeThumbnail: null,
                name: 'custom-example-theme',
                hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
            };

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                themes: mockThemes
            });

            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const select = spectator.query(Select);
            // Pinned should appear at top if it matches filter
            expect(select.options[0]).toEqual(pinned);
        }));

        it('should not show pinnedOption when filtering if it does not match the filter', fakeAsync(() => {
            const pinned: DotTheme = {
                identifier: 'theme99',
                inode: 'theme99',
                path: '/application/themes/theme99/',
                title: 'Custom Theme',
                themeThumbnail: null,
                name: 'custom-theme',
                hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
            };

            patchState(spectator.component.$state, {
                pinnedOption: pinned,
                themes: mockThemes
            });

            spectator.component.onFilterChange('example');
            tick(300);
            spectator.detectChanges();

            const select = spectator.query(Select);
            // Pinned should not appear if it doesn't match filter
            expect(select.options.find((t) => t.identifier === 'theme99')).toBeFalsy();
            // Should only show filtered results
            expect(select.options.length).toBeGreaterThan(0);
        }));
    });

    describe('Edge Cases', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should handle empty themes array', () => {
            // Create new component instance with empty response
            spectator = createComponent({ detectChanges: false });
            themeService = spectator.inject(DotThemesService, true);
            themeService.getThemes.mockReturnValue(
                of({
                    themes: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );
            themeService.get.mockImplementation((inode: string) =>
                of(mockThemes.find((t) => t.inode === inode) || mockThemes[0])
            );

            spectator.detectChanges();

            expect(spectator.component.$state.themes().length).toBe(0);
            expect(spectator.component.$state.totalRecords()).toBe(0);
        });

        it('should reset loading state on service error', () => {
            themeService.getThemes.mockReturnValue(throwError(() => new Error('API Error')));

            spectator.detectChanges();

            expect(spectator.component.$state.loading()).toBe(false);
        });

        it('should handle filter with only whitespace', fakeAsync(() => {
            spectator.component.onFilterChange('   ');
            tick(300);

            // Should reset filter instead of calling with whitespace
            expect(spectator.component.$state.filterValue()).toBe('');
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        }));

        it('should handle lazy load event with undefined last', () => {
            const event: Partial<SelectLazyLoadEvent> = { first: 0 };

            spectator.component.onLazyLoad(event as SelectLazyLoadEvent);
            spectator.detectChanges();

            // Should use pageSize when last is undefined
            expect(themeService.getThemes).toHaveBeenCalledWith({
                page: 1,
                per_page: 40
            });
        });

        it('should handle writeValue when themes list is empty', fakeAsync(() => {
            // Create a new component instance with empty themes
            spectator = createComponent({ detectChanges: false });
            themeService = spectator.inject(DotThemesService, true);
            themeService.getThemes.mockReturnValue(
                of({
                    themes: [],
                    pagination: { ...mockPagination, totalEntries: 0 }
                })
            );

            const newTheme: DotTheme = {
                identifier: 'theme99',
                inode: 'theme99',
                path: '/application/themes/theme99/',
                title: 'Custom Theme',
                themeThumbnail: null,
                name: 'custom-theme',
                hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
            };
            themeService.get.mockReturnValue(of(newTheme));

            spectator.detectChanges();

            spectator.component.writeValue('theme99');
            spectator.detectChanges();
            tick();
            spectator.detectChanges();

            // Verify pinnedOption is set
            expect(spectator.component.$state.pinnedOption()).toEqual(newTheme);
            // Verify the value appears in $options (which combines pinnedOption with themes)
            const options = spectator.component.$options();
            expect(options.find((t) => t.identifier === 'theme99')).toBeTruthy();
        }));
    });
});

describe('DotThemeComponent - ControlValueAccessor Integration', () => {
    const createHost = createHostFactory({
        component: DotThemeComponent,
        host: FormHostComponent,
        imports: [ReactiveFormsModule],
        providers: [mockProvider(DotThemesService), provideHttpClient(), provideHttpClientTesting()],
        detectChanges: false
    });

    let hostSpectator: SpectatorHost<DotThemeComponent, FormHostComponent>;
    let hostComponent: FormHostComponent;
    let hostThemeService: SpyObject<DotThemesService>;

    beforeEach(() => {
        hostSpectator = createHost(
            `<dot-theme [formControl]="themeControl"></dot-theme>`
        );
        hostComponent = hostSpectator.hostComponent;
        hostThemeService = hostSpectator.inject(DotThemesService, true);

        hostThemeService.getThemes.mockReturnValue(
            of({
                themes: mockThemes,
                pagination: mockPagination
            })
        );
        hostThemeService.get.mockImplementation((inode: string) =>
            of(mockThemes.find((t) => t.inode === inode) || mockThemes[0])
        );

        hostSpectator.detectChanges();
    });

    it('should write value to component from FormControl', fakeAsync(() => {
        const testValue = mockThemes[0].identifier;
        hostComponent.themeControl.setValue(testValue);
        hostSpectator.detectChanges();
        tick();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.value()).toEqual(testValue);
        // Verify pinnedOption is set when FormControl sets value
        expect(hostSpectator.component.$state.pinnedOption()).toEqual(mockThemes[0]);
    }));

    it('should set pinnedOption when writeValue is called', fakeAsync(() => {
        const testValue = mockThemes[0].identifier;
        hostSpectator.component.writeValue(testValue);
        hostSpectator.detectChanges();
        tick();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.$state.pinnedOption()).toEqual(mockThemes[0]);
    }));

    it('should set pinnedOption to null when writeValue is called with null', fakeAsync(() => {
        hostSpectator.component.writeValue(null);
        tick();

        expect(hostSpectator.component.$state.pinnedOption()).toBeNull();
    }));

    it('should show pinnedOption at the top of $options when writeValue is called', fakeAsync(() => {
        const testValue = mockThemes[0].identifier;
        const loadedThemes = [mockThemes[1], mockThemes[2]];

        patchState(hostSpectator.component.$state, { themes: loadedThemes });
        hostComponent.themeControl.setValue(testValue);
        hostSpectator.detectChanges();
        tick();
        hostSpectator.detectChanges();

        const options = hostSpectator.component.$options();
        expect(options[0]).toEqual(mockThemes[0]);
        expect(options.length).toBe(3);
    }));

    it('should handle null value from FormControl', fakeAsync(() => {
        hostComponent.themeControl.setValue(null);
        hostSpectator.detectChanges();
        tick();

        expect(hostSpectator.component.value()).toBeNull();
        // Verify pinnedOption is set to null
        expect(hostSpectator.component.$state.pinnedOption()).toBeNull();
    }));

    it('should propagate value changes from component to FormControl', () => {
        const testValue = mockThemes[0];

        // Trigger onChange event from p-select component
        hostSpectator.component.onThemeChange(testValue);
        hostSpectator.detectChanges();

        expect(hostComponent.themeControl.value).toEqual(testValue.identifier);
    });

    it('should mark FormControl as touched when user interacts', () => {
        const testValue = mockThemes[0];

        expect(hostComponent.themeControl.touched).toBe(false);

        // Trigger onChange event from p-select component (user interaction)
        hostSpectator.component.onThemeChange(testValue);
        hostSpectator.detectChanges();

        expect(hostComponent.themeControl.touched).toBe(true);
    });

    it('should set disabled state via FormControl', () => {
        hostComponent.themeControl.disable();
        hostSpectator.detectChanges();

        expect(hostSpectator.component.$isDisabled()).toBe(true);
        expect(hostSpectator.component.$disabled()).toBe(true);

        const select = hostSpectator.query(Select);
        expect(select.disabled()).toBe(true);
    });

    it('should respect FormControl disabled state', () => {
        // Test with FormControl enabled
        hostComponent.themeControl.enable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(false);

        // Test with FormControl disabled (should be disabled)
        hostComponent.themeControl.disable();
        hostSpectator.detectChanges();
        expect(hostSpectator.component.$disabled()).toBe(true);
    });
});
