import { Component } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-name-property',
    templateUrl: './name-property.component.html',
    standalone: false
})
export class NamePropertyComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
}
