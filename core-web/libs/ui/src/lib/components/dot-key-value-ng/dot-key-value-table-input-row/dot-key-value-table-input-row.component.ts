import {
    AfterViewInit,
    Component,
    ElementRef,
    input,
    output,
    viewChild,
    inject
} from '@angular/core';
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
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

@Component({
    selector: 'dot-key-value-table-input-row',
    styleUrls: ['./dot-key-value-table-input-row.component.scss'],
    templateUrl: './dot-key-value-table-input-row.component.html',
    standalone: true,
    imports: [
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        DotMessagePipe
    ]
})
export class DotKeyValueTableInputRowComponent implements AfterViewInit {
    #fb = inject(FormBuilder);

    $keyCell = viewChild.required<ElementRef>('keyCell');
    $saveButton = viewChild.required<ElementRef>('saveButton');
    $valueCell = viewChild.required<ElementRef>('valueCell');

    $autoFocus = input<boolean>(true, { alias: 'autoFocus' });
    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });
    $forbiddenkeys = input<Record<string, boolean>>({}, { alias: 'forbiddenkeys' });

    save = output<DotKeyValue>();

    form = this.#fb.nonNullable.group({
        key: ['', [Validators.required, this.keyValidator()]],
        value: ['', Validators.required],
        hidden: [false]
    });

    get keyControl() {
        return this.form.controls.key;
    }

    get valueControl() {
        return this.form.controls.value;
    }

    get hiddenControl() {
        return this.form.controls.hidden;
    }

    ngAfterViewInit(): void {
        if (this.$autoFocus()) {
            this.$keyCell().nativeElement.focus();
        }
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableInputRowComponent
     */
    onCancel($event: Event): void {
        $event.stopPropagation();
        this.resetForm();
    }

    /**
     * Handle Save event emitting variable value to parent component
     * @memberof DotKeyValueTableInputRowComponent
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
     * Reset form and focus on key input
     *
     * @memberof DotKeyValueTableInputRowComponent
     */
    resetForm(): void {
        this.form.reset();
        this.$keyCell().nativeElement.focus();
    }

    /**
     * Handle Enter key event on key input
     * If key control is valid, focus on value input
     * If key control is invalid, focus on key input
     *
     * @return {*}  {void}
     * @memberof DotKeyValueTableInputRowComponent
     */
    handleKeyInputEnter($event: Event): void {
        $event.preventDefault();

        if (this.keyControl.valid) {
            this.$valueCell().nativeElement.focus();

            return;
        }

        this.$keyCell().nativeElement.focus();
    }

    handleValueInputEnter($event: Event): void {
        $event.preventDefault();
        this.saveVariable();
    }

    private keyValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null => {
            if (!this.$forbiddenkeys()[value]) {
                return null;
            }

            return { duplicatedKey: true };
        };
    }
}
