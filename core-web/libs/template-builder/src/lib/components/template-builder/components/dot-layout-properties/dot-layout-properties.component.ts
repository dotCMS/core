import { Component, Input, ViewEncapsulation } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-layout-properties',
    templateUrl: './dot-layout-properties.component.html',
    styleUrls: ['./dot-layout-properties.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DotLayoutPropertiesComponent {
    @Input() group: UntypedFormGroup;
}
