import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, ngMocks } from 'ng-mocks';

import { DebugElement, signal } from '@angular/core';
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
 */
const mockUVEStore = {
    $pageURI: signal('/test/page/path'),
    $languageId: signal(1),
    $variantId: signal('DEFAULT'),
    $isStyleEditorEnabled: signal(false),
    $styleSchema: signal(undefined),
    palette: {
        currentTab: signal(UVE_PALETTE_TABS.CONTENT_TYPES)
    },
    setPaletteTab: jest.fn()
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
        mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.CONTENT_TYPES);
        mockUVEStore.$isStyleEditorEnabled.set(false);
        mockUVEStore.$styleSchema.set(undefined);
        mockUVEStore.setPaletteTab.mockClear();

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

    describe('Tab Navigation', () => {
        it('should only render one dot-uve-palette-list at a time', () => {
            const paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should update rendering when switching to Widget tab via onChange event', () => {
            // Update the store's activeTab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.WIDGETS);
            spectator.detectChanges();

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should update rendering when switching to Favorites tab via onChange event', () => {
            // Update the store's activeTab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.FAVORITES);
            spectator.detectChanges();

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should switch back to Content tab when activeTab changes to 0', () => {
            // First go to Widget tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.WIDGETS);
            spectator.detectChanges();
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Then go back to Content tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.CONTENT_TYPES);
            spectator.detectChanges();
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should always render exactly one dot-uve-palette-list regardless of active tab', () => {
            // Check initial state (Content tab)
            let paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Widget tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.WIDGETS);
            spectator.detectChanges();
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Favorites tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.FAVORITES);
            spectator.detectChanges();
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should navigate through all tabs sequentially', () => {
            // Start at Content (0)
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);

            // Move to Widget (1)
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.WIDGETS);
            spectator.detectChanges();
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Move to Favorites (2)
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.FAVORITES);
            spectator.detectChanges();
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);

            // Move back to Content (0)
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.CONTENT_TYPES);
            spectator.detectChanges();
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });
    });

    describe('Store Integration', () => {
        it('should initialize with correct default values from store', () => {
            expect(spectator.component.$languageId()).toBe(1);
            expect(spectator.component.$pagePath()).toBe('/test/page/path');
            expect(spectator.component.$variantId()).toBe('DEFAULT');
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should update signal values when store values change', () => {
            mockUVEStore.$languageId.set(5);
            mockUVEStore.$pageURI.set('/updated/path');
            mockUVEStore.$variantId.set('test-variant');
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.FAVORITES);
            spectator.detectChanges();

            expect(spectator.component.$languageId()).toBe(5);
            expect(spectator.component.$pagePath()).toBe('/updated/path');
            expect(spectator.component.$variantId()).toBe('test-variant');
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);
        });
    });

    describe('Tab Change', () => {
        it('should call store.setPaletteTab when user clicks tab', () => {
            // Trigger the onChange event from p-tabView by simulating user interaction
            triggerTabChange(spectator, 1);

            expect(mockUVEStore.setPaletteTab).toHaveBeenCalledWith(1);
        });

        it('should call store.setPaletteTab with correct tab index when switching tabs', () => {
            // Switch to Favorites tab
            triggerTabChange(spectator, 2);

            expect(mockUVEStore.setPaletteTab).toHaveBeenCalledWith(2);
        });

        it('should call store.setPaletteTab when switching back to Content tab', () => {
            // First go to Widget tab
            triggerTabChange(spectator, 1);
            expect(mockUVEStore.setPaletteTab).toHaveBeenCalledWith(1);

            // Clear the mock
            mockUVEStore.setPaletteTab.mockClear();

            // Then go back to Content tab
            triggerTabChange(spectator, 0);
            expect(mockUVEStore.setPaletteTab).toHaveBeenCalledWith(0);
        });
    });

    describe('Inputs Passed to dot-uve-palette-list', () => {
        it('should pass only listType input to Content tab palette list', () => {
            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify only listType is passed as input
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.CONTENT
            );
            // languageId and pagePath are no longer inputs - they're read from store
        });

        it('should pass only listType input to Widget tab palette list', () => {
            // Switch to Widget tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.WIDGETS);
            spectator.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify only listType is passed as input
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.WIDGET
            );
        });

        it('should pass only listType input to Favorites tab palette list', () => {
            // Switch to Favorites tab
            mockUVEStore.palette.currentTab.set(UVE_PALETTE_TABS.FAVORITES);
            spectator.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify only listType is passed as input
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.FAVORITES
            );
        });
    });
});
