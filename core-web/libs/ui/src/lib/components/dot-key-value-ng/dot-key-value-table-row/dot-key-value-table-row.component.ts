import {
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    output,
    viewChild,
    ChangeDetectionStrategy,
    model,
    ChangeDetectorRef
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { debounceTime, distinctUntilChanged, skip } from 'rxjs/operators';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

@Component({
    selector: 'dot-key-value-table-row',
    styleUrls: ['./dot-key-value-table-row.component.scss'],
    templateUrl: './dot-key-value-table-row.component.html',
    imports: [
        ButtonModule,
        ToggleSwitchModule,
        TextareaModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        TableModule,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueTableRowComponent {
    /**
     * Form builder service for creating reactive forms
     */
    #formBuilder = inject(FormBuilder);

    /**
     * Change detector reference
     */
    changeDetectorRef = inject(ChangeDetectorRef);

    /**
     * Event emitted when a variable is saved, containing the updated DotKeyValue
     */
    save = output<DotKeyValue>();

    /**
     * Event emitted when a variable is deleted, containing the DotKeyValue to be removed
     */
    delete = output<void>();

    /**
     * Input that controls whether to show a hidden field toggle
     */
    $showHiddenField = input.required<boolean>({ alias: 'showHiddenField' });

    /**
     * Input that controls the index of the row
     */
    $index = input.required<number>({ alias: 'index' });

    /**
     * Input that controls whether to show a drag and drop handle
     */
    $dragAndDrop = input.required<boolean>({ alias: 'dragAndDrop' });

    /**
     * The key-value pair to be displayed and edited in this row
     */
    $variable = model.required<DotKeyValue>({ alias: 'variable' });

    /**
     * Reference to the value cell element in the template
     */
    $valueCellRef = viewChild.required<ElementRef>('valueCell');

    /**
     * Placeholder text shown for hidden password fields
     */
    protected readonly passwordPlaceholder = '*****';

    /**
     * Computed property that determines if the current field should be displayed as hidden
     * @returns {boolean} True if the field should be hidden
     */
    $isHiddenField = computed(() => {
        return this.$variable()?.hidden || false;
    });

    /**
     * Reactive form to handle the value and hidden state
     */
    form = this.#formBuilder.nonNullable.group({
        value: ['', Validators.required],
        hidden: [false]
    });

    /**
     * Sets up an effect to sync the form values with the input variable
     */
    constructor() {
        effect(() => {
            const { value, hidden } = this.$variable();
            this.form.patchValue({ value, hidden });
            this.changeDetectorRef.detectChanges();
        });

        this.hiddenControl.valueChanges
            .pipe(skip(1), debounceTime(50), distinctUntilChanged())
            .subscribe(() => {
                this.saveVariable();
            });

        this.valueControl.valueChanges
            .pipe(skip(1), debounceTime(1000), distinctUntilChanged())
            .subscribe(() => {
                this.saveVariable();
            });
    }

    /**
     * Getter for the value form control
     * @returns The value form control
     */
    get valueControl() {
        return this.form.controls.value;
    }

    /**
     * Getter for the hidden form control
     * @returns The hidden form control
     */
    get hiddenControl() {
        return this.form.controls.hidden;
    }

    /**
     * Determines the input type based on the hidden state
     * @returns {string} 'password' if hidden, 'text' otherwise
     */
    get inputType(): string {
        return this.hiddenControl.value ? 'password' : 'text';
    }

    /**
     * Handles Enter key press by preventing default behavior and triggering save
     * @param {Event} event - The keyboard event
     */
    onPressEnter(event: Event): void {
        event.preventDefault();
        this.saveVariable();
    }

    /**
     * Saves the variable by emitting the current form values
     * Combines the original variable with updated value and hidden state
     */
    saveVariable(): void {
        this.save.emit({
            ...this.$variable(),
            value: this.valueControl.value,
            hidden: this.hiddenControl.value
        });
    }
}
