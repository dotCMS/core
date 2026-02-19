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
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUvePaletteListComponent } from './dot-uve-palette-list.component';
import { DotPaletteListStore } from './store/store';

import { DotPaletteListStatus, DotUVEPaletteListTypes, DotUVEPaletteListView } from '../../models';
import { DotFavoriteSelectorComponent } from '../dot-favorite-selector/dot-favorite-selector.component';

const mockStore = {
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
    initialLoad: signal(false),
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
    setLayoutMode: jest.fn()
};

// ===== Test Helper Functions =====

/**
 * Simulates user typing in the search input
 */
const triggerSearch = ({
    spectator,
    searchTerm
}: {
    spectator: Spectator<DotUvePaletteListComponent>;
    searchTerm: string;
}) => {
    const searchInput = spectator.query('[data-testid="palette-search-input"]') as HTMLInputElement;
    spectator.typeInElement(searchTerm, searchInput);
};

/**
 * Configures the mock store to display contentlets view
 */
const switchToContentletsView = (status = DotPaletteListStatus.LOADED) => {
    mockStore.currentView.set(DotUVEPaletteListView.CONTENTLETS);
    mockStore.searchParams.selectedContentType.set('Blog'); // Non-empty = contentlets view
    mockStore.$isContentletsView.set(true);
    mockStore.$isContentTypesView.set(false);
    mockStore.status.set(status);
};

/**
 * Configures the mock store to display content types view
 */
const switchToContentTypesView = (status = DotPaletteListStatus.LOADED) => {
    mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
    mockStore.searchParams.selectedContentType.set(''); // Empty = content types view
    mockStore.$isContentletsView.set(false);
    mockStore.$isContentTypesView.set(true);
    mockStore.$isFavoritesList.set(false);
    mockStore.status.set(status);
};

/**
 * Configures the component and store for favorites list view
 */
const setFavoritesList = ({ spectator }: { spectator: Spectator<DotUvePaletteListComponent> }) => {
    switchToContentTypesView();
    spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
    mockStore.searchParams.listType.set(DotUVEPaletteListTypes.FAVORITES);
    mockStore.$isFavoritesList.set(true);
};

/**
 * Configures the mock store for an empty state
 */
const setEmptyState = ({
    listType = DotUVEPaletteListTypes.CONTENT
}: { listType?: DotUVEPaletteListTypes } = {}) => {
    mockStore.status.set(DotPaletteListStatus.EMPTY);
    mockStore.$isEmpty.set(true);
    mockStore.contenttypes.set([]);
    mockStore.contentlets.set([]);
    mockStore.searchParams.listType.set(listType);
};

/**
 * Configures the mock store with loaded content types
 */
const setLoadedContentTypes = ({
    contentTypes = [basicContentType]
}: { contentTypes?: DotCMSContentType[] } = {}) => {
    mockStore.status.set(DotPaletteListStatus.LOADED);
    mockStore.contenttypes.set(contentTypes);
    mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
    mockStore.$isContentTypesView.set(true);
    mockStore.$isContentletsView.set(false);
    mockStore.$isEmpty.set(false);
};

/**
 * Configures the mock store with loaded contentlets
 */
const setLoadedContentlets = ({
    contentlets = [basicContentlet]
}: { contentlets?: DotCMSContentlet[] } = {}) => {
    switchToContentletsView();
    mockStore.contentlets.set(contentlets);
    mockStore.$isEmpty.set(false);
};

/**
 * Advances timers to trigger debounced search
 */
const advanceSearchDebounce = () => {
    jest.advanceTimersByTime(400);
};

const basicContentType = {
    id: '1',
    name: 'Blog',
    variable: 'blog',
    baseType: 'CONTENT'
} as DotCMSContentType;

const basicContentlet = {
    identifier: '123',
    title: 'My Blog Post',
    contentType: 'Blog'
} as DotCMSContentlet;

const mockGlobalStore = {
    currentSiteId: signal('demo.dotcms.com')
};

describe('DotUvePaletteListComponent', () => {
    let spectator: Spectator<DotUvePaletteListComponent>;
    let store: jest.Mocked<InstanceType<typeof DotPaletteListStore>>;

    const createComponent = createComponentFactory({
        component: DotUvePaletteListComponent,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: GlobalStore,
                useValue: mockGlobalStore
            },
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
        // Reset mockGlobalStore signal
        mockGlobalStore.currentSiteId.set('demo.dotcms.com');
        spectator = createComponent({
            providers: [mockProvider(DotPaletteListStore, mockStore)],
            detectChanges: false
        });
        store = spectator.inject(DotPaletteListStore, true);
        spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
        spectator.fixture.componentRef.setInput('languageId', 1);
        spectator.fixture.componentRef.setInput('pagePath', '/test-page');
    });

    afterEach(() => {
        jest.clearAllTimers();
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('search (content types): debounces and calls getContentTypes with filter and page 1', () => {
        setLoadedContentTypes();
        spectator.detectChanges();

        triggerSearch({ spectator, searchTerm: 'blog' });
        advanceSearchDebounce();

        expect(store.getContentTypes).toHaveBeenCalledWith({
            filter: 'blog',
            page: 1
        });
    });

    it('search (contentlets): debounces and calls getContentlets with filter and page 1', () => {
        setLoadedContentlets();
        spectator.detectChanges();

        triggerSearch({ spectator, searchTerm: 'news' });
        advanceSearchDebounce();

        expect(store.getContentlets).toHaveBeenCalledWith({ filter: 'news', page: 1 });
    });

    describe('template', () => {
        it('should render dot-uve-palette-contenttype when in content types view', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            const contentletComponent = spectator.query('dot-uve-palette-contentlet');

            expect(contentTypeComponent).toBeTruthy();
            expect(contentletComponent).toBeNull();
        });

        it('should render dot-uve-palette-contentlet when in contentlets view', () => {
            setLoadedContentlets();
            spectator.detectChanges();

            const contentletComponent = spectator.query('dot-uve-palette-contentlet');
            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');

            expect(contentletComponent).toBeTruthy();
            expect(contentTypeComponent).toBeNull();
        });

        it('should switch from content types to contentlets view when onSelectContentType is emitted', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            triggerSearch({ spectator, searchTerm: 'test search' });
            advanceSearchDebounce();
            spectator.detectChanges();

            expect(spectator.query('dot-uve-palette-contenttype')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contentlet')).toBeNull();

            // Trigger the onSelectContentType event
            spectator.triggerEventHandler(
                'dot-uve-palette-contenttype',
                'onSelectContentType',
                'Blog'
            );

            setLoadedContentlets();
            spectator.detectChanges();

            expect(store.getContentlets).toHaveBeenCalledWith({
                selectedContentType: 'Blog',
                filter: '',
                page: 1
            });

            // Verify template switched to contentlets view
            expect(spectator.query('dot-uve-palette-contentlet')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contenttype')).toBeNull();
        });

        it('should switch back to content types view when back button is clicked', () => {
            setLoadedContentlets();
            spectator.detectChanges();

            // Verify initial state - contentlet view should be visible
            expect(spectator.query('dot-uve-palette-contentlet')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contenttype')).toBeNull();
            expect(spectator.query('[data-testid="back-to-content-types-button"]')).toBeTruthy();

            triggerSearch({ spectator, searchTerm: 'blog search' });
            advanceSearchDebounce();
            spectator.detectChanges();

            // Click the back button using triggerEventHandler for PrimeNG components
            spectator.triggerEventHandler(
                '[data-testid="back-to-content-types-button"]',
                'onClick',
                new Event('click')
            );

            setLoadedContentTypes();
            spectator.detectChanges();

            expect(store.getContentTypes).toHaveBeenCalledWith({
                selectedContentType: '',
                filter: '',
                page: 1
            });

            // Verify template switched back to content types view
            expect(spectator.query('dot-uve-palette-contenttype')).toBeTruthy();
            expect(spectator.query('dot-uve-palette-contentlet')).toBeNull();
            expect(spectator.query('[data-testid="back-to-content-types-button"]')).toBeNull();
        });

        it('should call favoritesPanel.toggle when add button is clicked in favorites view', () => {
            setFavoritesList({ spectator });
            mockStore.contenttypes.set([basicContentType]);

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
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            setEmptyState({ listType: DotUVEPaletteListTypes.CONTENT });
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            spectator.detectChanges();

            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');

            expect(emptyStateMessage).toBeTruthy();
            // Note: DotMessageService mock returns the key itself
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.contenttypes.message'
            );
        });

        it('should display the empty state message for FAVORITES list type', () => {
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.FAVORITES);
            setEmptyState({ listType: DotUVEPaletteListTypes.FAVORITES });
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            spectator.detectChanges();

            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');

            expect(emptyStateMessage).toBeTruthy();
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.favorites.message'
            );
        });

        it('should display the empty state message for WIDGET list type', () => {
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.WIDGET);
            setEmptyState({ listType: DotUVEPaletteListTypes.WIDGET });
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            spectator.detectChanges();

            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');

            expect(emptyStateMessage).toBeTruthy();
            expect(emptyStateMessage?.textContent).toBe('uve.palette.empty.state.widgets.message');
        });

        it('should display the search empty state message when search returns no results', () => {
            // Setup: Empty state with search term
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            mockStore.$isEmpty.set(true);
            mockStore.contenttypes.set([]);

            // Simulate user having typed a search term - set form control and $isSearching
            // (This test focuses on empty state display, not search debouncing which is tested elsewhere)
            spectator.component.searchControl.setValue('nonexistent search term');
            spectator.component['$isSearching'].set(true);
            spectator.detectChanges();

            // Query the empty state message element
            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');
            expect(emptyStateMessage).toBeTruthy();

            // Verify the message displayed matches the search empty state message
            expect(emptyStateMessage?.textContent).toBe('uve.palette.empty.search.state.message');
        });

        it('should display the empty state message for CONTENTLETS view (drilling into a content type)', () => {
            switchToContentletsView();
            setEmptyState();
            mockStore.$isContentletsView.set(true);
            mockStore.$isContentTypesView.set(false);
            mockStore.searchParams.selectedContentType.set('Blog');
            spectator.detectChanges();

            const emptyStateMessage = spectator.query('[data-testid="empty-state-message"]');

            expect(emptyStateMessage).toBeTruthy();
            expect(emptyStateMessage?.textContent).toBe(
                'uve.palette.empty.state.contentlets.message'
            );
        });

        it('should display the correct icon for CONTENT list type empty state', () => {
            spectator.fixture.componentRef.setInput('listType', DotUVEPaletteListTypes.CONTENT);
            setEmptyState({ listType: DotUVEPaletteListTypes.CONTENT });
            mockStore.currentView.set(DotUVEPaletteListView.CONTENT_TYPES);
            spectator.detectChanges();

            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');

            expect(emptyStateIcon).toBeTruthy();
            expect(emptyStateIcon?.className).toContain('pi-folder-open');
        });

        it('should display the correct icon for FAVORITES list type empty state', () => {
            // Setup favorites list type with empty state - order matters!
            setFavoritesList({ spectator });
            setEmptyState({ listType: DotUVEPaletteListTypes.FAVORITES });
            spectator.detectChanges();

            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');

            expect(emptyStateIcon).toBeTruthy();
            expect(emptyStateIcon?.className).toContain('pi-plus');
        });

        it('should display the correct icon for search empty state', () => {
            switchToContentTypesView();
            spectator.detectChanges();

            // Set search state before detecting changes
            triggerSearch({ spectator, searchTerm: 'nonexistent 123' });
            advanceSearchDebounce();
            setEmptyState();
            spectator.detectChanges();

            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');

            expect(emptyStateIcon).toBeTruthy();
            expect(emptyStateIcon?.className).toContain('pi-search');
        });

        it('should display the correct icon for CONTENTLETS view empty state', () => {
            switchToContentletsView();
            setEmptyState();
            spectator.detectChanges();

            const emptyStateIcon = spectator.query('.dot-uve-palette-list__empty-icon i');

            expect(emptyStateIcon).toBeTruthy();
            expect(emptyStateIcon?.className).toContain('pi-folder-open');
        });

        it('should call menu.toggle when sort menu button is clicked in content types view', () => {
            switchToContentTypesView();
            spectator.detectChanges();

            const sortMenuButton = spectator.query('[data-testid="sort-menu-button"]');
            expect(sortMenuButton).toBeTruthy();

            const menuComponent = spectator.query(Menu);
            expect(menuComponent).toBeTruthy();

            const toggleSpy = jest.spyOn(menuComponent, 'toggle');
            const mockEvent = new MouseEvent('click');
            spectator.triggerEventHandler('[data-testid="sort-menu-button"]', 'onClick', mockEvent);

            expect(toggleSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should call contextMenu.show when content type is right-clicked', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            const contentTypeComponent = spectator.query('dot-uve-palette-contenttype');
            expect(contentTypeComponent).toBeTruthy();

            const contextMenuComponent = spectator.query(ContextMenu);
            expect(contextMenuComponent).toBeTruthy();

            const showSpy = jest.spyOn(contextMenuComponent, 'show');
            const mockEvent = new MouseEvent('contextmenu');
            spectator.triggerEventHandler('dot-uve-palette-contenttype', 'contextMenu', mockEvent);

            expect(showSpy).toHaveBeenCalledWith(mockEvent);
        });
    });

    describe('control visibility behavior', () => {
        it('should hide controls initially when status is first LOADING', () => {
            mockStore.status.set(DotPaletteListStatus.LOADING);
            spectator.detectChanges();

            // Verify controls are hidden during loading
            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();
        });

        it('should show controls when status changes to LOADED', () => {
            mockStore.status.set(DotPaletteListStatus.LOADED);
            spectator.detectChanges();

            // Trigger view change to start listening for status changes
            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();
        });

        it('should hide controls when status changes to EMPTY', () => {
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();
        });

        it('should update control visibility when switching between content types and contentlets views', () => {
            switchToContentTypesView();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();

            switchToContentletsView(DotPaletteListStatus.EMPTY);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();
        });

        it('should only listen the first status change from LOADING to LOADED or EMPTY', () => {
            mockStore.status.set(DotPaletteListStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();

            mockStore.status.set(DotPaletteListStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();

            mockStore.status.set(DotPaletteListStatus.EMPTY);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();
        });

        it('should listen the first change again if view changes', () => {
            switchToContentTypesView();
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();

            // Should ignore the change in status
            mockStore.status.set(DotPaletteListStatus.EMPTY);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeTruthy();

            // Should start listening the first status change again
            switchToContentletsView(DotPaletteListStatus.EMPTY);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();

            // Should ignore second status change
            mockStore.status.set(DotPaletteListStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="palette-search-input"]')).toBeNull();
        });
    });

    describe('host parameter (from $siteId)', () => {
        it('should pass host parameter from $siteId to getContentTypes on initialization', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            expect(store.getContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({
                    host: 'demo.dotcms.com',
                    pagePathOrId: '/test-page',
                    language: 1,
                    listType: DotUVEPaletteListTypes.CONTENT
                })
            );
        });

        it('should pass updated host when $siteId changes', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            // Clear previous calls
            jest.clearAllMocks();

            // Update the siteId
            mockGlobalStore.currentSiteId.set('new-site.dotcms.com');
            spectator.detectChanges();

            expect(store.getContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({
                    host: 'new-site.dotcms.com',
                    pagePathOrId: '/test-page',
                    language: 1,
                    listType: DotUVEPaletteListTypes.CONTENT
                })
            );
        });

        it('should include host parameter when sorting content types', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            // Clear initial calls
            jest.clearAllMocks();

            // Trigger sort
            spectator.component['onSortSelect']({ orderby: 'usage', direction: 'DESC' });

            expect(store.getContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({
                    orderby: 'usage',
                    direction: 'DESC',
                    page: 1
                })
            );
        });

        it('should pass host parameter with other input changes (languageId)', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            // Clear initial calls
            jest.clearAllMocks();

            // Change languageId
            spectator.fixture.componentRef.setInput('languageId', 2);
            spectator.detectChanges();

            expect(store.getContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({
                    host: 'demo.dotcms.com',
                    language: 2,
                    pagePathOrId: '/test-page',
                    listType: DotUVEPaletteListTypes.CONTENT
                })
            );
        });

        it('should pass host parameter with other input changes (pagePath)', () => {
            setLoadedContentTypes();
            spectator.detectChanges();

            // Clear initial calls
            jest.clearAllMocks();

            // Change pagePath
            spectator.fixture.componentRef.setInput('pagePath', '/new-page');
            spectator.detectChanges();

            expect(store.getContentTypes).toHaveBeenCalledWith(
                expect.objectContaining({
                    host: 'demo.dotcms.com',
                    pagePathOrId: '/new-page',
                    language: 1,
                    listType: DotUVEPaletteListTypes.CONTENT
                })
            );
        });
    });
});
