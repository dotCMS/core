import { Component, input, inject, computed } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

@Component({
    selector: 'dot-uve-style-editor-field-radio',
    standalone: true,
    imports: [ReactiveFormsModule, RadioButtonModule],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    templateUrl: './uve-style-editor-field-radio.component.html',
    styleUrl: './uve-style-editor-field-radio.component.scss'
})
export class UveStyleEditorFieldRadioComponent {
    $field = input.required<StyleEditorFieldSchema>({ alias: 'field' });

    $options = computed<StyleEditorRadioOptionObject[]>(() => {
        return this.$field().config?.options || [];
    });

    $hasRadioImage = computed(() => {
        const options = this.$options();
        return options.length > 0 && options[0]?.imageURL !== undefined;
    });

    $columns = computed(() => {
        return this.$field().config?.columns || 1;
    });
}
