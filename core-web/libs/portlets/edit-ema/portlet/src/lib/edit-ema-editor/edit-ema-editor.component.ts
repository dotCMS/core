import { Component, OnInit, inject } from '@angular/core';

import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { DotPaletteListStore } from '@dotcms/portlets/dot-ema/ui';
import { UVE_MODE } from '@dotcms/types';

import { DotUveBrowserToolbarComponent } from './components/dot-uve-browser-toolbar/dot-uve-browser-toolbar.component';
import { DotUveCanvasComponent } from './components/dot-uve-canvas/dot-uve-canvas.component';
import { DotUveContentEditorComponent } from './components/dot-uve-content-editor/dot-uve-content-editor.component';
import { DotUveEditPanelComponent } from './components/dot-uve-edit-panel/dot-uve-edit-panel.component';
import { DotUvePaletteComponent } from './components/dot-uve-palette/dot-uve-palette.component';
import { DotUveSeoResultsComponent } from './components/dot-uve-seo-results/dot-uve-seo-results.component';
import { DotUveToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';

import { UVEStore } from '../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../store/features/editor/models';

/**
 * The UVE editor screen. It places the regions and decides which show in edit and preview mode;
 * each region reads and writes the UVE store itself.
 */
@Component({
    selector: 'dot-edit-ema-editor',
    templateUrl: './edit-ema-editor.component.html',
    imports: [
        ConfirmDialogModule,
        DotUveToolbarComponent,
        DotUveSeoResultsComponent,
        DotUvePaletteComponent,
        DotUveBrowserToolbarComponent,
        DotUveCanvasComponent,
        DotUveEditPanelComponent,
        DotUveContentEditorComponent
    ],
    // Here, not on the palette: the lists keep their search while the palette hides and comes back.
    providers: [DotPaletteListStore],
    host: {
        // minmax(0, 1fr) lets the middle column shrink below the canvas's size when both side
        // panels are open; otherwise the canvas forces it wider than the viewport.
        class: 'grid h-full grid-cols-[min-content_minmax(0,1fr)_min-content] grid-rows-[min-content_1fr_min-content]'
    }
})
export class EditEmaEditorComponent implements OnInit {
    protected readonly uveStore = inject(UVEStore);

    protected readonly UVE_MODE = UVE_MODE;

    ngOnInit(): void {
        // The store outlives the editor, so only the editor knows when it is created again.
        this.uveStore.editorSetEditPanelTab(UVE_EDIT_PANEL_TABS.QUICK_EDIT);
    }
}
