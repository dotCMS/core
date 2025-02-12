import { ChangeDetectionStrategy, Component, Input, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextareaModule } from 'primeng/inputtextarea';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { AiRefiningInputDirective } from '../../directives/ai-refining-input/ai-refining-input.directive';

@Component({
    selector: 'dot-edit-content-text-area',
    templateUrl: './dot-edit-content-text-area.component.html',
    styleUrls: ['./dot-edit-content-text-area.component.scss'],
    standalone: true,
    imports: [InputTextareaModule, ReactiveFormsModule, AiRefiningInputDirective],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentTextAreaComponent {
    @Input() field: DotCMSContentTypeField;
    currentLanguage = input<string>('en-us');
}
