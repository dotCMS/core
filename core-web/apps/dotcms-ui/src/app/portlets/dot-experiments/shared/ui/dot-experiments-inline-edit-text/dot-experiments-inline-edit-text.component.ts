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
import {
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ChipsModule } from 'primeng/chips';
import { Inplace, InplaceModule } from 'primeng/inplace';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { MAX_INPUT_DESCRIPTIVE_LENGTH } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';

/**
 * Component to edit a text inplace and expose the changed value
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
    emptyTextMessage = 'dot.common.inplace.empty.text';
    @Output()
    textChanged = new EventEmitter<string>();
    @ViewChild(Inplace) inplace!: Inplace;
    form: FormGroup;
    @Input()
    disabled: boolean;
    private validatorsFn: ValidatorFn[] = [];

    constructor() {
        this.initForm();
    }

    @Input()
    set textRequired(value: boolean) {
        if (value) this.validatorsFn.push(Validators.required);
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
            this.validatorsFn.push(Validators.maxLength(maxCharacterLength.currentValue));
        } else {
            this.validatorsFn.push(Validators.maxLength(this.maxCharacterLength));
        }

        this.updateValidators();
    }

    /**
     * Take te value of the input and emit the value
     *
     * @memberof DotExperimentsInlineEditTextComponent
     * @returns void
     */
    saveAction(): void {
        this.textChanged.emit(this.textControl.value);
    }

    /**
     * Deactivate the Inplace Component and reset the form
     *
     * @memberof DotExperimentsInlineEditTextComponent
     * @returns void
     */
    deactivateInplace() {
        this.inplace.deactivate();
        this.resetForm();
    }

    private initForm() {
        this.form = new FormGroup({
            text: new FormControl<string>('', {
                validators: [Validators.required, Validators.maxLength(this.maxCharacterLength)]
            })
        });
    }

    private resetForm() {
        this.textControl.setValue(this.text);
    }

    private updateValidators() {
        this.textControl.clearValidators();
        this.textControl.setValidators(this.validatorsFn);
        this.textControl.updateValueAndValidity();
    }
}
