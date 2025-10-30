import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotFavoriteSelectorComponent } from './dot-favorite-selector.component';

import { DotPageContentTypeService } from '../../service/dot-page-contenttype.service';
import { DotPageFavoriteContentTypeService } from '../../service/dot-page-favorite-contentType.service';
import { DotPaletteListStore } from '../dot-uve-palette-list/store/store';

const MOCK_CONTENT_TYPES: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'blog',
        baseType: 'CONTENT',
        icon: 'article'
    } as DotCMSContentType,
    {
        id: '2',
        name: 'News',
        variable: 'news',
        baseType: 'CONTENT',
        icon: 'newspaper'
    } as DotCMSContentType
];

const MOCK_FAVORITE_CONTENT_TYPES: DotCMSContentType[] = [MOCK_CONTENT_TYPES[0]];

describe('DotFavoriteSelectorComponent', () => {
    let spectator: Spectator<DotFavoriteSelectorComponent>;
    let mockPageContentTypeService: jest.Mocked<DotPageContentTypeService>;
    let mockFavoriteContentTypeService: jest.Mocked<DotPageFavoriteContentTypeService>;
    let mockStore: jest.Mocked<InstanceType<typeof DotPaletteListStore>>;

    const createComponent = createComponentFactory({
        component: DotFavoriteSelectorComponent,
        imports: [DotFavoriteSelectorComponent],
        providers: [provideHttpClient(), provideHttpClientTesting(), DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: DotPageContentTypeService,
                    useValue: {
                        getAllContentTypes: jest.fn().mockReturnValue(
                            of({
                                contenttypes: MOCK_CONTENT_TYPES,
                                pagination: {
                                    currentPage: 1,
                                    perPage: 30,
                                    totalEntries: 2
                                }
                            })
                        )
                    }
                },
                {
                    provide: DotPageFavoriteContentTypeService,
                    useValue: {
                        getAll: jest.fn().mockReturnValue(MOCK_FAVORITE_CONTENT_TYPES),
                        set: jest.fn().mockReturnValue(MOCK_FAVORITE_CONTENT_TYPES)
                    }
                },
                {
                    provide: DotPaletteListStore,
                    useValue: {
                        contenttypes: signal(MOCK_CONTENT_TYPES),
                        setContentTypesFromFavorite: jest.fn()
                    }
                }
            ]
        });
        mockPageContentTypeService = spectator.inject(DotPageContentTypeService);
        mockFavoriteContentTypeService = spectator.inject(DotPageFavoriteContentTypeService);
        mockStore = spectator.inject(DotPaletteListStore);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Filtering', () => {
        it('should call getAllContentTypes when listbox emits onFilter event', fakeAsync(() => {
            // Clear any calls from ngOnInit
            mockPageContentTypeService.getAllContentTypes.mockClear();

            // Get the Listbox instance from the component's viewChild
            const listboxInstance = spectator.component.$listbox();

            // Trigger the onFilter event with a filter value
            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Blog'
            });

            // Wait for debounce (300ms)
            tick(300);

            // Verify the service was called with correct parameters
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith({
                filter: 'Blog',
                orderby: 'name',
                direction: 'ASC',
                types: ['CONTENT', 'FILEASSET', 'DOTASSET', 'WIDGET']
            });
        }));

        it('should call getAllContentTypes with empty string filter', fakeAsync(() => {
            mockPageContentTypeService.getAllContentTypes.mockClear();

            const listboxInstance = spectator.component.$listbox();

            // Trigger with empty filter
            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: ''
            });

            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith({
                filter: '',
                orderby: 'name',
                direction: 'ASC',
                types: ['CONTENT', 'FILEASSET', 'DOTASSET', 'WIDGET']
            });
        }));

        it('should debounce multiple filter events', fakeAsync(() => {
            mockPageContentTypeService.getAllContentTypes.mockClear();

            const listboxInstance = spectator.component.$listbox();

            // Trigger multiple filter events rapidly
            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'B'
            });
            tick(100);

            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Bl'
            });
            tick(100);

            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Blog'
            });

            // Should not have been called yet
            expect(mockPageContentTypeService.getAllContentTypes).not.toHaveBeenCalled();

            // Wait for debounce
            tick(300);

            // Should only be called once with the last filter value
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledTimes(1);
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'Blog' })
            );
        }));

        it('should set fetching state to true when filter is triggered', () => {
            const listboxInstance = spectator.component.$listbox();

            spectator.component.$fetchingItems.set(false);

            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'News'
            });

            expect(spectator.component.$fetchingItems()).toBe(true);
        });
    });

    describe('Selection', () => {
        it('should call setContentTypesFromFavorite when listbox emits onChange event', () => {
            const listboxInstance = spectator.component.$listbox();

            const selectedTypes = [MOCK_CONTENT_TYPES[0], MOCK_CONTENT_TYPES[1]];

            // Trigger the onChange event
            listboxInstance.onChange.emit({
                originalEvent: new Event('change'),
                value: selectedTypes
            });

            // Verify the favorite service was called to set the favorites
            expect(mockFavoriteContentTypeService.set).toHaveBeenCalledWith(selectedTypes);

            // Verify the store was updated
            expect(mockStore.setContentTypesFromFavorite).toHaveBeenCalledWith(
                MOCK_FAVORITE_CONTENT_TYPES
            );
        });

        it('should handle empty selection', () => {
            mockFavoriteContentTypeService.set.mockReturnValue([]);

            const listboxInstance = spectator.component.$listbox();

            listboxInstance.onChange.emit({
                originalEvent: new Event('change'),
                value: []
            });

            expect(mockFavoriteContentTypeService.set).toHaveBeenCalledWith([]);
            expect(mockStore.setContentTypesFromFavorite).toHaveBeenCalledWith([]);
        });

        it('should handle single item selection', () => {
            const singleSelection = [MOCK_CONTENT_TYPES[0]];
            mockFavoriteContentTypeService.set.mockReturnValue(singleSelection);

            const listboxInstance = spectator.component.$listbox();

            listboxInstance.onChange.emit({
                originalEvent: new Event('change'),
                value: singleSelection
            });

            expect(mockFavoriteContentTypeService.set).toHaveBeenCalledWith(singleSelection);
            expect(mockStore.setContentTypesFromFavorite).toHaveBeenCalledWith(singleSelection);
        });
    });

    describe('OverlayPanel toggle', () => {
        it('should expose toggle method that calls overlayPanel.toggle', () => {
            const overlayPanelInstance = spectator.component.$overlayPanel();

            // Spy on the toggle method
            const toggleSpy = jest.spyOn(overlayPanelInstance, 'toggle');

            // Create a mock event
            const mockEvent = new MouseEvent('click', {
                bubbles: true,
                cancelable: true
            });

            // Call the component's toggle method
            spectator.component.toggle(mockEvent);

            // Verify overlayPanel.toggle was called with the event
            expect(toggleSpy).toHaveBeenCalledWith(mockEvent);
            expect(toggleSpy).toHaveBeenCalledTimes(1);
        });

        it('should be accessible from component instance', () => {
            // Verify the toggle method exists and is a function
            expect(spectator.component.toggle).toBeDefined();
            expect(typeof spectator.component.toggle).toBe('function');
        });

        it('should call toggle with different event types', () => {
            const overlayPanelInstance = spectator.component.$overlayPanel();
            const toggleSpy = jest.spyOn(overlayPanelInstance, 'toggle');

            // Test with click event
            const clickEvent = new MouseEvent('click');
            spectator.component.toggle(clickEvent);
            expect(toggleSpy).toHaveBeenCalledWith(clickEvent);

            // Test with custom event
            const customEvent = new Event('custom');
            spectator.component.toggle(customEvent);
            expect(toggleSpy).toHaveBeenCalledWith(customEvent);

            expect(toggleSpy).toHaveBeenCalledTimes(2);
        });
    });

    describe('OverlayPanel onHide', () => {
        it('should call getAllContentTypes with empty filter when overlay hides', fakeAsync(() => {
            mockPageContentTypeService.getAllContentTypes.mockClear();

            const overlayPanelInstance = spectator.component.$overlayPanel();

            // Trigger the onHide event
            overlayPanelInstance.onHide.emit();

            // Wait for debounce
            tick(300);

            // Verify getAllContentTypes was called with empty filter
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith({
                filter: '',
                orderby: 'name',
                direction: 'ASC',
                types: ['CONTENT', 'FILEASSET', 'DOTASSET', 'WIDGET']
            });
        }));

        it('should reset listbox filter when overlay hides', () => {
            const listboxInstance = spectator.component.$listbox();
            const resetFilterSpy = jest.spyOn(listboxInstance, 'resetFilter');

            const overlayPanelInstance = spectator.component.$overlayPanel();

            overlayPanelInstance.onHide.emit();

            expect(resetFilterSpy).toHaveBeenCalled();
        });

        it('should reset filter after user has filtered content types', fakeAsync(() => {
            const listboxInstance = spectator.component.$listbox();

            // User filters content types
            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Blog'
            });
            tick(300);

            mockPageContentTypeService.getAllContentTypes.mockClear();

            // User closes overlay
            const overlayPanelInstance = spectator.component.$overlayPanel();
            overlayPanelInstance.onHide.emit();
            tick(300);

            // Should reset to empty filter
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: '' })
            );
        }));
    });

    describe('User workflow', () => {
        it('should handle complete workflow: filter -> select -> close', fakeAsync(() => {
            const listboxInstance = spectator.component.$listbox();
            const overlayPanelInstance = spectator.component.$overlayPanel();

            mockPageContentTypeService.getAllContentTypes.mockClear();

            // Step 1: User filters
            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Blog'
            });
            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'Blog' })
            );

            // Step 2: User selects items
            mockFavoriteContentTypeService.set.mockClear();
            mockStore.setContentTypesFromFavorite.mockClear();

            listboxInstance.onChange.emit({
                originalEvent: new Event('change'),
                value: [MOCK_CONTENT_TYPES[0]]
            });

            expect(mockFavoriteContentTypeService.set).toHaveBeenCalled();
            expect(mockStore.setContentTypesFromFavorite).toHaveBeenCalled();

            // Step 3: User closes overlay
            mockPageContentTypeService.getAllContentTypes.mockClear();

            overlayPanelInstance.onHide.emit();
            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: '' })
            );
        }));

        it('should handle toggle -> filter -> close workflow', fakeAsync(() => {
            const overlayPanelInstance = spectator.component.$overlayPanel();
            const toggleSpy = jest.spyOn(overlayPanelInstance, 'toggle');

            // Step 1: Open overlay
            const clickEvent = new MouseEvent('click');
            spectator.component.toggle(clickEvent);
            expect(toggleSpy).toHaveBeenCalledWith(clickEvent);

            // Step 2: Filter
            mockPageContentTypeService.getAllContentTypes.mockClear();
            const listboxInstance = spectator.component.$listbox();

            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'News'
            });
            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'News' })
            );

            // Step 3: Close overlay
            mockPageContentTypeService.getAllContentTypes.mockClear();
            overlayPanelInstance.onHide.emit();
            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: '' })
            );
        }));
    });

    describe('Edge Cases', () => {
        it('should handle filter with special characters', fakeAsync(() => {
            mockPageContentTypeService.getAllContentTypes.mockClear();

            const listboxInstance = spectator.component.$listbox();

            listboxInstance.onFilter.emit({
                originalEvent: new Event('filter'),
                filter: 'Test@#$%'
            });
            tick(300);

            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'Test@#$%' })
            );
        }));

        it('should handle onChange with undefined value', () => {
            mockFavoriteContentTypeService.set.mockReturnValue([]);

            const listboxInstance = spectator.component.$listbox();

            expect(() => {
                listboxInstance.onChange.emit({
                    originalEvent: new Event('change'),
                    value: undefined
                });
            }).not.toThrow();
        });

        it('should handle rapid open/close of overlay', fakeAsync(() => {
            const overlayPanelInstance = spectator.component.$overlayPanel();

            mockPageContentTypeService.getAllContentTypes.mockClear();

            // Trigger hide multiple times
            overlayPanelInstance.onHide.emit();
            overlayPanelInstance.onHide.emit();
            overlayPanelInstance.onHide.emit();

            tick(300);

            // Should still work correctly
            expect(mockPageContentTypeService.getAllContentTypes).toHaveBeenCalled();
        }));
    });
});
