import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { DATA_TYPE_PROPERTY_INFO } from '../../../service/data-type-property-info';

@Component({
    selector: 'data-type-property',
    templateUrl: './data-type-property.component.html',
})
export class DataTypePropertyComponent extends BaseComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;
    radioInputs: object;

    constructor(public messageService: DotMessageService) {
        super(
            [
                'contenttypes.field.properties.data_type.label',
                'contenttypes.field.properties.data_type.values.binary',
                'contenttypes.field.properties.data_type.values.text',
                'contenttypes.field.properties.data_type.values.boolean',
                'contenttypes.field.properties.data_type.values.date',
                'contenttypes.field.properties.data_type.values.decimal',
                'contenttypes.field.properties.data_type.values.number',
                'contenttypes.field.properties.data_type.values.large_text',
                'contenttypes.field.properties.data_type.values.system'
            ],
            messageService
        );
    }

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
