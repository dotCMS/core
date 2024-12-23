import { DeepSignal } from '@ngrx/signals';

import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotDevice, DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICES } from '../../../../../shared/consts';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { Orientation } from '../../../../../store/models';

@Component({
    selector: 'dot-uve-device-selector',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass, MenuModule],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveDeviceSelectorComponent implements OnInit {
    defaultDevices = DEFAULT_DEVICES;

    #store = inject(UVEStore);
    #messageService = inject(DotMessageService);

    $devices = input<DotDeviceListItem[]>([], {
        alias: 'devices'
    });

    readonly $currentDevice = this.#store.device as DeepSignal<DotDeviceListItem>;

    readonly $disableOrientation = computed(() => this.#store.viewParams().device === 'default');

    readonly $currentOrientation = this.#store.orientation;

    readonly $menuItems = computed(() => {
        const extraDevices = this.$devices().filter((device) => !device._isDefault);

        const customDevices = {
            label: 'Custom Devices',
            items: extraDevices.map((device) => ({
                label: `${this.#messageService.get(device.name)} (${device.cssWidth}x${device.cssHeight})`,
                command: () => this.onDeviceSelect(device),
                styleClass: this.$currentDevice().inode === device.inode ? 'active' : ''
            }))
        };

        return extraDevices.length ? [customDevices] : [];
    });

    readonly $isADefaultDeviceActive = computed(() => {
        return !!this.$currentDevice()._isDefault;
    });

    ngOnInit() {
        const deviceInode = this.#store.viewParams().device;

        const device = this.$devices().find((d) => d.inode === deviceInode);

        if (device) {
            this.#store.setDevice(device);
        } else {
            // If the device is not from the devices list, we need to reset the device
            this.#store.patchViewParams({
                device: 'default',
                orientation: null
            });
        }
    }

    onDeviceSelect(device: DotDevice): void {
        const currentDevice = this.$currentDevice();

        // console.log(this.$devices());

        if (currentDevice && currentDevice.inode === device.inode) {
            this.#store.patchViewParams({
                device: 'default'
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
