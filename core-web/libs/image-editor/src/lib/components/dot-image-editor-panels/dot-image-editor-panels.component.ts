import { ChangeDetectionStrategy, Component, effect, signal } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorAdjustPanelComponent } from './dot-image-editor-adjust-panel/dot-image-editor-adjust-panel.component';
import { DotImageEditorFileInfoPanelComponent } from './dot-image-editor-fileinfo-panel/dot-image-editor-fileinfo-panel.component';
import { DotImageEditorHistoryPanelComponent } from './dot-image-editor-history-panel/dot-image-editor-history-panel.component';
import { DotImageEditorTransformPanelComponent } from './dot-image-editor-transform-panel/dot-image-editor-transform-panel.component';

import { getStoredPanelState, savePanelState } from '../../utils/panel-state.storage';

/**
 * Side panel container of the image editor dialog. Stacks the adjust, transform,
 * file info and applied-edits sub-panels in a multi-expand PrimeNG accordion so
 * the user can keep several sections open at once. Each sub-panel is fully
 * self-contained and talks to the {@link ImageEditorStore} on its own; this
 * container only owns the accordion layout and section headers.
 *
 * Which sections are open is persisted to `localStorage` (same approach as the
 * Edit Content sidebar): the panel starts from the stored set — empty, so every
 * section is collapsed on first use — and an effect writes the set back whenever
 * it changes, so the user's layout is remembered the way they left it.
 */
@Component({
    selector: 'dot-image-editor-panels',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-image-editor-panels.component.html',
    styleUrl: './dot-image-editor-panels.component.scss',
    imports: [
        AccordionModule,
        DotMessagePipe,
        DotImageEditorAdjustPanelComponent,
        DotImageEditorTransformPanelComponent,
        DotImageEditorFileInfoPanelComponent,
        DotImageEditorHistoryPanelComponent
    ]
})
export class DotImageEditorPanelsComponent {
    /** Values of the currently open accordion sections; seeded from storage. */
    protected readonly openPanels = signal<string[]>(getStoredPanelState());

    constructor() {
        // Persist the open sections whenever they change, mirroring the Edit
        // Content sidebar's save-on-change effect.
        effect(() => savePanelState(this.openPanels()));
    }

    /** Updates the open-section set from the accordion (also triggers the save effect). */
    protected onOpenPanelsChange(value: string[]): void {
        this.openPanels.set(value ?? []);
    }
}
