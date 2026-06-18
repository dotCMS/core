import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Footer action bar of the image editor dialog. Reads readiness from the
 * {@link ImageEditorStore} (`isBusy`, `canSave`, `saveStatus`) and dispatches the
 * download / save / save-as lifecycle events. Cancel is surfaced as an output so
 * the owning dialog component controls closing.
 */
@Component({
    selector: 'dot-image-editor-footer',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, SplitButtonModule, DotMessagePipe],
    templateUrl: './dot-image-editor-footer.component.html'
})
export class DotImageEditorFooterComponent {
    readonly #dotMessageService = inject(DotMessageService);

    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Lifecycle event dispatcher for download/save/save-as actions. */
    protected readonly dispatch = injectDispatch(imageEditorLifecycleEvents);

    /** Emitted when the user clicks Cancel; the dialog owner closes the editor. */
    cancel = output<void>();

    /** Label shown on the primary save button, reflecting the in-flight save status. */
    protected readonly saveLabel = computed(() =>
        this.store.saveStatus() === 'saving'
            ? this.#dotMessageService.get('edit.content.image-editor.footer.saving')
            : this.#dotMessageService.get('edit.content.image-editor.footer.save')
    );

    /** Spinner icon shown on the save button only while a save is in flight. */
    protected readonly saveIcon = computed(() =>
        this.store.saveStatus() === 'saving' ? 'pi pi-spin pi-spinner' : ''
    );

    /** Secondary save actions; currently only "Save as…". */
    protected readonly saveMenuItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('edit.content.image-editor.footer.save-as'),
            command: () => this.onSaveAs()
        }
    ];

    /** Dispatches a download of the current preview. */
    protected onDownload(): void {
        this.dispatch.downloadRequested();
    }

    /** Dispatches a save of the current edits. */
    protected onSave(): void {
        this.dispatch.saveRequested();
    }

    /** Dispatches a save-as using the current asset file name. */
    protected onSaveAs(): void {
        this.dispatch.saveAsRequested({ fileName: this.store.assetContext().fileName });
    }
}
