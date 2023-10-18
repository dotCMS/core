import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { INPUT_TEXT_OPTIONS } from './utils';

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

    readonly inputTextOptions = INPUT_TEXT_OPTIONS;
}
