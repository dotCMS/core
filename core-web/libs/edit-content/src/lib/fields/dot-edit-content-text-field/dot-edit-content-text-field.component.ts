import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { ContentTypeTextField } from '@dotcms/dotcms-models';

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
    $field = input.required<ContentTypeTextField>({ alias: 'field' });

    $inputTextOptions = computed(() => {
        const field = this.$field();

        return INPUT_TEXT_OPTIONS[field.fieldType];
    });
}
