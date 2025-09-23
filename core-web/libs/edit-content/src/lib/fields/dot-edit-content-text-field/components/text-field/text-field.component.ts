import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule, FormsModule, ControlContainer } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { INPUT_TEXT_OPTIONS } from './utils';

import { BaseFieldComponent } from '../../../shared/base-field.component';

@Component({
    selector: 'dot-text-field',
    templateUrl: './text-field.component.html',
    styleUrls: ['./text-field.component.scss'],
    imports: [ReactiveFormsModule, InputTextModule, FormsModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotTextFieldComponent extends BaseFieldComponent {
    $dataType = input.required<string>({ alias: 'dataType' });
    $variableName = input.required<string>({ alias: 'variableName' });
    $hasError = input.required<boolean>({ alias: 'hasError' });
    readonly inputTextOptions = INPUT_TEXT_OPTIONS;

    writeValue(_: unknown): void {
        // Do nothing
    }
}
