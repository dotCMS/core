import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, ngMocks } from 'ng-mocks';

import { computed, signal } from '@angular/core';

import { DotPageLayoutService } from '@dotcms/data-access';
import { DotUvePaletteListComponent, DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';

import { DotRowReorderComponent } from './components/dot-row-reorder/dot-row-reorder.component';
import { DotUvePaletteComponent } from './dot-uve-palette.component';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

/**
 * Helper function to trigger tab change event
 * Simulates the valueChange event that p-tabs (PrimeNG v21) emits when a tab is clicked.
 * Also updates the host's activeTab to reflect the change (simulating real parent behavior).
 */
/**
 * Triggers a tab change by calling the component's handleTabChange (same as p-tabs valueChange).
 */
function triggerTabChange(spectator: Spectator<DotUvePaletteComponent>, index: number): void {
    (
        spectator.component as DotUvePaletteComponent & {
            handleTabChange: (value: number) => void;
        }
    ).handleTabChange(index);
    spectator.fixture.detectChanges();
}

/**
 * Mock UVEStore with default test values
 */
const mockActiveContentlet = signal(null);
const mockUVEStore = {
    pageURI: signal('/test/page/path'),
    pageLanguageId: signal(1),
    pageVariantId: signal('DEFAULT'),
    $allowedContentTypes: signal<Record<string, true>>({}),
    $isStyleEditorEnabled: signal(false),
    $canEditStyles: () => false, // Computed property used by component
    $styleSchema: signal(undefined),
    // editor() method returns editor state with activeContentlet
    // Must be a computed function to reflect changes when mockActiveContentlet changes
    editor: computed(() => ({
        activeContentlet: mockActiveContentlet(),
        dragItem: null,
        bounds: [],
        state: 'IDLE',
        contentArea: null,
        panels: {
            palette: { open: true },
            rightSidebar: { open: false }
        },
        ogTags: null,
        styleSchemas: []
    })),
    // Expose activeContentlet for test control
    editorActiveContentlet: mockActiveContentlet,
    // Single source of truth in current store API
    pageAsset: signal(null)
};

describe('DotUvePaletteComponent', () => {
    let spectator: Spectator<DotUvePaletteComponent>;

    const createComponent = createComponentFactory({
        component: DotUvePaletteComponent,
        imports: [DotUvePaletteComponent, MockComponent(DotUvePaletteListComponent)],
        overrideComponents: [
            [
                DotUvePaletteComponent,
                {
                    remove: { imports: [DotRowReorderComponent] },
                    add: { imports: [MockComponent(DotRowReorderComponent)] }
                }
            ]
        ],
        mocks: [DotPageLayoutService]
    });

    beforeEach(() => {
        // Mock scrollIntoView for PrimeNG TabView
        Element.prototype.scrollIntoView = jest.fn();

        // Reset mock store values
        mockUVEStore.pageURI.set('/test/page/path');
        mockUVEStore.pageLanguageId.set(1);
        mockUVEStore.pageVariantId.set('DEFAULT');
        mockUVEStore.$isStyleEditorEnabled.set(false);
        mockUVEStore.$styleSchema.set(undefined);
        // Reset activeContentlet to prevent auto-switch to STYLE_EDITOR
        // editor is now a computed that reflects mockActiveContentlet automatically
        mockActiveContentlet.set(null);

        spectator = createComponent({
            providers: [mockProvider(UVEStore, mockUVEStore)]
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Content Tab - Initial Render (Index 0)', () => {
        it('should render dot-uve-palette-list for Content tab by default', () => {
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should have active tab set to 0 by default', () => {
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });
    });

    describe('Tab Navigation - Local State', () => {
        it('should only render one dot-uve-palette-list at a time', () => {
            const paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should update local state and rendering when switching to Widget tab via onChange event', () => {
            // Trigger tab change via handleTabChange (same as p-tabs valueChange)
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should update local state and rendering when switching to Favorites tab via onChange event', () => {
            // Trigger tab change via handleTabChange (same as p-tabs valueChange)
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should switch back to Content tab when user clicks tab 0', () => {
            // First go to Widget tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Then go back to Content tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.CONTENT_TYPES);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should always render exactly one dot-uve-palette-list regardless of active tab', () => {
            // Check initial state (Content tab)
            let paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Widget tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Favorites tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should navigate through all tabs sequentially', () => {
            // Start at Content (0)
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);

            // Move to Widget (1)
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Move to Favorites (2)
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);

            // Move back to Content (0)
            triggerTabChange(spectator, UVE_PALETTE_TABS.CONTENT_TYPES);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });
    });

    describe('Layers tab', () => {
        // The LAYERS tab branches on whether the page's template is
        // standard (drawed=true → render row-reorder) or advanced
        // (drawed=false → render an empty-state explaining why layers
        // are not available). See $isStandardTemplate.
        it('renders dot-row-reorder when the template is standard (drawed=true)', () => {
            mockUVEStore.pageAsset.set({ template: { drawed: true } });
            spectator.detectChanges();

            triggerTabChange(spectator, UVE_PALETTE_TABS.LAYERS);

            expect(spectator.query('dot-row-reorder')).toBeTruthy();
            expect(spectator.query('[data-testid="layers-advanced-template-empty"]')).toBeNull();
        });

        it('renders the advanced-template empty-state when drawed=false', () => {
            mockUVEStore.pageAsset.set({ template: { drawed: false } });
            spectator.detectChanges();

            triggerTabChange(spectator, UVE_PALETTE_TABS.LAYERS);

            expect(spectator.query('dot-row-reorder')).toBeNull();
            expect(spectator.query('[data-testid="layers-advanced-template-empty"]')).toBeTruthy();
        });

        it('defaults to the standard branch while pageAsset is still loading', () => {
            // pageAsset null = page still loading; we should not flash
            // the empty-state at users with standard templates.
            mockUVEStore.pageAsset.set(null);
            spectator.detectChanges();

            triggerTabChange(spectator, UVE_PALETTE_TABS.LAYERS);

            expect(spectator.query('dot-row-reorder')).toBeTruthy();
        });
    });

    describe('Store Integration', () => {
        it('should initialize with correct default values from store and local state', () => {
            expect(spectator.component.$languageId()).toBe(1);
            expect(spectator.component.$pagePath()).toBe('/test/page/path');
            expect(spectator.component.$variantId()).toBe('DEFAULT');
            // Active tab initialized from local state, not store
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should update signal values when store values change', () => {
            mockUVEStore.pageLanguageId.set(5);
            mockUVEStore.pageURI.set('/updated/path');
            mockUVEStore.pageVariantId.set('test-variant');
            spectator.detectChanges();

            expect(spectator.component.$languageId()).toBe(5);
            expect(spectator.component.$pagePath()).toBe('/updated/path');
            expect(spectator.component.$variantId()).toBe('test-variant');
        });
    });

    describe('Local State Management', () => {
        it('should update local state when user clicks Widget tab', () => {
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);
        });

        it('should update local state with correct tab index when switching to Favorites tab', () => {
            // Switch to Favorites tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);
        });

        it('should update local state when switching back to Content tab', () => {
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            triggerTabChange(spectator, UVE_PALETTE_TABS.CONTENT_TYPES);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });
    });

    describe('Inputs Passed to dot-uve-palette-list', () => {
        it('should pass all required inputs to Content tab palette list', () => {
            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify all inputs are passed correctly - container/presentational pattern
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.CONTENT
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
            expect(ngMocks.input(paletteListDebugEl, 'variantId')).toBe('DEFAULT');
        });

        it('should pass all required inputs to Widget tab palette list', () => {
            // Switch to Widget tab via handleTabChange
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify all inputs are passed correctly - container/presentational pattern
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.WIDGET
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
            expect(ngMocks.input(paletteListDebugEl, 'variantId')).toBe('DEFAULT');
        });

        it('should pass all required inputs to Favorites tab palette list', () => {
            // Switch to Favorites tab via handleTabChange
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify all inputs are passed correctly - container/presentational pattern
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.FAVORITES
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
            expect(ngMocks.input(paletteListDebugEl, 'variantId')).toBe('DEFAULT');
        });
    });
});
