import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ChipsModule } from 'primeng/chips';
import { Inplace, InplaceModule } from 'primeng/inplace';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { MAX_INPUT_DESCRIPTIVE_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

/**
 * Component to edit a text inplace and expose
 * the value changed
 *
 * @export
 * @class DotExperimentsInlineEditTextComponent
 */
@Component({
    selector: 'dot-experiments-inplace-edit-text',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        DotMessagePipeModule,
        DotAutofocusModule,
        DotFieldValidationMessageModule,
        ChipsModule,
        InplaceModule,
        NgIf
    ],
    templateUrl: './dot-experiments-inline-edit-text.component.html',
    styleUrls: ['./dot-experiments-inline-edit-text.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsInlineEditTextComponent implements OnChanges {
    @Input()
    maxCharacterLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    @Input()
    isLoading = false;

    @Input()
    text: string;

    @Input()
    emptyText = 'dot.common.inplace.empty.text';

    @Output()
    textChanged = new EventEmitter<string>();

    @ViewChild(Inplace) inplace!: Inplace;

    form: FormGroup;
    /**
     * Enable or disable Inplace
     * */
    @Input()
    disabled: boolean;

    constructor() {
        this.initForm();
    }

    get textControl(): FormControl {
        return this.form.controls['text'] as FormControl;
    }

    ngOnChanges({ text, isLoading, maxCharacterLength }: SimpleChanges): void {
        if (text) {
            this.textControl.setValue(text.currentValue);
        }

        if (isLoading && isLoading.previousValue === true && isLoading.currentValue === false) {
            this.inplace.deactivate();
        }

        if (maxCharacterLength && maxCharacterLength.currentValue) {
            this.updateValidators();
        }
    }

    /**
     * Take te value of the input and emit the value
     */
    saveAction() {
        this.textChanged.emit(this.textControl.value);
    }

    /**
     * Deactivate the Inplace Component
     */
    deactivateInplace() {
        this.inplace.deactivate();
        this.resetForm();
    }

    private initForm() {
        this.form = new FormGroup({
            text: new FormControl<string>('', {
                nonNullable: true,
                validators: [Validators.required, Validators.maxLength(this.maxCharacterLength)]
            })
        });
    }

    private resetForm() {
        this.textControl.setValue(this.text);
    }

    private updateValidators() {
        this.textControl.clearValidators();
        this.textControl.setValidators([
            Validators.required,
            Validators.maxLength(this.maxCharacterLength)
        ]);
        this.textControl.updateValueAndValidity();
    }
}
