import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotDevice } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-ema-device-display',
    standalone: true,
    imports: [NgIf],
    templateUrl: './dot-ema-device-display.component.html',
    styleUrls: ['./dot-ema-device-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaDeviceDisplayComponent {
    @Input() currentDevice: DotDevice & { icon?: string };
}
