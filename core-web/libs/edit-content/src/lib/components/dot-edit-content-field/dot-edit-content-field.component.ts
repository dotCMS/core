import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective } from '@dotcms/ui';

// Input type that you can select when creating the field
export enum INPUT_TYPE {
    TEXT = 'TEXT',
    INTEGER = 'INTEGER',
    FLOAT = 'FLOAT'
}

// This is to hold the options for the input type
export interface InputTextOptions {
    type: string;
    inputMode: string;
    step?: string | number;
}

@Component({
    selector: 'dot-edit-content-field',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        DotFieldRequiredDirective,
        InputTextareaModule
    ],
    templateUrl: './dot-edit-content-field.component.html',
    styleUrls: ['./dot-edit-content-field.component.scss'],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFieldComponent {
    @Input() field!: DotCMSContentTypeField;

    // This is to hold the options for the input type
    readonly INPUT_TEXT_OPTIONS: Record<INPUT_TYPE, InputTextOptions> = {
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
            step: 'any'
        }
    };
}
