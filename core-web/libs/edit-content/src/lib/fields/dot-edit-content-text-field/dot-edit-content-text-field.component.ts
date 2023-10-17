import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { INPUT_TYPE, InputTextOptions } from '../models';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    styleUrls: ['./dot-edit-content-text-field.component.scss'],
    standalone: true,
    imports: [ReactiveFormsModule, InputTextModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextFieldComponent {
    @Input() field!: DotCMSContentTypeField;

    // This is to hold the options for the input type
    readonly inputTextOptions: Record<INPUT_TYPE, InputTextOptions> = {
        TEXT: {
            type: 'text',
            inputMode: 'text'
        },

        INTEGER: {
            type: 'number',
            inputMode: 'numeric',
            step: 1
        },
        FLOAT: {
            type: 'number',
            inputMode: 'decimal',
            step: 0.1
        }
    };
}
