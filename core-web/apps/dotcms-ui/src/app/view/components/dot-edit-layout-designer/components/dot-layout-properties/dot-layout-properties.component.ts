import { UntypedFormGroup } from '@angular/forms';
import { Component, Input, ViewEncapsulation } from '@angular/core';

@Component({
    selector: 'dot-layout-properties',
    templateUrl: './dot-layout-properties.component.html',
    styleUrls: ['./dot-layout-properties.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class DotLayoutPropertiesComponent {
    @Input() group: UntypedFormGroup;
}
