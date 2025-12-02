import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ContextMenu } from 'primeng/contextmenu';
import { Menu } from 'primeng/menu';

import {
    DotESContentService,
    DotFavoriteContentTypeService,
    DotLocalstorageService,
    DotMessageService,
    DotPageContentTypeService
} from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUvePaletteListComponent } from './dot-uve-palette-list.component';
import { DotPaletteListStore } from './store/store';

import { DotPaletteListStatus, DotUVEPaletteListTypes, DotUVEPaletteListView } from '../../models';
import { DotFavoriteSelectorComponent } from '../dot-favorite-selector/dot-favorite-selector.component';

const mockStore = {
    // signals/derived used by component/template
    contenttypes: signal([]),
    contentlets: signal([]),
    pagination: {
        perPage: signal(30),
        totalEntries: signal(0),
        currentPage: signal(1)
    },
    searchParams: {
        selectedContentType: signal(''),
        filter: signal(''),
        listType: signal(DotUVEPaletteListTypes.CONTENT),
        orderby: signal('name' as 'name' | 'usage'),
        direction: signal('ASC' as 'ASC' | 'DESC')
    },
    currentView: signal(DotUVEPaletteListView.CONTENT_TYPES),
    status: signal(DotPaletteListStatus.LOADING),
    layoutMode: signal('grid' as 'grid' | 'list'),
    $isLoading: signal(false),
    $isEmpty: signal(false),
    $showListLayout: signal(false),
    $currentSort: signal({
        orderby: 'name' as 'name' | 'usage',
        direction: 'ASC' as 'ASC' | 'DESC'
    }),
    $isContentletsView: signal(false),
    $isContentTypesView: signal(true),
    $isFavoritesList: signal(false),
    // methods we assert on
    getContentTypes: jest.fn(),
    getContentlets: jest.fn(),
    setLayoutMode: jest.fn(),
    addFavorite: jest.fn(),
    removeFavorite: jest.fn()
};

describe('DotUvePaletteListComponent', () => {
    let spectator: Spectator<DotUvePaletteListComponent>;
    let store: jest.Mocked<InstanceType<typeof DotPaletteListStore>>;

    const createComponent = createComponentFactory({
        component: DotUvePaletteListComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotPageContentTypeService,
                useValue: {
                    get: jest.fn().mockReturnValue(
                        of({
                            contenttypes: [],
                            pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
                        })
                    ),
                    getAllContentTypes: jest.fn().mockReturnValue(
                        of({
                            contenttypes: [],
                            pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
                        })
                    )
                }
            },
            {
                provide: DotFavoriteContentTypeService,
                useValue: {
                    getAll: jest.fn().mockReturnValue([]),
                    isFavorite: jest.fn().mockReturnValue(false),
                    add: jest.fn().mockReturnValue([]),
                    remove: jest.fn().mockReturnValue([]),
                    set: jest.fn().mockReturnValue([])
                }
            },
            DotLocalstorageService,
            {
                provide: DotESContentService,
                useValue: {
                    get: jest.fn().mockReturnValue(
                        of({
                            contentlets: [],
                            pagination: { currentPage: 1, perPage: 30, totalEntries: 0 }
                        })
                    )
                }
            },
            MessageService,
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            },
            {
                provide: DotMessageService,
                useValue: {
                    ...MockDotMessageService,
                    get: (key: string) => key
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent({
            providers: [mockProvider(DotPaletteListStore, mockStore)]
        });
        store = spectator.inject(DotPaletteListStore, true);
        // Use componentRef.setInput for required signal inputs with aliases
        spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
        spectator.fixture.componentRef.setInput('languageId', 1);
        spectator.fixture.componentRef.setInput('pagePath', '/test-page');
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllTimers();
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('search (content types): debounces and calls getContentTypes with filter and page 1', () => {
        // ensure we are in content types view
        mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);

        // Simulate user typing in the search input
        const searchInput = spectator.query(
            '[data-testid="palette-search-input"]'
        ) as HTMLInputElement;
        spectator.typeInElement('blog', searchInput);

        jest.advanceTimersByTime(300);

        expect(store.getContentTypes).toHaveBeenCalled();
        expect(store.getContentTypes).toHaveBeenCalledWith({
            filter: 'blog',
            page: 1
        });
    });

    it('search (contentlets): debounces and calls getContentlets with filter and page 1', () => {
        // switch to contentlets view by setting selectedContentType (which determines $isContentTypesView)
        mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
        mockStore.searchParams.selectedContentType.set('Blog'); // Non-empty = contentlets view
        mockStore.$isContentletsView.set(true);
        mockStore.$isContentTypesView.set(false);

        // Simulate user typing in the search input
        const searchInput = spectator.query(
            '[data-testid="palette-search-input"]'
        ) as HTMLInputElement;
        spectator.typeInElement('news', searchInput);

        jest.advanceTimersByTime(300);

        expect(store.getContentlets).toHaveBeenCalled();
        expect(store.getContentlets).toHaveBeenCalledWith({
            filter: 'news',
            page: 1
        });
    });

    describe('template', () => {
        it('should render dot-uve-palette-contenttype when in content types view', () => {
            // Set up content types view with mock data
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);
            mockStore.$isContentTypesView.set(true);
            mockStore.$isContentletsView.set(false);

            spectator.detectChanges();

            // Verify content type component is rendered
            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            const contentletComponent = spectator.query('dot-uve-palette-contentlet');

            expect(contentTypeComponent).toBeTruthy();
            expect(contentletComponent).toBeNull();
        });

        it('should render dot-uve-palette-contentlet when in contentlets view', () => {
            // Set up contentlets view with mock data
            mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.contentlets.set([
                {
                    identifier: '123',
                    title: 'My Blog Post',
                    contentType: 'Blog'
                } as DotCMSContentlet
            ]);
            mockStore.$isContentletsView.set(true);
            mockStore.$isContentTypesView.set(false);

            spectator.detectChanges();

            // Verify contentlet component is rendered
            const contentletComponent = spectator.query('dot-uve-palette-contentlet');
            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');

            expect(contentletComponent).toBeTruthy();
            expect(contentTypeComponent).toBeNull();
        });

        it('should switch from content types to contentlets view when onSelectContentType is emitted', () => {
            // Set up initial content types view
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);
            mockStore.$isContentTypesView.set(true);
            mockStore.$isContentletsView.set(false);

            // Set a search value initially
            spectator.component.searchControl.setValue('test search');
            spectator.detectChanges();

            // Verify initial state - content type component should be visible
            let contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            let contentletComponent = spectator.query('dot-uve-palette-contentlet');
            expect(contentTypeComponent).toBeTruthy();
            expect(contentletComponent).toBeNull();

            // Trigger the onSelectContentType event
            spectator.triggerEventHandler(
                'dot-uve-palette-contenttype',
                'onSelectContentType',
                'Blog'
            );

            // Update mock store to reflect the new state after selection
            mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
            mockStore.contentlets.set([
                {
                    identifier: '123',
                    title: 'My Blog Post',
                    contentType: 'Blog'
                } as DotCMSContentlet
            ]);
            mockStore.$isContentTypesView.set(false);
            mockStore.$isContentletsView.set(true);

            spectator.detectChanges();

            // Verify getContentlets was called with correct arguments
            expect(store.getContentlets).toHaveBeenCalledWith({
                selectedContentType: 'Blog',
                filter: '',
                page: 1
            });

            // Verify search input was cleared in the DOM
            const searchInput = spectator.query(
                '[data-testid="palette-search-input"]'
            ) as HTMLInputElement;
            expect(searchInput.value).toBe('');

            // Verify template switched to contentlets view
            contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            contentletComponent = spectator.query('dot-uve-palette-contentlet');
            expect(contentTypeComponent).toBeNull();
            expect(contentletComponent).toBeTruthy();
        });

        it('should switch back to content types view when back button is clicked', () => {
            // Setup: Start in contentlets view
            mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.contentlets.set([
                {
                    identifier: '123',
                    title: 'My Blog Post',
                    contentType: 'Blog'
                } as DotCMSContentlet
            ]);
            mockStore.$isContentTypesView.set(false);
            mockStore.$isContentletsView.set(true);

            spectator.detectChanges();

            // Verify initial state - contentlet view should be visible
            expect(spectator.query('dot-uve-palette-contentlet')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contenttype')).toBeNull();
            expect(spectator.query('[data-testid="back-to-content-types-button"]')).toBeTruthy();

            // Set a search value to verify it gets cleared
            const searchInput = spectator.query(
                '[data-testid="palette-search-input"]'
            ) as HTMLInputElement;
            spectator.typeInElement('blog search', searchInput);
            expect(searchInput.value).toBe('blog search');

            // Clear the mock to only check the call from the back button
            store.getContentTypes.mockClear();

            // Click the back button using triggerEventHandler for PrimeNG components
            spectator.triggerEventHandler(
                '[data-testid="back-to-content-types-button"]',
                'onClick',
                new Event('click')
            );

            // Update mock store to reflect new state
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);
            mockStore.$isContentTypesView.set(true);
            mockStore.$isContentletsView.set(false);

            spectator.detectChanges();

            // Verify getContentTypes was called with correct arguments
            expect(store.getContentTypes).toHaveBeenCalledWith({
                selectedContentType: '',
                filter: '',
                page: 1
            });

            // Verify search input was cleared in the DOM
            expect(searchInput.value).toBe('');

            // Verify template switched back to content types view
            expect(spectator.query('dot-uve-palette-contenttype')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contentlet')).toBeNull();
            expect(spectator.query('[data-testid="back-to-content-types-button"]')).toBeNull();
        });

        it('should call favoritesPanel.toggle when add button is clicked in favorites view', () => {
            // Setup: Configure for favorites list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
            mockStore.searchParams.listType.set(DotUVEPaletteListTypes.FAVORITES);
            mockStore.$isFavoritesList.set(true); // Set computed signal to match favorites view
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.$isEmpty.set(false); // Ensure controls are visible
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);

            spectator.detectChanges();

            // Verify add favorites button is visible
            const addButton = spectator.query('[data-testid="add-favorites-button"]');
            expect(addButton).toBeTruthy();

            // Query the DotFavoriteSelectorComponent using Spectator
            const favoritesPanelComponent = spectator.query(DotFavoriteSelectorComponent);
            expect(favoritesPanelComponent).toBeTruthy();

            // Create spy on the component's toggle method
            const toggleSpy = jest.spyOn(favoritesPanelComponent, 'toggle');

            // Trigger click on the add button
            const mockEvent = new MouseEvent('click');
            spectator.triggerEventHandler(
                '[data-testid="add-favorites-button"]',
                'onClick',
                mockEvent
            );

            // Verify toggle was called with the event
            expect(toggleSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should display the empty state message for CONTENT list type', () => {
            // Setup: Empty state with no content for CONTENT list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true); // Set isEmpty to true for empty state to render
            mockStore.contenttypes.set([]);

            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the CONTENT list type empty message
            // Note: DotMessageService mock returns the key itself (line 118: get: (key: string) => key)
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.contenttypes.message'
            );
        });

        it('should display the empty state message for FAVORITES list type', () => {
            // Setup: Empty state with no content for FAVORITES list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
            mockStore.searchParams.listType.set(DotUVEPaletteListTypes.FAVORITES);
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the FAVORITES list type empty message
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.favorites.message'
            );
        });

        it('should display the empty state message for WIDGET list type', () => {
            // Setup: Empty state with no content for WIDGET list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.WIDGET);
            mockStore.searchParams.listType.set(DotUVEPaletteListTypes.WIDGET);
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the WIDGET list type empty message
            expect(emptyStateMessage?.textContent).toBe('uve.palette.empty.state.widgets.message');
        });

        it('should display the search empty state message when search returns no results', () => {
            // Setup: Empty state with search term
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            // Set a search term in the form control
            spectator.component.searchControl.setValue('nonexistent search term');

            // Force the computed signal to re-evaluate by updating a signal dependency
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.WIDGET);
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the search empty state message
            expect(emptyStateMessage?.textContent).toBe('uve.palette.empty.search.state.message');
        });

        it('should display the empty state message for CONTENTLETS view (drilling into a content type)', () => {
            // Setup: Empty contentlets view (drilling into Blog content type with no contentlets)
            mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.$isContentletsView.set(true);
            mockStore.$isContentTypesView.set(false);
            mockStore.contentlets.set([]);
            mockStore.searchParams.selectedContentType.set('Blog');

            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the contentlets empty state message
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.contentlets.message'
            );
        });

        it('should display the correct icon for CONTENT list type empty state', () => {
            // Setup: Empty state with no content for CONTENT list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            spectator.detectChanges();

            // Query the empty state icon element
            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');
            expect(emptyStateIcon).toBeTruthy();

            // Verify the icon class matches the CONTENT list type icon
            expect(emptyStateIcon?.className).toContain('pi-folder-open');
        });

        it('should display the correct icon for FAVORITES list type empty state', () => {
            // Setup: Empty state for FAVORITES list type
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
            mockStore.searchParams.listType.set(DotUVEPaletteListTypes.FAVORITES);
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.$isContentTypesView.set(true);
            mockStore.$isContentletsView.set(false); // Ensure not in contentlets view
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            spectator.detectChanges();

            // Query the empty state icon element
            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');
            expect(emptyStateIcon).toBeTruthy();

            // Verify the icon class matches the FAVORITES list type icon
            expect(emptyStateIcon?.className).toContain('pi-plus');
        });

        it('should display the correct icon for search empty state', () => {
            // Setup: Empty state with search term
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            // Set a search term
            spectator.component.searchControl.setValue('nonexistent');

            // Force the computed signal to re-evaluate by updating a signal dependency
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            spectator.detectChanges();

            // Query the empty state icon element
            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');
            expect(emptyStateIcon).toBeTruthy();

            // Verify the icon class matches the search empty state icon
            expect(emptyStateIcon?.className).toContain('pi-search');
        });

        it('should display the correct icon for CONTENTLETS view empty state', () => {
            // Setup: Empty contentlets view (drilling into content type with no contentlets)
            mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.$isContentletsView.set(true);
            mockStore.$isContentTypesView.set(false);
            mockStore.contentlets.set([]);

            spectator.detectChanges();

            // Query the empty state icon element
            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');
            expect(emptyStateIcon).toBeTruthy();

            // Verify the icon class matches the contentlets empty state icon (folder-open)
            expect(emptyStateIcon?.className).toContain('pi-folder-open');
        });

        it('should call menu.toggle when sort menu button is clicked in content types view', () => {
            // Setup: Content types view with CONTENT list type (not FAVORITES)
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            mockStore.searchParams.listType.set(DotUVEPaletteListTypes.CONTENT);
            mockStore.$isFavoritesList.set(false); // Ensure not in favorites view
            mockStore.$isContentTypesView.set(true); // Ensure in content types view
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.$isEmpty.set(false); // Set isEmpty to false so controls are visible
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);

            spectator.detectChanges();

            // Verify sort menu button is visible
            const sortMenuButton = spectator.query('[data-testid="sort-menu-button"]');
            expect(sortMenuButton).toBeTruthy();

            // Query the PrimeNG Menu component using Spectator
            const menuComponent = spectator.query(Menu);
            expect(menuComponent).toBeTruthy();

            // Create spy on the menu's toggle method
            const toggleSpy = jest.spyOn(menuComponent, 'toggle');

            // Trigger click on the sort menu button
            const mockEvent = new MouseEvent('click');
            spectator.triggerEventHandler('[data-testid="sort-menu-button"]', 'onClick', mockEvent);

            // Verify toggle was called with the event
            expect(toggleSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should call contextMenu.show when content type is right-clicked', () => {
            // Setup: Content types view with mock data
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.LOADED);
            mockStore.$isLoading.set(false); // Ensure content renders (not loading)
            mockStore.$isEmpty.set(false); // Ensure content renders (not empty)
            mockStore.contenttypes.set([
                {
                    id: '1',
                    name: 'Blog',
                    variable: 'blog',
                    baseType: 'CONTENT'
                } as DotCMSContentType
            ]);
            mockStore.$isContentTypesView.set(true);
            mockStore.$isContentletsView.set(false);

            spectator.detectChanges();

            // Verify content type component is rendered
            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            expect(contentTypeComponent).toBeTruthy();

            // Query the PrimeNG ContextMenu component using Spectator
            const contextMenuComponent = spectator.query(ContextMenu);
            expect(contextMenuComponent).toBeTruthy();

            // Create spy on the context menu's show method
            const showSpy = jest.spyOn(contextMenuComponent, 'show');

            // Trigger context menu event on the content type component
            const mockEvent = new MouseEvent('contextmenu');
            spectator.triggerEventHandler('dot-uve-palette-contenttype', 'contextMenu', mockEvent);

            // Verify show was called with the event
            expect(showSpy).toHaveBeenCalledWith(mockEvent);
        });
    });
});
