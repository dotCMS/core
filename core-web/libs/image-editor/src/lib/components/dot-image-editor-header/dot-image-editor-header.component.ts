import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorViewEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Header bar of the image editor dialog. Renders the editor title on the left and,
 * on the right, the full-screen toggle next to a close icon button (grouped as the
 * dialog's window controls). Close emits {@link DotImageEditorHeaderComponent.close};
 * the full-screen toggle dispatches {@link imageEditorViewEvents} and the root
 * component performs the actual dialog resize, reacting to `store.isFullscreen()`.
 */
@Component({
    selector: 'dot-image-editor-header',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-image-editor-header.component.html'
})
export class DotImageEditorHeaderComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);
    readonly #viewDispatch = injectDispatch(imageEditorViewEvents);

    /** Emitted when the user clicks the close (✕) button. */
    close = output<void>();

    /** Toggles the editor dialog between its windowed size and full-screen. */
    protected toggleFullscreen(): void {
        this.#viewDispatch.fullscreenToggled();
    }
}
