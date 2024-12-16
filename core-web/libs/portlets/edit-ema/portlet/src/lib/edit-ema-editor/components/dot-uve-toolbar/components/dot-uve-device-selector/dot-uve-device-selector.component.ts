import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICES } from './const';

import { UVEStore } from '../../../../../store/dot-uve.store';
import { Orientation } from '../../../../../store/models';

@Component({
    selector: 'dot-uve-device-selector',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveDeviceSelectorComponent implements OnInit {
    defaultDevices = DEFAULT_DEVICES;

    #store = inject(UVEStore);

    readonly $currentDevice = this.#store.device;

    readonly $currentOrientation = this.#store.orientation;

    ngOnInit() {
        const deviceInode = this.#store.viewParams().device;

        // I HAVE TO CHANGE THIS TO LOOK IN A FULL LIST OF THE DEVICES
        // I WILL FETCH THE DEVICES IN THE UVE TOOLBAR
        const device = this.defaultDevices.find((d) => d.inode === deviceInode);

        if (device) {
            this.#store.setDevice(device);
        } else {
            // If the device is not from the devices list, we need to reset the device
            this.#store.patchViewParams({
                device: null,
                orientation: null
            });
        }
    }

    onDeviceSelect(device: DotDeviceListItem): void {
        const currentDevice = this.$currentDevice();

        if (currentDevice && currentDevice.inode === device.inode) {
            this.#store.patchViewParams({
                device: null
            });
        } else {
            this.#store.patchViewParams({
                device: device.inode
            });
        }
    }

    onOrientationChange(): void {
        this.#store.patchViewParams({
            orientation:
                this.$currentOrientation() === Orientation.LANDSCAPE
                    ? Orientation.PORTRAIT
                    : Orientation.LANDSCAPE
        });
    }
}
