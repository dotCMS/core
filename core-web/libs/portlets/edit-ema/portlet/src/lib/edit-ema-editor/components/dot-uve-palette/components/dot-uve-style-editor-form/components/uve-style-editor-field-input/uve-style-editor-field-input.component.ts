import { Component, input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';
import { StyleEditorFieldSchema } from '@dotcms/uve';

@Component({
    selector: 'dot-uve-style-editor-field-input',
    standalone: true,
    imports: [ReactiveFormsModule, InputTextModule, DotMessagePipe],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    templateUrl: './uve-style-editor-field-input.component.html'
})
export class UveStyleEditorFieldInputComponent {
    $field = input.required<StyleEditorFieldSchema>({ alias: 'field' });
}
