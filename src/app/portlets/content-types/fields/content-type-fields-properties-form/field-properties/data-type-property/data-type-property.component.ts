import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DotMessageService } from '@services/dot-messages-service';
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

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.radioInputs = DATA_TYPE_PROPERTY_INFO[this.property.field.clazz];

        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.data_type.label',
                'contenttypes.field.properties.data_type.values.binary',
                'contenttypes.field.properties.data_type.values.text',
                'contenttypes.field.properties.data_type.values.boolean',
                'contenttypes.field.properties.data_type.values.date',
                'contenttypes.field.properties.data_type.values.decimal',
                'contenttypes.field.properties.data_type.values.number',
                'contenttypes.field.properties.data_type.values.large_text',
                'contenttypes.field.properties.data_type.values.system'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });
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
