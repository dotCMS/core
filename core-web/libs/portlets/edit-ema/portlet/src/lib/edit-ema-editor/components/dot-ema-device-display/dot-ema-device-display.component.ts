import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotDevice } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-ema-device-display',
    standalone: true,
    imports: [CommonModule, ButtonModule],
    templateUrl: './dot-ema-device-display.component.html',
    styleUrls: ['./dot-ema-device-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaDeviceDisplayComponent {
    @Input() currentDevice: DotDevice & { icon?: string };
    @Output() resetDevice = new EventEmitter<undefined>();

    /**
     * Reset the device
     *
     * @memberof DotEmaDeviceDisplayComponent
     */
    handleDeviceReset(): void {
        this.resetDevice.emit();
    }
}
