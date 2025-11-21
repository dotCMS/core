import { Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

@Component({
    selector: 'dot-field-helper',
    templateUrl: './dot-field-helper.component.html',
    styleUrls: ['./dot-field-helper.component.scss'],
    imports: [ButtonModule, OverlayPanelModule]
})
export class DotFieldHelperComponent {
    @Input() message: string;
}
