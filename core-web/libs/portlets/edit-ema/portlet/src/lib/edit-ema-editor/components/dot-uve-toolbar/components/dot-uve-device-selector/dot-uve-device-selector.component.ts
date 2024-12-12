import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICES } from './const';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-device-selector',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveDeviceSelectorComponent {
    defaultDevices = DEFAULT_DEVICES;

    #store = inject(UVEStore);

    readonly $currentDevice = this.#store.device;

    onDeviceSelect(device: DotDeviceListItem): void {
        // console.log('Device selected:', device);

        // console.log(this.#store);

        const currentDevice = this.$currentDevice();

        if (currentDevice && currentDevice.inode === device.inode) {
            this.#store.setDevice(null);
        } else {
            this.#store.setDevice(device);
        }
    }
}
