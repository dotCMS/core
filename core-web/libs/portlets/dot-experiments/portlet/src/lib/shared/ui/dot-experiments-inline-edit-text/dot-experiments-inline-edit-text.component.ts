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

import { ButtonModule } from 'primeng/button';
import { Inplace, InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';

import { MAX_INPUT_DESCRIPTIVE_LENGTH } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotTrimInputDirective,
    DotValidators
} from '@dotcms/ui';

type InplaceInputSize = 'small' | 'large';

/**
 * Component to edit a text inplace and if the text control is valid
 * emit the value
 *
 * @export
 * @class DotExperimentsInlineEditTextComponent
 */
@Component({
    selector: 'dot-experiments-inplace-edit-text',
    imports: [
        ReactiveFormsModule,
        DotMessagePipe,
        DotAutofocusDirective,
        DotFieldValidationMessageComponent,
        InplaceModule,
        ButtonModule,
        InputTextModule,
        DotTrimInputDirective
    ],
    templateUrl: './dot-experiments-inline-edit-text.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsInlineEditTextComponent implements OnChanges {
    /**
     * Max length of the text override by the user
     */
    @Input()
    maxCharacterLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    /**
     * Flag to show the loading spinner
     */
    @Input()
    isLoading = false;

    /**
     * Text to be edited
     */
    @Input()
    text = '';

    /**
     * Text to be shown when the text is empty
     */
    @Input()
    emptyTextMessage = 'dot.common.inplace.empty.text';

    /**
     * Flag to disable the inplace
     */
    @Input()
    disabled = false;

    /**
     * Size of the input and button
     **/
    @Input()
    inputSize: InplaceInputSize = 'small';

    /**
     * Flag to make the text required
     */
    @Input()
    required = false;

    /**
     * Flag to hide the error message
     */
    @Input()
    showErrorMsg = true;

    /**
     * Emitted when the text is changed and valid
     */
    @Output()
    textChanged = new EventEmitter<string>();

    @ViewChild(Inplace) inplace!: Inplace;
    form: FormGroup;

    constructor() {
        this.initForm();
    }

    get textControl(): FormControl {
        return this.form.controls['text'] as FormControl;
    }

    ngOnChanges({ text, isLoading, maxCharacterLength, required }: SimpleChanges): void {
        if (text) {
            this.textControl.setValue(text.currentValue);
        }

        if (isLoading && isLoading.previousValue === true && isLoading.currentValue === false) {
            this.deactivateInplace();
        }

        isLoading && isLoading.currentValue
            ? this.textControl.disable()
            : this.textControl.enable();

        if (maxCharacterLength || required) {
            this.updateValidators();
        }
    }

    /**
     * Take te value of the input and emit the value
     * if the textControls is valid
     *
     * @memberof DotExperimentsInlineEditTextComponent
     * @returns void
     */
    saveAction(): void {
        if (this.textControl.valid) {
            this.textChanged.emit(this.textControl.value.trim());
        }
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
        this.updateValidators();
    }

    private resetForm() {
        this.textControl.setValue(this.text);
        this.textControl.markAsPristine();
    }

    private updateValidators() {
        const validators: ValidatorFn[] = [
            DotValidators.noWhitespace,
            Validators.maxLength(this.maxCharacterLength)
        ];

        if (this.required) {
            validators.push(Validators.required);
        }

        this.textControl.clearValidators();
        this.textControl.setValidators(validators);
        this.textControl.updateValueAndValidity();
    }
}
