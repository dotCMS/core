import { Component, computed, inject } from '@angular/core';

import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_EDIT_PANEL_TABS } from '../../../store/features/editor/models';
import { DotUveContentletQuickEditComponent } from '../dot-uve-contentlet-quick-edit/dot-uve-contentlet-quick-edit.component';
import { DotUveStyleEditorEmptyStateComponent } from '../dot-uve-palette/components/dot-uve-style-editor-empty-state/dot-uve-style-editor-empty-state.component';
import { DotUveStyleEditorFormComponent } from '../dot-uve-palette/components/dot-uve-style-editor-form/dot-uve-style-editor-form.component';

/**
 * The edit panel on the right of the editor: quick edit and, when the page allows style editing,
 * the style editor for the selected contentlet, or the no-permission state when the user cannot
 * edit it. Reads and writes the UVE store directly; the contentlet tools open it on a tab through
 * the store.
 */
@Component({
    selector: 'dot-uve-edit-panel',
    imports: [
        TabsModule,
        TooltipModule,
        DotMessagePipe,
        DotUveContentletQuickEditComponent,
        DotUveStyleEditorFormComponent,
        DotUveStyleEditorEmptyStateComponent
    ],
    templateUrl: './dot-uve-edit-panel.component.html',
    host: {
        class: 'block overflow-hidden border-gray-200 transition-[width] duration-150 ease-[ease-in-out]',
        // Narrows to nothing while the panel is closed, as its body slides out.
        '[class.w-[clamp(360px,20vw,500px)]]': 'store.editorEditPanelOpen()',
        '[class.border-l]': 'store.editorEditPanelOpen()',
        '[class.w-0]': '!store.editorEditPanelOpen()'
    }
})
export class DotUveEditPanelComponent {
    protected readonly store = inject(UVEStore);

    protected readonly UVE_EDIT_PANEL_TABS = UVE_EDIT_PANEL_TABS;

    /** The selected contentlet's content type, which the style editor's empty state links to. */
    protected readonly $styleSchemaContentTypeVar = computed(
        () => this.store.editorSelected()?.payload?.contentlet?.contentType ?? ''
    );

    /**
     * Shows the tab the user clicked.
     *
     * @param value - The clicked tab's value, as PrimeNG's tabs emit it
     */
    protected handleTabChange(value: string | number | undefined): void {
        if (value === undefined) {
            return;
        }

        const tab = Number(value);

        if (isNaN(tab)) {
            return;
        }

        this.store.editorSetEditPanelTab(tab);
    }
}
