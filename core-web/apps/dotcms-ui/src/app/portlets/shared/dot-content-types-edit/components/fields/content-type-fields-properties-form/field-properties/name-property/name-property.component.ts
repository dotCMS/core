import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-name-property',
    styleUrls: ['./name-property.component.scss'],
    templateUrl: './name-property.component.html'
})
export class NamePropertyComponent {
    property: FieldProperty;
    group: FormGroup;
}
