import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * Header bar of the AssetPicker dialog: the title on the left and, on the right, the full-screen
 * toggle next to the close icon (the dialog's window controls).
 *
 * The picker renders its own header instead of using PrimeNG's `DynamicDialog` chrome
 * (`showHeader: false`), because the chrome offers nowhere to put the full-screen button. Same
 * arrangement as {@link DotImageEditorHeaderComponent}; the difference is that the title is an input
 * (it reads "Add File" or "Add Image" depending on the field) and that the toggle calls the store
 * directly — the picker store is plain `withMethods`, with none of the image editor's event
 * machinery.
 *
 * Resizing the dialog is the shell's job; this component only flips the flag it reads.
 */
@Component({
    selector: 'dot-asset-picker-header',
    templateUrl: './dot-asset-picker-header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    host: { class: 'block w-full' }
})
export class DotAssetPickerHeaderComponent {
    /** Picker state store, provided by the owning dialog component. */
    protected readonly store = inject(DotAssetPickerStore);

    /**
     * Dialog title, already translated by the host — it depends on the field that opened the
     * picker ("Add File" / "Add Image"), which this component has no way to know.
     * @type {string}
     * @alias title
     */
    readonly $title = input('', { alias: 'title' });

    /** Emitted when the user clicks the close (✕) button. */
    readonly close = output<void>();

    /** Material Symbol ligature for the full-screen toggle, by current state. */
    protected readonly $fullscreenIcon = computed(() =>
        this.store.isFullscreen() ? 'close_fullscreen' : 'open_in_full'
    );

    /** i18n key for the toggle's label, by current state. */
    protected readonly $fullscreenLabelKey = computed(() =>
        this.store.isFullscreen()
            ? 'dot.asset.picker.fullscreen.exit.aria'
            : 'dot.asset.picker.fullscreen.enter.aria'
    );
}
