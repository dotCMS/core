import { ChangeDetectionStrategy, Component, ViewEncapsulation, input } from '@angular/core';

import { DotRelativeDatePipe } from '@dotcms/ui';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-custom-time',
    styleUrls: ['./dot-custom-time.component.scss'],
    templateUrl: 'dot-custom-time.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotRelativeDatePipe]
})
export class CustomTimeComponent {
    readonly time = input<string>();
}
