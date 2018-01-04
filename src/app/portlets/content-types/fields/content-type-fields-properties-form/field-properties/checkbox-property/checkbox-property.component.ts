import { Component } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { FormGroup } from '@angular/forms';

@Component({
    selector: 'checkbox-property',
    templateUrl: './checkbox-property.component.html',
})
export class CheckboxPropertyComponent extends BaseComponent {
    property: FieldProperty;
    group: FormGroup;

    private readonly map = {
        indexed: 'contenttypes.field.properties.system_indexed.label',
        listed: 'contenttypes.field.properties.listed.label',
        required: 'contenttypes.field.properties.required.label',
        searchable: 'contenttypes.field.properties.user_searchable.label',
        unique: 'contenttypes.field.properties.unique.label',
    };

    constructor(public dotMessageService: DotMessageService) {
        super(
            [
                'contenttypes.field.properties.required.label',
                'contenttypes.field.properties.user_searchable.label',
                'contenttypes.field.properties.system_indexed.label',
                'contenttypes.field.properties.listed.label',
                'contenttypes.field.properties.unique.label'
            ],
            dotMessageService
        );
    }

    setCheckboxLabel(field): string {
        return this.map[field] || field;
    }
}
