import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Component, computed, signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { UVE_MODE } from '@dotcms/types';

import { DotUveBrowserToolbarComponent } from './components/dot-uve-browser-toolbar/dot-uve-browser-toolbar.component';
import { DotUveCanvasComponent } from './components/dot-uve-canvas/dot-uve-canvas.component';
import { DotUveContentEditorComponent } from './components/dot-uve-content-editor/dot-uve-content-editor.component';
import { DotUveEditPanelComponent } from './components/dot-uve-edit-panel/dot-uve-edit-panel.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotUveSeoResultsComponent } from './components/dot-uve-seo-results/dot-uve-seo-results.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';
import { EditEmaEditorComponent } from './edit-ema-editor.component';

import { UVEStore } from '../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../store/features/editor/models';

// Each region has its own spec; here it only needs to be placed.

@Component({ selector: 'dot-uve-toolbar', template: '' })
class DotUveToolbarStubComponent {}

@Component({ selector: 'dot-uve-palette', template: '' })
class DotUvePaletteStubComponent {}

@Component({ selector: 'dot-uve-browser-toolbar', template: '' })
class DotUveBrowserToolbarStubComponent {}

@Component({ selector: 'dot-uve-canvas', template: '' })
class DotUveCanvasStubComponent {}

@Component({ selector: 'dot-uve-seo-results', template: '' })
class DotUveSeoResultsStubComponent {}

@Component({ selector: 'dot-uve-edit-panel', template: '' })
class DotUveEditPanelStubComponent {}

@Component({ selector: 'dot-uve-content-editor', template: '' })
class DotUveContentEditorStubComponent {}

/**
 * The part of `UVEStore` the editor reads and writes. `editorCanEditContent` is derived as the
 * real store derives it, so switching the mode hides the palette the way it does on the page. The
 * tab setter writes back, as the real store's does.
 */
function createUVEStoreMock() {
    const viewMode = signal(UVE_MODE.EDIT);
    const editorHasAccessToEditMode = signal(true);
    const editorEditPanelTab = signal(UVE_EDIT_PANEL_TABS.QUICK_EDIT);

    return {
        viewMode,
        editorHasAccessToEditMode,
        editorCanEditContent: computed(
            () => editorHasAccessToEditMode() && viewMode() === UVE_MODE.EDIT
        ),
        viewSocialMedia: signal<string | null>(null),
        editorEditPanelTab,
        editorSetEditPanelTab: vi.fn((tab: UVE_EDIT_PANEL_TABS) => editorEditPanelTab.set(tab))
    };
}

/** The regions that show whatever the mode. */
const REGIONS_IN_EVERY_MODE = [
    ['the toolbar', 'uve-toolbar'],
    ['the browser toolbar', 'uve-browser-toolbar'],
    ['the canvas', 'uve-canvas'],
    ['the SEO results', 'uve-seo-results'],
    ['the content editor', 'uve-content-editor'],
    ['the confirm dialog', 'confirm-dialog']
];

describe('EditEmaEditorComponent', () => {
    let spectator: Spectator<EditEmaEditorComponent>;
    let store: ReturnType<typeof createUVEStoreMock>;

    const createComponent = createComponentFactory({
        component: EditEmaEditorComponent,
        overrideComponents: [
            [
                EditEmaEditorComponent,
                {
                    remove: {
                        imports: [
                            DotUveToolbarComponent,
                            DotUvePaletteComponent,
                            DotUveBrowserToolbarComponent,
                            DotUveCanvasComponent,
                            DotUveSeoResultsComponent,
                            DotUveEditPanelComponent,
                            DotUveContentEditorComponent
                        ]
                    },
                    add: {
                        imports: [
                            DotUveToolbarStubComponent,
                            DotUvePaletteStubComponent,
                            DotUveBrowserToolbarStubComponent,
                            DotUveCanvasStubComponent,
                            DotUveSeoResultsStubComponent,
                            DotUveEditPanelStubComponent,
                            DotUveContentEditorStubComponent
                        ]
                    }
                }
            ]
        ],
        // The regions ask through the confirm dialog, so it needs the service; the editor itself
        // reads only the store.
        providers: [ConfirmationService, { provide: UVEStore, useFactory: () => store }],
        detectChanges: false
    });

    beforeEach(() => {
        store = createUVEStoreMock();
        spectator = createComponent();
    });

    describe('in edit mode', () => {
        beforeEach(() => spectator.detectChanges());

        it.each([...REGIONS_IN_EVERY_MODE, ['the edit panel', 'uve-edit-panel']])(
            'should show %s',
            (_region, testId) => {
                expect(spectator.query(byTestId(testId))).toBeTruthy();
            }
        );

        it('should show the palette when the user can edit the page', () => {
            expect(spectator.query(byTestId('palette'))).toBeTruthy();
        });

        it('should hide the palette when the user cannot edit the page', () => {
            store.editorHasAccessToEditMode.set(false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('palette'))).toBeNull();
        });

        it('should keep the edit panel when the user cannot edit the page', () => {
            store.editorHasAccessToEditMode.set(false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('uve-edit-panel'))).toBeTruthy();
        });
    });

    describe('in preview mode', () => {
        beforeEach(() => {
            store.viewMode.set(UVE_MODE.PREVIEW);
            spectator.detectChanges();
        });

        it.each(REGIONS_IN_EVERY_MODE)('should show %s', (_region, testId) => {
            expect(spectator.query(byTestId(testId))).toBeTruthy();
        });

        it('should hide the palette', () => {
            expect(spectator.query(byTestId('palette'))).toBeNull();
        });

        it('should hide the edit panel', () => {
            expect(spectator.query(byTestId('uve-edit-panel'))).toBeNull();
        });
    });

    describe('social-media preview', () => {
        it('should show the browser toolbar and the canvas while no social-media preview is chosen', () => {
            spectator.detectChanges();

            const browser = spectator.query(byTestId('editor-content'));

            expect(browser).toHaveClass('grid');
            expect(browser).not.toHaveClass('hidden');
        });

        it('should hide the browser toolbar and the canvas while a social-media preview is chosen', () => {
            store.viewSocialMedia.set('facebook');
            spectator.detectChanges();

            const browser = spectator.query(byTestId('editor-content'));

            // Hidden rather than removed, so the page in the canvas is not loaded again on return.
            expect(browser).toHaveClass('hidden');
            expect(browser).not.toHaveClass('grid');
            expect(spectator.query(byTestId('uve-canvas'))).toBeTruthy();
        });
    });

    describe('when the editor is created', () => {
        it('should open the edit panel on the quick-edit tab', () => {
            // The store outlives the editor: coming back from Layout, Rules or Experiments, it
            // still holds the tab the panel was last on.
            store.editorEditPanelTab.set(UVE_EDIT_PANEL_TABS.STYLE_EDITOR);

            spectator.detectChanges();

            expect(store.editorEditPanelTab()).toBe(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
        });
    });
});
