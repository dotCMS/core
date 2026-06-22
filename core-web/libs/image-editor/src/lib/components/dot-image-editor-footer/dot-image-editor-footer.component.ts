import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Footer action bar of the image editor dialog. Reads readiness from the
 * {@link ImageEditorStore} (`isBusy`) and dispatches the download lifecycle event.
 * Cancel is surfaced as an output so the owning dialog component controls closing.
 * (Saving the edited image back to the field is handled in a separate issue.)
 */
@Component({
    selector: 'dot-image-editor-footer',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-image-editor-footer.component.html'
})
export class DotImageEditorFooterComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Lifecycle event dispatcher for the download action. */
    protected readonly dispatch = injectDispatch(imageEditorLifecycleEvents);

    /** Emitted when the user clicks Cancel; the dialog owner closes the editor. */
    cancel = output<void>();

    /** Dispatches a download of the current preview. */
    protected onDownload(): void {
        this.dispatch.downloadRequested();
    }
}
