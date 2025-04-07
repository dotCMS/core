import {
    AfterViewInit,
    Component,
    ElementRef,
    input,
    OnInit,
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

import { debounceTime } from 'rxjs/operators';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

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
export class DotKeyValueTableInputRowComponent implements OnInit, AfterViewInit {
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

    private dotMessageService = inject(DotMessageService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);

    get keyControl() {
        return this.form.controls.key;
    }

    get valueControl() {
        return this.form.controls.value;
    }

    get hiddenControl() {
        return this.form.controls.hidden;
    }

    ngOnInit(): void {
        this.keyControl.valueChanges.pipe(debounceTime(250)).subscribe((value) => {
            const { duplicatedKey } = this.keyControl.errors || {};
            if (duplicatedKey) {
                this.showErrorMessage(value);
            }
        });
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
        this.save.emit(this.form.getRawValue());
        this.resetForm();
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
        $event.stopPropagation();

        if (this.keyControl.valid) {
            this.$valueCell().nativeElement.focus();

            return;
        }

        this.$keyCell().nativeElement.focus();
    }

    private keyValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null => {
            if (!this.$forbiddenkeys()[value]) {
                return null;
            }

            return { duplicatedKey: true };
        };
    }

    private showErrorMessage(value: string): void {
        this.dotMessageDisplayService.push({
            life: 3000,
            message: this.dotMessageService.get('keyValue.error.duplicated.variable', value),
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }
}
