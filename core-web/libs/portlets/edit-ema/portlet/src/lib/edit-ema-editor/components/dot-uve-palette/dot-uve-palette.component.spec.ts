import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, ngMocks } from 'ng-mocks';

import { computed, DebugElement, signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { TabView } from 'primeng/tabview';

import { DotPageLayoutService } from '@dotcms/data-access';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUvePaletteComponent } from './dot-uve-palette.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

/**
 * Helper function to trigger tab change event
 * Simulates the onChange event that p-tabView emits when a tab is clicked
 */
function triggerTabChange(
    spectator: Spectator<DotUvePaletteComponent>,
    index: number
): void {
    const tabViewDebugElement: DebugElement = spectator.debugElement.query(By.directive(TabView));
    const tabViewComponent: TabView = tabViewDebugElement?.componentInstance;

    if (tabViewComponent && tabViewComponent.onChange) {
        // Trigger the onChange event with the expected structure
        tabViewComponent.onChange.emit({ originalEvent: new Event('click'), index });
        spectator.detectChanges();
    }
}

/**
 * Mock UVEStore with default test values
 * Note: palette.currentTab and setPaletteTab removed - now using local signalState
 */
const mockActiveContentlet = signal(null);
const mockUVEStore = {
    $pageURI: signal('/test/page/path'),
    $languageId: signal(1),
    $variantId: signal('DEFAULT'),
    $isStyleEditorEnabled: signal(false),
    $canEditStyles: () => false,  // Computed property used by component
    $styleSchema: signal(undefined),
    // Phase 3: editor() method returns editor state with activeContentlet
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
    activeContentlet: mockActiveContentlet,
    // Normalized page response properties (replacing pageAPIResponse)
    page: signal(null),
    site: signal(null),
    viewAs: signal(null),
    template: signal(null),
    layout: signal(null),
    containers: signal(null)
};

describe('DotUvePaletteComponent', () => {
    let spectator: Spectator<DotUvePaletteComponent>;

    const createComponent = createComponentFactory({
        component: DotUvePaletteComponent,
        imports: [DotUvePaletteComponent, MockComponent(DotUvePaletteListComponent)],
        mocks: [DotPageLayoutService]
    });

    beforeEach(() => {
        // Mock scrollIntoView for PrimeNG TabView
        Element.prototype.scrollIntoView = jest.fn();

        // Reset mock store values
        mockUVEStore.$pageURI.set('/test/page/path');
        mockUVEStore.$languageId.set(1);
        mockUVEStore.$variantId.set('DEFAULT');
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
            // Trigger tab change via user interaction
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should update local state and rendering when switching to Favorites tab via onChange event', () => {
            // Trigger tab change via user interaction
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

    describe('Store Integration', () => {
        it('should initialize with correct default values from store and local state', () => {
            expect(spectator.component.$languageId()).toBe(1);
            expect(spectator.component.$pagePath()).toBe('/test/page/path');
            expect(spectator.component.$variantId()).toBe('DEFAULT');
            // Active tab initialized from local state, not store
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should update signal values when store values change', () => {
            mockUVEStore.$languageId.set(5);
            mockUVEStore.$pageURI.set('/updated/path');
            mockUVEStore.$variantId.set('test-variant');
            spectator.detectChanges();

            expect(spectator.component.$languageId()).toBe(5);
            expect(spectator.component.$pagePath()).toBe('/updated/path');
            expect(spectator.component.$variantId()).toBe('test-variant');
        });
    });

    describe('Local State Management', () => {
        it('should update local state when user clicks Widget tab', () => {
            // Trigger the onChange event from p-tabView by simulating user interaction
            triggerTabChange(spectator, 1);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);
        });

        it('should update local state with correct tab index when switching to Favorites tab', () => {
            // Switch to Favorites tab
            triggerTabChange(spectator, 2);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);
        });

        it('should update local state when switching back to Content tab', () => {
            // First go to Widget tab
            triggerTabChange(spectator, 1);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Then go back to Content tab
            triggerTabChange(spectator, 0);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should switch to STYLE_EDITOR tab when activeContentlet changes', () => {
            // Start on Widget tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // When activeContentlet is set in store (simulating user selecting a contentlet)
            mockUVEStore.activeContentlet.set({
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test',
                contentType: 'test'
            });
            spectator.detectChanges();

            // Should auto-switch to STYLE_EDITOR tab via effect()
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.STYLE_EDITOR);
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
            // Switch to Widget tab via user interaction
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
            // Switch to Favorites tab via user interaction
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
