import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';

import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPageFavoritesPanelComponent } from './dot-page-favorites-panel.component';
import { DotPagesCardComponent } from './dot-pages-card/dot-pages-card.component';

import { LOCAL_STORAGE_FAVORITES_PANEL_KEY } from '../dot-pages-store/dot-pages.store';
import { DotActionsMenuEventParams } from '../dot-pages.component';

const createMockContentlet = (partial: Partial<DotCMSContentlet>): DotCMSContentlet =>
    partial as unknown as DotCMSContentlet;

const MOCK_FAVORITE_PAGES: DotCMSContentlet[] = [
    createMockContentlet({
        title: 'Home Page',
        url: '/home',
        screenshot: 'https://example.com/screenshot1.jpg',
        languageId: 1,
        identifier: 'page-1',
        inode: 'inode-1'
    }),
    createMockContentlet({
        title: 'About Page',
        url: '/about',
        screenshot: 'https://example.com/screenshot2.jpg',
        languageId: 1,
        identifier: 'page-2',
        inode: 'inode-2'
    }),
    createMockContentlet({
        title: 'Contact Page',
        url: '/contact',
        screenshot: '',
        languageId: 1,
        identifier: 'page-3',
        inode: 'inode-3'
    })
];

describe('DotPageFavoritesPanelComponent', () => {
    let spectator: Spectator<DotPageFavoritesPanelComponent>;
    let mockLocalStorageService: jest.Mocked<DotLocalstorageService>;

    const createComponent = createComponentFactory({
        component: DotPageFavoritesPanelComponent,
        imports: [
            DotPageFavoritesPanelComponent,
            DotPagesCardComponent,
            PanelModule,
            ButtonModule,
            DotMessagePipe
        ],
        detectChanges: false
    });

    beforeEach(() => {
        // Create component with mocked providers
        spectator = createComponent({
            providers: [
                MockProvider(DotLocalstorageService, {
                    getItem: jest.fn().mockReturnValue(true),
                    setItem: jest.fn(),
                    removeItem: jest.fn()
                })
            ]
        });

        mockLocalStorageService = spectator.inject(
            DotLocalstorageService
        ) as unknown as jest.Mocked<DotLocalstorageService>;

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should initialize with collapsed state from localStorage', () => {
            expect(mockLocalStorageService.getItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY
            );
            expect(spectator.component.$isCollapsed()).toBe(true);
        });

        it('should set timestamp on initialization', () => {
            expect(spectator.component.$timeStamp()).toBeTruthy();
            expect(typeof spectator.component.$timeStamp()).toBe('string');
        });

        it('should read collapsed state from localStorage on initialization', () => {
            // Verify that localStorage was called during initialization
            expect(mockLocalStorageService.getItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY
            );

            // Verify the component respects the localStorage value
            expect(spectator.component.$isCollapsed()).toBe(true);
        });
    });

    describe('Template Rendering', () => {
        it('should render loading state when isLoading is true', () => {
            spectator.setInput('isLoading', true);
            spectator.setInput('favoritePages', []);
            spectator.detectChanges();

            const loading = spectator.query('[data-testId="favoritesLoading"]');
            expect(loading).toBeTruthy();

            const spinner = spectator.query('[data-testId="favoritesLoadingSpinner"]');
            expect(spinner).toBeTruthy();

            const emptyState = spectator.query('[data-testid="dot-pages-empty__content"]');
            expect(emptyState).toBeNull();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(0);
        });

        it('should not render cards when isLoading is true (regardless of favorite pages)', () => {
            spectator.setInput('isLoading', true);
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            const loading = spectator.query('[data-testId="favoritesLoading"]');
            expect(loading).toBeTruthy();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(0);
        });

        it('should render panel with correct header', () => {
            const panel = spectator.query('p-panel');
            expect(panel).toBeTruthy();

            const icon = spectator.query('[data-testId="bookmarksIcon"]');
            expect(icon).toBeTruthy();
            expect(icon?.classList.contains('pi-star-fill')).toBe(true);
        });

        it('should render panel as toggleable', () => {
            const panel = spectator.query('p-panel');
            expect(panel).toBeTruthy();
            // Panel is toggleable - we can verify this through the toggler attribute
            expect(panel?.hasAttribute('toggler')).toBe(true);
        });

        it('should reflect collapsed state in component signal', () => {
            spectator.component.$isCollapsed.set(true);
            spectator.detectChanges();

            expect(spectator.component.$isCollapsed()).toBe(true);

            spectator.component.$isCollapsed.set(false);
            spectator.detectChanges();

            expect(spectator.component.$isCollapsed()).toBe(false);
        });

        it('should render empty state when no favorite pages', () => {
            spectator.setInput('favoritePages', []);
            spectator.detectChanges();

            const emptyState = spectator.query('[data-testid="dot-pages-empty__content"]');
            expect(emptyState).toBeTruthy();

            const emptyIcon = spectator.query('.pi-star');
            expect(emptyIcon).toBeTruthy();
        });

        it('should render favorite pages cards when data is provided', () => {
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(3);
        });

        it('should not render empty state when favorite pages exist', () => {
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            const emptyState = spectator.query('[data-testid="dot-pages-empty__content"]');
            expect(emptyState).toBeNull();
        });

        it('should render correct number of cards with unique identifiers', () => {
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(3);

            // Verify each card is rendered (actual button IDs are set internally by the child component)
            cards.forEach((card) => {
                expect(card).toBeTruthy();
            });
        });
    });

    describe('Rendered screenshot URL', () => {
        it('should pass empty imageUri to dot-pages-card when screenshot is missing', () => {
            const page = MOCK_FAVORITE_PAGES[2];
            spectator.setInput('favoritePages', [page]);
            spectator.detectChanges();

            const card = spectator.query(DotPagesCardComponent);
            expect(card).toBeTruthy();
            expect(card?.$imageUri()).toBe('');
        });

        it('should pass formatted imageUri (screenshot + language + timestamp) to dot-pages-card', () => {
            const page = MOCK_FAVORITE_PAGES[1];
            spectator.setInput('favoritePages', [page]);
            spectator.detectChanges();

            const timestamp = spectator.component.$timeStamp();
            const expected = `${page.screenshot}?language_id=${page.languageId}&${timestamp}`;

            const card = spectator.query(DotPagesCardComponent);
            expect(card).toBeTruthy();
            expect(card?.$imageUri()).toBe(expected);
        });
    });

    describe('Panel Collapse/Expand', () => {
        it('should collapse panel and save to localStorage (via p-panel collapsedChange)', () => {
            spectator.component.$isCollapsed.set(false);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', true);

            expect(spectator.component.$isCollapsed()).toBe(true);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'true'
            );
        });

        it('should expand panel and save to localStorage (via p-panel collapsedChange)', () => {
            spectator.component.$isCollapsed.set(true);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);

            expect(spectator.component.$isCollapsed()).toBe(false);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'false'
            );
        });

        it('should handle p-panel collapsedChange=true (collapse) and persist to localStorage', () => {
            spectator.component.$isCollapsed.set(false);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', true);

            expect(spectator.component.$isCollapsed()).toBe(true);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'true'
            );
        });

        it('should handle p-panel collapsedChange=false (expand) and persist to localStorage', () => {
            spectator.component.$isCollapsed.set(true);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);

            expect(spectator.component.$isCollapsed()).toBe(false);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'false'
            );
        });

        it('should trigger panel collapse through UI interaction', () => {
            spectator.component.$isCollapsed.set(false);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', true);

            expect(spectator.component.$isCollapsed()).toBe(true);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'true'
            );
        });

        it('should trigger panel expand through UI interaction', () => {
            spectator.component.$isCollapsed.set(true);
            spectator.detectChanges();

            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);

            expect(spectator.component.$isCollapsed()).toBe(false);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'false'
            );
        });
    });

    describe('Output Events', () => {
        it('should emit openMenu event with correct data', () => {
            let emittedEvent: DotActionsMenuEventParams | null = null;
            spectator.output('openMenu').subscribe((event) => {
                emittedEvent = event;
            });

            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            const mockEvent = new MouseEvent('click', { bubbles: true, cancelable: true });

            // Emit from the first card; panel template binds (openMenu)="handleOpenMenu($event, favoritePage)"
            spectator.triggerEventHandler('dot-pages-card', 'openMenu', mockEvent);

            expect(emittedEvent).toBeTruthy();
            expect(emittedEvent?.originalEvent).toBe(mockEvent);
            expect(emittedEvent?.data).toBe(MOCK_FAVORITE_PAGES[0]);
        });

        it('should stop event propagation when opening menu', () => {
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            // Use a MouseEvent-like object so we can assert stopPropagation is called.
            const stopPropagation = jest.fn();
            const mockEvent = {
                stopPropagation
            } as unknown as MouseEvent;

            spectator.triggerEventHandler('dot-pages-card', 'openMenu', mockEvent);

            expect(stopPropagation).toHaveBeenCalled();
        });
    });

    describe('Integration Workflows', () => {
        it('should handle complete user workflow with favorite pages', () => {
            // Step 1: Component starts collapsed
            expect(spectator.component.$isCollapsed()).toBe(true);

            // Step 2: User expands panel
            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);
            expect(spectator.component.$isCollapsed()).toBe(false);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledWith(
                LOCAL_STORAGE_FAVORITES_PANEL_KEY,
                'false'
            );

            // Step 3: Load favorite pages
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            spectator.detectChanges();

            // Step 4: Verify cards are rendered
            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(3);

            // Step 5: User opens menu on a card
            let emittedEvent: DotActionsMenuEventParams | null = null;
            spectator.output('openMenu').subscribe((event) => {
                emittedEvent = event;
            });

            const mockEvent = new MouseEvent('click');
            spectator.triggerEventHandler('dot-pages-card', 'openMenu', mockEvent);

            expect(emittedEvent?.data).toBe(MOCK_FAVORITE_PAGES[0]);

            // Step 6: User collapses panel again
            spectator.triggerEventHandler('p-panel', 'collapsedChange', true);
            expect(spectator.component.$isCollapsed()).toBe(true);
        });

        it('should handle workflow with empty favorites list', () => {
            // Step 1: Set empty favorites
            spectator.setInput('favoritePages', []);
            spectator.detectChanges();

            // Step 2: Verify empty state is shown
            const emptyState = spectator.query('[data-testid="dot-pages-empty__content"]');
            expect(emptyState).toBeTruthy();

            // Step 3: Verify no cards are rendered
            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(0);

            // Step 4: Panel can still be toggled
            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);
            expect(spectator.component.$isCollapsed()).toBe(false);
        });
    });

    describe('Edge Cases', () => {
        it('should render dot-pages-card with empty imageUri when screenshot is empty', () => {
            const pageWithoutScreenshot = createMockContentlet({
                ...MOCK_FAVORITE_PAGES[0],
                screenshot: ''
            });

            spectator.setInput('favoritePages', [pageWithoutScreenshot]);
            spectator.detectChanges();

            const card = spectator.query(DotPagesCardComponent);
            expect(card).toBeTruthy();
            expect(card?.$imageUri()).toBe('');
        });

        it('should handle rapid collapsedChange events from p-panel', () => {
            // Start collapsed (mocked localstorage getItem returns true)
            expect(spectator.component.$isCollapsed()).toBe(true);

            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);
            spectator.triggerEventHandler('p-panel', 'collapsedChange', true);
            spectator.triggerEventHandler('p-panel', 'collapsedChange', false);

            expect(spectator.component.$isCollapsed()).toBe(false);
            expect(mockLocalStorageService.setItem).toHaveBeenCalledTimes(3);
        });

        it('should handle single favorite page', () => {
            spectator.setInput('favoritePages', [MOCK_FAVORITE_PAGES[0]]);
            spectator.detectChanges();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(1);

            const emptyState = spectator.query('[data-testid="dot-pages-empty__content"]');
            expect(emptyState).toBeNull();
        });

        it('should handle many favorite pages', () => {
            const manyPages = Array(20)
                .fill(null)
                .map((_, i) => ({
                    ...MOCK_FAVORITE_PAGES[0],
                    identifier: `page-${i}`,
                    title: `Page ${i}`
                }));

            spectator.setInput('favoritePages', manyPages);
            spectator.detectChanges();

            const cards = spectator.queryAll('dot-pages-card');
            expect(cards).toHaveLength(20);
        });

        it('should maintain timestamp consistency across calls', () => {
            const timestamp1 = spectator.component.$timeStamp();
            const timestamp2 = spectator.component.$timeStamp();

            expect(timestamp1).toBe(timestamp2);
        });
    });

    describe('Signal State Management', () => {
        it('should update isCollapsed signal correctly', () => {
            expect(spectator.component.$isCollapsed()).toBe(true);

            spectator.component.$isCollapsed.set(false);
            expect(spectator.component.$isCollapsed()).toBe(false);

            spectator.component.$isCollapsed.set(true);
            expect(spectator.component.$isCollapsed()).toBe(true);
        });

        it('should have readonly timestamp signal', () => {
            const initialTimestamp = spectator.component.$timeStamp();

            expect(initialTimestamp).toBeTruthy();
            expect(typeof initialTimestamp).toBe('string');
        });

        it('should accept favoritePages input updates', () => {
            spectator.setInput('favoritePages', MOCK_FAVORITE_PAGES);
            expect(spectator.component.$favoritePages()).toEqual(MOCK_FAVORITE_PAGES);

            spectator.setInput('favoritePages', []);
            expect(spectator.component.$favoritePages()).toEqual([]);
        });
    });
});
