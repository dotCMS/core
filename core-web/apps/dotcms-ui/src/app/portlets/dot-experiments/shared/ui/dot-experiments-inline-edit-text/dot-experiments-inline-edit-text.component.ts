import { CommonModule } from '@angular/common';
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
        DotMessagePipeModule,
        InplaceModule,
        CommonModule,
        ChipsModule,
        DotAutofocusModule,
        DotFieldValidationMessageModule,
        ReactiveFormsModule
    ],
    templateUrl: './dot-experiments-inline-edit-text.component.html',
    styleUrls: ['./dot-experiments-inline-edit-text.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsInlineEditTextComponent implements OnChanges {
    /**
     * Saving status
     * required
     * */
    @Input()
    isLoading = false;

    /**
     * Text to be edited
     * required
     * */
    @Input()
    text: string;

    /**
     * Fired when the text is changed
     * */
    @Output()
    textChanged = new EventEmitter<string>();

    @ViewChild(Inplace) inplace!: Inplace;

    form: FormGroup;
    protected readonly maxDescriptiveLengthLength = MAX_INPUT_DESCRIPTIVE_LENGTH;

    ngOnChanges(changes: SimpleChanges): void {
        const { text, isLoading } = changes;
        if (text) {
            this.setForm(text.currentValue);
        }

        if (isLoading && isLoading.previousValue === true && isLoading.currentValue === false) {
            this.inplace.deactivate();
        }
    }

    /**
     * Take te value of the input and emit the value
     */
    saveAction() {
        this.textChanged.emit(this.form.value['text']);
    }

    /**
     * Deactivate the Inplace Component
     */
    deactivateInplace() {
        this.inplace.deactivate();
        this.resetForm();
    }

    private setForm(textValue: string) {
        this.form = new FormGroup({
            text: new FormControl<string>(textValue, {
                nonNullable: true,
                validators: [
                    Validators.required,
                    Validators.maxLength(this.maxDescriptiveLengthLength)
                ]
            })
        });
    }

    private resetForm() {
        this.form.controls['text'].setValue(this.text);
    }
}
