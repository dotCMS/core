import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FieldProperty } from '../field-properties.model';
import { DATA_TYPE_PROPERTY_INFO } from '../../../service/data-type-property-info';

@Component({
    selector: 'dot-data-type-property',
    templateUrl: './data-type-property.component.html'
})
export class DataTypePropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;
    radioInputs: object;

    constructor() {}

    ngOnInit(): void {
        this.radioInputs = DATA_TYPE_PROPERTY_INFO[this.property.field.clazz];
    }

    isEmpty(obj) {
        for (const prop in obj) {
            if (obj.hasOwnProperty(prop)) {
                return false;
            }
        }

        return Object.keys(obj).length;
    }
}
