import { Component, input, inject, computed } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

@Component({
    selector: 'dot-uve-style-editor-field-checkbox-group',
    standalone: true,
    imports: [ReactiveFormsModule, CheckboxModule],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    templateUrl: './uve-style-editor-field-checkbox-group.component.html',
    styleUrl: './uve-style-editor-field-checkbox-group.component.scss'
})
export class UveStyleEditorFieldCheckboxGroupComponent {
    $field = input.required<StyleEditorFieldSchema>({ alias: 'field' });

    $options = computed<StyleEditorRadioOptionObject[]>(() => {
        return this.$field().config?.options || [];
    });
}
