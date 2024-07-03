import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
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
    @ViewChild('keyCell', { static: true }) keyCell: ElementRef;
    @ViewChild('saveButton', { static: true }) saveButton: ElementRef;
    @ViewChild('valueCell', { static: true }) valueCell: ElementRef;

    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() forbiddenkeys: Record<string, boolean> = {};

    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);

    form = new FormGroup({
        key: new FormControl('', [Validators.required, this.keyValidator()]),
        value: new FormControl('', Validators.required),
        hidden: new FormControl(false)
    });

    private dotMessageService = inject(DotMessageService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);

    get keyControl(): AbstractControl {
        return this.form.get('key');
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
        if (this.autoFocus) {
            this.keyCell.nativeElement.focus();
        }
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableInputRowComponent
     */
    onCancel($event: KeyboardEvent): void {
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
        this.keyCell.nativeElement.focus();
    }

    /**
     * Handle Enter key event on key input
     * If key control is valid, focus on value input
     * If key control is invalid, focus on key input
     *
     * @return {*}  {void}
     * @memberof DotKeyValueTableInputRowComponent
     */
    handleKeyInputEnter($event): void {
        $event.stopPropagation();

        if (this.keyControl.valid) {
            this.valueCell.nativeElement.focus();

            return;
        }

        this.keyCell.nativeElement.focus();
    }

    private keyValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null => {
            if (!this.forbiddenkeys[value]) {
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
