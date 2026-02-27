import { Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { Checkbox } from 'primeng/checkbox';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

@Component({
    selector: 'dot-uve-style-editor-field-checkbox-group',
    standalone: true,
    imports: [ReactiveFormsModule, Checkbox],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    templateUrl: './uve-style-editor-field-checkbox-group.component.html'
})
export class UveStyleEditorFieldCheckboxGroupComponent {
    $field = input.required<StyleEditorFieldSchema>({ alias: 'field' });

    $options = computed<StyleEditorRadioOptionObject[]>(() => {
        return this.$field().config?.options || [];
    });
}
