import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild
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
        CommonModule,
        ButtonModule,
        InputSwitchModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        DotMessagePipe
    ]
})
export class DotKeyValueTableInputRowComponent implements AfterViewInit {
    @ViewChild('keyCell', { static: true }) keyCell: ElementRef;
    @ViewChild('saveButton', { static: true }) saveButton: ElementRef;
    @ViewChild('valueCell', { static: true }) valueCell: ElementRef;

    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() variablesList: DotKeyValue[] = [];

    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);

    form = new FormGroup({
        key: new FormControl('', [Validators.required, this.customValidator()]),
        value: new FormControl('', Validators.required),
        hidden: new FormControl(false)
    });

    constructor(
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService
    ) {}

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
        this.save.emit({
            ...(this.form.value as DotKeyValue)
        });
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

    private customValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null => {
            const matchKey = this.variablesList.some((item: DotKeyValue) => item.key === value);

            if (!matchKey) {
                return null;
            }

            this.showErrorMessage();

            return { duplicatedKey: true };
        };
    }

    private showErrorMessage(): void {
        this.dotMessageDisplayService.push({
            life: 3000,
            message: this.dotMessageService.get(
                'keyValue.error.duplicated.variable',
                this.form.value.key
            ),
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }
}
