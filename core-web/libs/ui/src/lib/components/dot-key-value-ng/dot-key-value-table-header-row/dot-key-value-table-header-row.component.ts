import { Component, ElementRef, input, output, viewChild, inject } from '@angular/core';
import {
    AbstractControl,
    FormsModule,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators,
    FormBuilder
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

@Component({
    selector: 'dot-key-value-table-header-row',
    templateUrl: './dot-key-value-table-header-row.component.html',
    host: { class: 'contents' },
    imports: [
        ButtonModule,
        ToggleSwitchModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        DotMessagePipe
    ]
})
export class DotKeyValueTableHeaderRowComponent {
    /** Form builder service for creating reactive forms */
    #fb = inject(FormBuilder);

    /** Reference to the key cell element */
    $keyCell = viewChild.required<ElementRef>('keyCell');

    /** Reference to the save button element */
    $saveButton = viewChild.required<ElementRef>('saveButton');

    /** Reference to the value cell element */
    $valueCell = viewChild.required<ElementRef>('valueCell');

    /** Controls visibility of the hidden field option */
    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });

    /** Record of keys that are not allowed to be used */
    $forbiddenkeys = input<Record<string, boolean>>({}, { alias: 'forbiddenkeys' });

    /** Enables drag and drop functionality for the row */
    $dragAndDrop = input<boolean>(false, { alias: 'dragAndDrop' });

    /** Emits the key-value pair when saved */
    save = output<DotKeyValue>();

    /** Form group for managing key-value inputs and validation */
    form = this.#fb.nonNullable.group({
        key: ['', [Validators.required, this.keyValidator()]],
        value: ['', Validators.required],
        hidden: [false]
    });

    /** Gets the key form control */
    get keyControl() {
        return this.form.controls.key;
    }

    /** Gets the value form control */
    get valueControl() {
        return this.form.controls.value;
    }

    /** Gets the hidden form control */
    get hiddenControl() {
        return this.form.controls.hidden;
    }

    /**
     * Handles cancel event by stopping propagation and resetting the form
     *
     * @param {Event} $event - The event object
     */
    onCancel($event: Event): void {
        $event.stopPropagation();
        this.resetForm();
    }

    /**
     * Saves the variable if the form is valid, otherwise marks form controls as touched
     * Emits the form value when valid and resets the form
     */
    saveVariable(): void {
        if (this.form.valid) {
            this.save.emit(this.form.getRawValue());
            this.resetForm();
        } else {
            this.form.markAllAsTouched();
            this.keyControl.markAsDirty();
            this.valueControl.markAsDirty();
        }
    }

    /**
     * Resets the form to initial state and focuses on the key input
     */
    resetForm(): void {
        this.form.reset();
        this.$keyCell().nativeElement.focus();
    }

    /**
     * Handles Enter key event on key input
     * Focuses on value input if key is valid, otherwise keeps focus on key input
     *
     * @param {Event} $event - The keyboard event
     */
    handleKeyInputEnter($event: Event): void {
        $event.preventDefault();

        if (this.keyControl.valid) {
            this.$valueCell().nativeElement.focus();

            return;
        }

        this.$keyCell().nativeElement.focus();
    }

    /**
     * Handles Enter key event on value input
     * Triggers save action when Enter is pressed
     *
     * @param {Event} $event - The keyboard event
     */
    handleValueInputEnter($event: Event): void {
        $event.preventDefault();
        this.saveVariable();
    }

    /**
     * Creates a validator function that checks if a key is forbidden
     *
     * @returns {ValidatorFn} Validator function that returns error if key is forbidden
     * @private
     */
    private keyValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null => {
            if (!this.$forbiddenkeys()[value]) {
                return null;
            }

            return { duplicatedKey: true };
        };
    }
}
