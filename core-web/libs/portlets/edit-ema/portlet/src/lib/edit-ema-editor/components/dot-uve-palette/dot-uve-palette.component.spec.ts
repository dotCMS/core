import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import { MockComponent, ngMocks } from 'ng-mocks';

import { Component } from '@angular/core';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUvePaletteComponent } from './dot-uve-palette.component';
import { DotUVEPaletteListTypes } from './models';

import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

/**
 * Helper function to trigger tab change event
 * Simulates the valueChange event that p-tabs (PrimeNG v21) emits when a tab is clicked.
 * Also updates the host's activeTab to reflect the change (simulating real parent behavior).
 */
function triggerTabChange(
    spectator: SpectatorHost<DotUvePaletteComponent, TestHostComponent>,
    index: number
): void {
    // PrimeNG v21 p-tabs valueChange event emits the value directly
    // Directly call the component's handleTabChange method to simulate the event
    (
        spectator.component as DotUvePaletteComponent & {
            handleTabChange: (value: number) => void;
        }
    ).handleTabChange(index);

    // In real usage, the parent component updates its activeTab property
    // when receiving the onTabChange event. Simulate that here.
    spectator.hostComponent.activeTab = index;
    spectator.fixture.detectChanges();
}

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-uve-palette
            [languageId]="languageId"
            [pagePath]="pagePath"
            [variantId]="variantId"
            [activeTab]="activeTab"
            (onTabChange)="onTabChange($event)" />
    `
})
class TestHostComponent {
    languageId = 1;
    pagePath = '/test/page/path';
    variantId = 'DEFAULT';
    activeTab = UVE_PALETTE_TABS.CONTENT_TYPES;
    onTabChange = jest.fn();
}

describe('DotUvePaletteComponent', () => {
    let spectator: SpectatorHost<DotUvePaletteComponent, TestHostComponent>;

    const createHost = createHostFactory({
        component: DotUvePaletteComponent,
        host: TestHostComponent,
        imports: [DotUvePaletteComponent, MockComponent(DotUvePaletteListComponent)]
    });

    beforeEach(() => {
        // Mock scrollIntoView for PrimeNG TabView
        Element.prototype.scrollIntoView = jest.fn();

        spectator = createHost(
            `<dot-uve-palette
                [languageId]="languageId"
                [pagePath]="pagePath"
                [variantId]="variantId"
                [activeTab]="activeTab"
                (onTabChange)="onTabChange($event)" />`
        );
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
            // Trigger the onChange event from p-tabs (PrimeNG v21)
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.WIDGETS);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should update rendering when switching to Favorites tab via onChange event', () => {
            // Trigger the onChange event from p-tabs (PrimeNG v21)
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should switch back to Content tab when activeTab changes', () => {
            // First go to Widget tab - use triggerTabChange to simulate user interaction
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

    describe('Signal Inputs', () => {
        it('should initialize with correct default values', () => {
            expect(spectator.component.$languageId()).toBe(1);
            expect(spectator.component.$pagePath()).toBe('/test/page/path');
            expect(spectator.component.$variantId()).toBe('DEFAULT');
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);
        });

        it('should update signal inputs when host inputs change', () => {
            // Set host properties directly to avoid ExpressionChangedAfterItHasBeenCheckedError
            spectator.hostComponent.languageId = 5;
            spectator.hostComponent.pagePath = '/updated/path';
            spectator.hostComponent.variantId = 'test-variant';
            spectator.hostComponent.activeTab = UVE_PALETTE_TABS.FAVORITES;
            // Use changeDetectorRef.detectChanges() to bypass AppRef's checkNoChanges
            spectator.fixture.changeDetectorRef.detectChanges();

            expect(spectator.component.$languageId()).toBe(5);
            expect(spectator.component.$pagePath()).toBe('/updated/path');
            expect(spectator.component.$variantId()).toBe('test-variant');
            expect(spectator.component.$activeTab()).toBe(UVE_PALETTE_TABS.FAVORITES);
        });
    });

    describe('onTabChange Output', () => {
        it('should emit onTabChange when user clicks tab', () => {
            // Trigger the onChange event from p-tabs (PrimeNG v21) by simulating user interaction
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            expect(spectator.hostComponent.onTabChange).toHaveBeenCalledWith(
                UVE_PALETTE_TABS.WIDGETS
            );
        });

        it('should emit correct tab index when switching tabs', () => {
            // Switch to Favorites tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            expect(spectator.hostComponent.onTabChange).toHaveBeenCalledWith(
                UVE_PALETTE_TABS.FAVORITES
            );
        });

        it('should emit when switching back to Content tab', () => {
            // First go to Widget tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);
            expect(spectator.hostComponent.onTabChange).toHaveBeenCalledWith(
                UVE_PALETTE_TABS.WIDGETS
            );

            // Clear the mock
            spectator.hostComponent.onTabChange.mockClear();

            // Then go back to Content tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.CONTENT_TYPES);
            expect(spectator.hostComponent.onTabChange).toHaveBeenCalledWith(
                UVE_PALETTE_TABS.CONTENT_TYPES
            );
        });
    });

    describe('Inputs Passed to dot-uve-palette-list', () => {
        it('should pass correct inputs to Content tab palette list', () => {
            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify inputs using ngMocks.input()
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.CONTENT
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
        });

        it('should pass correct inputs to Widget tab palette list', () => {
            // Switch to Widget tab using triggerTabChange
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify inputs
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.WIDGET
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
        });

        it('should pass correct inputs to Favorites tab palette list', () => {
            // Switch to Favorites tab using triggerTabChange
            triggerTabChange(spectator, UVE_PALETTE_TABS.FAVORITES);

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify inputs
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.FAVORITES
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(1);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/test/page/path');
        });

        it('should update languageId input when host input changes', () => {
            // Change the host input directly to avoid ExpressionChangedAfterItHasBeenCheckedError
            spectator.hostComponent.languageId = 5;
            // Use changeDetectorRef.detectChanges() to bypass AppRef's checkNoChanges
            spectator.fixture.changeDetectorRef.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify the updated input is passed down
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(5);
        });

        it('should update pagePath input when host input changes', () => {
            // Change the host input directly to avoid ExpressionChangedAfterItHasBeenCheckedError
            spectator.hostComponent.pagePath = '/new/path';
            // Use changeDetectorRef.detectChanges() to bypass AppRef's checkNoChanges
            spectator.fixture.changeDetectorRef.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify the updated input is passed down
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/new/path');
        });

        it('should pass updated inputs after switching tabs and changing host inputs', () => {
            // Change host inputs directly to avoid ExpressionChangedAfterItHasBeenCheckedError
            spectator.hostComponent.languageId = 3;
            spectator.hostComponent.pagePath = '/updated/path';
            // Use changeDetectorRef.detectChanges() to bypass AppRef's checkNoChanges
            spectator.fixture.changeDetectorRef.detectChanges();

            // Switch to Widgets tab
            triggerTabChange(spectator, UVE_PALETTE_TABS.WIDGETS);

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify all inputs are correct
            expect(ngMocks.input(paletteListDebugEl, 'listType')).toBe(
                DotUVEPaletteListTypes.WIDGET
            );
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(3);
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/updated/path');
        });
    });
});
