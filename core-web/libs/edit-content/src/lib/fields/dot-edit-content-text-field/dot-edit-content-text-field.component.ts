import { ChangeDetectionStrategy, Component, Input, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { AiRefiningInputDirective } from '../../directives/ai-refining-input/ai-refining-input.directive';
import { INPUT_TEXT_OPTIONS } from './utils';

@Component({
    selector: 'dot-edit-content-text-field',
    templateUrl: './dot-edit-content-text-field.component.html',
    styleUrls: ['./dot-edit-content-text-field.component.scss'],
    standalone: true,
    imports: [ReactiveFormsModule, InputTextModule, AiRefiningInputDirective],
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
    currentLanguage = input<string>('en-us');
    readonly inputTextOptions = INPUT_TEXT_OPTIONS;
}
