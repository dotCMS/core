import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotDevice } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../shared/consts';
import { Orientation } from '../../../store/models';
import {
    DeviceSelectorChange,
    DeviceSelectorState
} from '../dot-uve-toolbar/components/dot-uve-device-selector/dot-uve-device-selector.component';

@Component({
    selector: 'dot-uve-device-controls',
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-device-controls.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex items-center bg-gray-100 rounded-full px-3 py-1 gap-1 overflow-hidden' }
})
export class DotUveDeviceControlsComponent {
    $state = input.required<DeviceSelectorState>({ alias: 'state' });

    stateChange = output<DeviceSelectorChange>();

    readonly Orientation = Orientation;
    readonly defaultDevices = DEFAULT_DEVICES;

    readonly $disableOrientation = computed(
        () =>
            this.$state().device?.inode === DEFAULT_DEVICE.inode ||
            this.$state().socialMedia !== null
    );

    readonly $currentDevice = computed(() => this.$state().device);
    readonly $currentOrientation = computed(() => this.$state().orientation);

    onDeviceSelect(device: DotDevice): void {
        const isSameDevice = this.$state().device?.inode === device.inode;

        this.stateChange.emit({
            type: 'device',
            device: isSameDevice ? DEFAULT_DEVICE : device
        });
    }

    onOrientationChange(): void {
        const newOrientation =
            this.$state().orientation === Orientation.LANDSCAPE
                ? Orientation.PORTRAIT
                : Orientation.LANDSCAPE;

        this.stateChange.emit({ type: 'orientation', orientation: newOrientation });
    }
}
