import {
    ChangeDetectionStrategy,
    Component,
    effect,
    input,
    output,
    viewChild
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
export class DotExperimentsInlineEditTextComponent {
    /**
     * Max length of the text override by the user
     */
    maxCharacterLength = input(MAX_INPUT_DESCRIPTIVE_LENGTH);

    /**
     * Flag to show the loading spinner
     */
    isLoading = input(false);

    /**
     * Text to be edited
     */
    text = input('');

    /**
     * Text to be shown when the text is empty
     */
    emptyTextMessage = input('dot.common.inplace.empty.text');

    /**
     * Flag to disable the inplace
     */
    disabled = input(false);

    /**
     * Size of the input and button
     **/
    inputSize = input<InplaceInputSize>('small');

    /**
     * Flag to make the text required
     */
    required = input(false);

    /**
     * Flag to hide the error message
     */
    showErrorMsg = input(true);

    /**
     * Emitted when the text is changed and valid
     */
    textChanged = output<string>();

    inplace = viewChild.required(Inplace);
    form: FormGroup;

    private previousIsLoading = false;

    constructor() {
        this.initForm();

        effect(() => {
            const textValue = this.text();
            this.textControl.setValue(textValue);
        });

        effect(() => {
            const isLoadingValue = this.isLoading();
            isLoadingValue ? this.textControl.disable() : this.textControl.enable();

            // Check if loading changed from true to false
            if (this.previousIsLoading && !isLoadingValue) {
                const inplaceRef = this.inplace();
                if (inplaceRef) {
                    this.deactivateInplace();
                }
            }
            this.previousIsLoading = isLoadingValue;
        });

        effect(() => {
            this.updateValidators();
        });
    }

    get textControl(): FormControl {
        return this.form.controls['text'] as FormControl;
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
        const inplaceRef = this.inplace();
        if (inplaceRef) {
            inplaceRef.deactivate();
        }
        this.resetForm();
    }

    private initForm() {
        this.form = new FormGroup({
            text: new FormControl<string>('', {
                validators: [Validators.required, Validators.maxLength(this.maxCharacterLength())]
            })
        });
        this.updateValidators();
    }

    private resetForm() {
        this.textControl.setValue(this.text());
        this.textControl.markAsPristine();
    }

    private updateValidators() {
        const validators: ValidatorFn[] = [
            DotValidators.noWhitespace,
            Validators.maxLength(this.maxCharacterLength())
        ];

        if (this.required()) {
            validators.push(Validators.required);
        }

        this.textControl.clearValidators();
        this.textControl.setValidators(validators);
        this.textControl.updateValueAndValidity();
    }
}
