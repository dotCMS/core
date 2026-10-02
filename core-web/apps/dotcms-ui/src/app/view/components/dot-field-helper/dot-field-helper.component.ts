import { Component, ChangeDetectionStrategy, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PopoverModule } from 'primeng/popover';

@Component({
    selector: 'dot-field-helper',
    templateUrl: './dot-field-helper.component.html',
    styleUrls: ['./dot-field-helper.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ButtonModule, PopoverModule]
})
export class DotFieldHelperComponent {
    readonly message = input<string>();
}
