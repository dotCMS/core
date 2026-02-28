import { Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PopoverModule } from 'primeng/popover';

@Component({
    selector: 'dot-field-helper',
    templateUrl: './dot-field-helper.component.html',
    styleUrls: ['./dot-field-helper.component.scss'],
    imports: [ButtonModule, PopoverModule]
})
export class DotFieldHelperComponent {
    @Input() message: string;
}
