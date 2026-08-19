import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorViewEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Full-screen toggle for the image editor dialog, projected into the shared header's
 * `[dialogHeaderActions]` slot — the shell owns the title and the close button, and this is the one
 * control that is specific to the editor.
 *
 * It only dispatches {@link imageEditorViewEvents}; the root component performs the actual dialog
 * resize, reacting to `store.isFullscreen()`.
 */
@Component({
    selector: 'dot-image-editor-fullscreen-toggle',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-image-editor-fullscreen-toggle.component.html'
})
export class DotImageEditorFullscreenToggleComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);
    readonly #viewDispatch = injectDispatch(imageEditorViewEvents);

    /** Material Symbol ligature for the full-screen toggle, by current state. */
    protected readonly $fullscreenIcon = computed(() =>
        this.store.isFullscreen() ? 'close_fullscreen' : 'open_in_full'
    );

    /** i18n key for the toggle's label, by current state. */
    protected readonly $fullscreenLabelKey = computed(() =>
        this.store.isFullscreen()
            ? 'edit.content.image-editor.fullscreen.exit.aria'
            : 'edit.content.image-editor.fullscreen.enter.aria'
    );

    /** Toggles the editor dialog between its windowed size and full-screen. */
    protected toggleFullscreen(): void {
        this.#viewDispatch.fullscreenToggled();
    }
}
