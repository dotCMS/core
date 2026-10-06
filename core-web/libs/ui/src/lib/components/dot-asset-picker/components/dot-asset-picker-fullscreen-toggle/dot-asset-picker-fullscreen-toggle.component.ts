import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

/**
 * Full-screen toggle for the AssetPicker dialog, projected into the shared header's
 * `[dialogHeaderActions]` slot — the shell owns the title and the close button, and this is the one
 * control that is specific to the picker.
 *
 * It only flips the flag it reads; resizing the dialog is the picker component's job, which reacts
 * to `store.isFullscreen()`. The picker store is plain `withMethods`, so the toggle calls it
 * directly, with none of the image editor's event machinery.
 */
@Component({
    selector: 'dot-asset-picker-fullscreen-toggle',
    templateUrl: './dot-asset-picker-fullscreen-toggle.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, TooltipModule, DotMessagePipe]
})
export class DotAssetPickerFullscreenToggleComponent {
    /** Picker state store, provided by the owning dialog component. */
    protected readonly store = inject(DotAssetPickerStore);

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
