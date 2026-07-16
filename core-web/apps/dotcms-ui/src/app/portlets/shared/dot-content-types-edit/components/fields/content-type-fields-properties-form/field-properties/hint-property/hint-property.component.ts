import { Component } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-hint-property',
    templateUrl: './hint-property.component.html',
    standalone: false
})
export class HintPropertyComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
}
