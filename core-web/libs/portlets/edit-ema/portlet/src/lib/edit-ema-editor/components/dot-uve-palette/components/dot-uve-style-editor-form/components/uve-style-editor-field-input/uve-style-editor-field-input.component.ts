import { Component, inject, input, output } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { StyleEditorFieldSchema } from '@dotcms/types/internal';
import { DotMessagePipe } from '@dotcms/ui';

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

    /**
     * Emitted when the user finishes editing the field (blur or Enter).
     *
     * Text/number inputs produce a stream of intermediate values while typing.
     * Rather than persisting on every keystroke, the parent treats this event
     * as the "commit" signal and saves once. Per-keystroke changes still flow
     * through the form's `valueChanges` for the live (headless) preview.
     */
    readonly commit = output<void>();
}
