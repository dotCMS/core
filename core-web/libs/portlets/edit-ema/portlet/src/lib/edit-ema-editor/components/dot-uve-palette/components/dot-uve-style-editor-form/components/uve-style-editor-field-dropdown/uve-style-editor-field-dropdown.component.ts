import { Component, input, inject, computed } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

@Component({
    selector: 'dot-uve-style-editor-field-dropdown',
    standalone: true,
    imports: [ReactiveFormsModule, DropdownModule],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    templateUrl: './uve-style-editor-field-dropdown.component.html',
    styleUrl: './uve-style-editor-field-dropdown.component.scss'
})
export class UveStyleEditorFieldDropdownComponent {
    $field = input.required<StyleEditorFieldSchema>({ alias: 'field' });

    $options = computed<StyleEditorRadioOptionObject[]>(() => {
        return this.$field().config?.options || [];
    });
}
