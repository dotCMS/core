import { Component } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'dot-checkbox-property',
    templateUrl: './checkbox-property.component.html'
})
export class CheckboxPropertyComponent {
    property: FieldProperty;
    group: UntypedFormGroup;

    private readonly labelMap = {
        indexed: 'contenttypes.field.properties.system_indexed.label',
        listed: 'contenttypes.field.properties.listed.label',
        required: 'contenttypes.field.properties.required.label',
        searchable: 'contenttypes.field.properties.user_searchable.label',
        unique: 'contenttypes.field.properties.unique.label'
    };

    setCheckboxLabel(field): string {
        return this.labelMap[field] || field;
    }
}
