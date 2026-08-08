import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorHistoryEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

/**
 * Applied-edits panel. Lists the edits captured up to the current history head
 * from the {@link ImageEditorStore} `appliedEdits` computed, lets the user remove
 * an individual entry or reset every edit, and shows an empty state when no edits
 * have been applied. User actions dispatch the matching
 * {@link imageEditorHistoryEvents}; the store owns the resulting state changes.
 */
@Component({
    selector: 'dot-image-editor-history-panel',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-image-editor-history-panel.component.html',
    styleUrl: './dot-image-editor-history-panel.component.scss'
})
export class DotImageEditorHistoryPanelComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** History event dispatcher for removing and resetting applied edits. */
    protected readonly dispatch = injectDispatch(imageEditorHistoryEvents);

    /** Dispatches removal of a single applied edit by its id. */
    protected onRemove(id: string): void {
        this.dispatch.editRemoved({ id });
    }

    /** Dispatches a reset that clears every applied edit. */
    protected onReset(): void {
        this.dispatch.resetRequested();
    }
}
