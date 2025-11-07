import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import { MockComponent, ngMocks } from 'ng-mocks';

import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { TabView } from 'primeng/tabview';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUvePaletteComponent } from './dot-uve-palette.component';
import { DotUVEPaletteListTypes } from './models';

/**
 * Helper function to trigger tab change event
 * Simulates the activeIndexChange event that p-tabView emits when a tab is clicked
 */
function triggerTabChange(
    spectator: SpectatorHost<DotUvePaletteComponent, TestHostComponent>,
    index: number
): void {
    const tabViewDebugElement: DebugElement = spectator.debugElement.query(By.directive(TabView));
    const tabViewComponent: TabView = tabViewDebugElement?.componentInstance;

    if (tabViewComponent && tabViewComponent.activeIndexChange) {
        tabViewComponent.activeIndexChange.emit(index);
        spectator.detectChanges();
    }
}

@Component({
    selector: 'dot-test-host',
    standalone: false,
    template: `
        <dot-uve-palette [languageId]="languageId" [pagePath]="pagePath" [variantId]="variantId" />
    `
})
class TestHostComponent {
    languageId = 1;
    pagePath = '/test/page/path';
    variantId = 'DEFAULT';
}

describe('DotUvePaletteComponent', () => {
    let spectator: SpectatorHost<DotUvePaletteComponent, TestHostComponent>;

    const createHost = createHostFactory({
        component: DotUvePaletteComponent,
        host: TestHostComponent,
        imports: [DotUvePaletteComponent, MockComponent(DotUvePaletteListComponent)]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dot-uve-palette [languageId]="languageId" [pagePath]="pagePath" [variantId]="variantId" />`
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

        it('should have current index set to 0 by default', () => {
            expect(spectator.component.$currentIndex()).toBe(0);
        });
    });

    describe('Tab Navigation', () => {
        it('should only render one dot-uve-palette-list at a time', () => {
            const paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should update rendering when switching to Widget tab via activeIndexChange event', () => {
            // Trigger the activeIndexChange event from p-tabView
            triggerTabChange(spectator, 1);

            expect(spectator.component.$currentIndex()).toBe(1);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should update rendering when switching to Favorites tab via activeIndexChange event', () => {
            // Trigger the activeIndexChange event from p-tabView
            triggerTabChange(spectator, 2);

            expect(spectator.component.$currentIndex()).toBe(2);

            // Palette list should still be rendered
            const paletteList = spectator.query('dot-uve-palette-list');
            expect(paletteList).toBeTruthy();
        });

        it('should switch back to Content tab when activeIndexChange emits 0', () => {
            // First go to Widget tab
            triggerTabChange(spectator, 1);
            expect(spectator.component.$currentIndex()).toBe(1);

            // Then go back to Content tab
            triggerTabChange(spectator, 0);
            expect(spectator.component.$currentIndex()).toBe(0);
        });

        it('should always render exactly one dot-uve-palette-list regardless of active tab', () => {
            // Check initial state (Content tab)
            let paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Widget tab via event
            triggerTabChange(spectator, 1);
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);

            // Switch to Favorites tab via event
            triggerTabChange(spectator, 2);
            paletteLists = spectator.queryAll('dot-uve-palette-list');
            expect(paletteLists).toHaveLength(1);
        });

        it('should navigate through all tabs sequentially via events', () => {
            // Start at Content (0)
            expect(spectator.component.$currentIndex()).toBe(0);

            // Move to Widget (1)
            triggerTabChange(spectator, 1);
            expect(spectator.component.$currentIndex()).toBe(1);

            // Move to Favorites (2)
            triggerTabChange(spectator, 2);
            expect(spectator.component.$currentIndex()).toBe(2);

            // Move back to Content (0)
            triggerTabChange(spectator, 0);
            expect(spectator.component.$currentIndex()).toBe(0);
        });
    });

    describe('Signal Inputs', () => {
        it('should initialize with correct default values', () => {
            expect(spectator.component.$languageId()).toBe(1);
            expect(spectator.component.$pagePath()).toBe('/test/page/path');
            expect(spectator.component.$variantId()).toBe('DEFAULT');
        });

        it('should update signal inputs when host inputs change', () => {
            spectator.setHostInput({
                languageId: 5,
                pagePath: '/updated/path',
                variantId: 'test-variant'
            });
            spectator.detectChanges();

            expect(spectator.component.$languageId()).toBe(5);
            expect(spectator.component.$pagePath()).toBe('/updated/path');
            expect(spectator.component.$variantId()).toBe('test-variant');
        });
    });

    describe('Current Index Signal', () => {
        it('should initialize currentIndex to 0', () => {
            expect(spectator.component.$currentIndex()).toBe(0);
        });

        it('should be writable to simulate tab changes', () => {
            // This tests that the signal can be updated (simulating p-tabView activeIndexChange)
            spectator.component.$currentIndex.set(2);
            spectator.detectChanges();

            expect(spectator.component.$currentIndex()).toBe(2);
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
            // Switch to Widget tab
            triggerTabChange(spectator, 1);

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
            // Switch to Favorites tab
            triggerTabChange(spectator, 2);

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
            // Change the host input
            spectator.setHostInput({ languageId: 5 });
            spectator.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify the updated input is passed down
            expect(ngMocks.input(paletteListDebugEl, 'languageId')).toBe(5);
        });

        it('should update pagePath input when host input changes', () => {
            // Change the host input
            spectator.setHostInput({ pagePath: '/new/path' });
            spectator.detectChanges();

            // Find the mocked component DebugElement
            const paletteListDebugEl = ngMocks.find(DotUvePaletteListComponent);

            // Verify the updated input is passed down
            expect(ngMocks.input(paletteListDebugEl, 'pagePath')).toBe('/new/path');
        });

        it('should pass updated inputs after switching tabs and changing host inputs', () => {
            // Change host inputs
            spectator.setHostInput({ languageId: 3, pagePath: '/updated/path' });
            spectator.detectChanges();

            // Switch to Widget tab
            triggerTabChange(spectator, 1);

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
