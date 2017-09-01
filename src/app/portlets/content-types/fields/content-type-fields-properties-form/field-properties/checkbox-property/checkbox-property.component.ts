import { Component, Input, SimpleChanges } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { MessageService } from '../../../../../../api/services/messages-service';
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

    constructor(public messageService: MessageService) {
        super(
            [
                'contenttypes.field.properties.required.label',
                'contenttypes.field.properties.user_searchable.label',
                'contenttypes.field.properties.system_indexed.label',
                'contenttypes.field.properties.listed.label',
                'contenttypes.field.properties.unique.label'
            ],
            messageService
        );
    }

    setCheckboxLabel(field): string {
        return this.map[field] || field;
    }
}
