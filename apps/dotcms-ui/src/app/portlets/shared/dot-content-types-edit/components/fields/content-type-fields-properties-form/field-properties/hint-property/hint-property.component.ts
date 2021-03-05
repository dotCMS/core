import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-hint-property',
    templateUrl: './hint-property.component.html'
})
export class HintPropertyComponent {
    property: FieldProperty;
    group: FormGroup;

    constructor() {}
}
