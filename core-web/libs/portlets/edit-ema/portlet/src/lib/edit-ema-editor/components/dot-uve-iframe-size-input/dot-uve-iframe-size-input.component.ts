import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DEFAULT_DEVICE } from '../../../shared/consts';
import { UVEStore } from '../../../store/dot-uve.store';

/**
 * Numeric width × height inputs that mirror the iframe dimensions and let the
 * user type a precise size. Editing a value while a device preset is active
 * switches the preview back to responsive mode.
 */
@Component({
    selector: 'dot-uve-iframe-size-input',
    standalone: true,
    templateUrl: './dot-uve-iframe-size-input.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex items-center bg-gray-100 rounded-full px-3 gap-1' }
})
export class DotUveIframeSizeInputComponent {
    protected readonly store = inject(UVEStore);

    readonly $width = this.store.viewIframeWidth;
    readonly $height = this.store.viewIframeHeight;

    onWidthChange(event: Event): void {
        const value = Number((event.target as HTMLInputElement).value);
        if (!Number.isFinite(value) || value <= 0) {
            return;
        }
        this.#ensureResponsiveMode();
        this.store.viewSetIframeSize({ width: value });
    }

    onHeightChange(event: Event): void {
        const value = Number((event.target as HTMLInputElement).value);
        if (!Number.isFinite(value) || value <= 0) {
            return;
        }
        this.#ensureResponsiveMode();
        this.store.viewSetIframeSize({ height: value });
    }

    #ensureResponsiveMode(): void {
        if (!this.store.$viewIsResponsiveMode()) {
            this.store.viewSetDevice(DEFAULT_DEVICE);
        }
    }
}
