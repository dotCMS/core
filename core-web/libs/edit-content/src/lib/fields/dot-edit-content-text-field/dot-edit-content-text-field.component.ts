import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule, FormsModule, ControlContainer } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { INPUT_TEXT_OPTIONS } from './utils';

import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    styleUrls: ['./dot-edit-content-text-field.component.scss'],
    imports: [ReactiveFormsModule, FormsModule, InputTextModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextFieldComponent extends BaseFieldComponent {
    /**
     * The field configuration from DotCMS
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    readonly inputTextOptions = INPUT_TEXT_OPTIONS;
}
