import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    signal
} from '@angular/core';
import {
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

@Component({
    selector: 'dot-key-value-table-row',
    styleUrls: ['./dot-key-value-table-row.component.scss'],
    templateUrl: './dot-key-value-table-row.component.html',
    standalone: true,
    imports: [
        ButtonModule,
        InputSwitchModule,
        InputTextareaModule,
        InputTextModule,
        FormsModule,
        ReactiveFormsModule,
        TableModule,
        DotMessagePipe
    ]
})
export class DotKeyValueTableRowComponent implements OnInit {
    @ViewChild('saveButton') saveButton: ElementRef;
    @ViewChild('valueCell') valueCell: ElementRef;
    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);
    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter(false);

    @Input() showHiddenField = false;
    @Input() variable: DotKeyValue;

    form: FormGroup;
    protected readonly showEditMenu = signal(false);
    protected readonly passwordPlaceholder = '*****';

    get isHiddenField(): boolean {
        return this.variable?.hidden || false;
    }

    get initialValue(): string {
        return this.variable.value;
    }

    get currentValue(): string {
        return this.form.get('value').value;
    }

    get currentHiddenValue(): boolean {
        return this.form?.get('hidden').value;
    }

    get inputType(): string {
        return this.currentHiddenValue ? 'password' : 'text';
    }

    ngOnInit(): void {
        this.form = new FormGroup({
            value: new FormControl(this.initialValue, Validators.required),
            hidden: new FormControl({
                value: this.isHiddenField,
                disabled: this.isHiddenField
            })
        });
    }

    /**
     * Focus on Key input
     * @param {Event} [$event]
     * @memberof DotKeyValueTableRowComponent
     */
    focusValueInput($event: Event): void {
        $event.stopPropagation();
        this.valueCell.nativeElement.click();
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableRowComponent
     */
    onCancel(): void {
        this.showEditMenu.set(false);
        this.form.reset({
            value: this.initialValue,
            hidden: this.isHiddenField
        });
    }

    /**
     * Handle Enter key event
     * @memberof DotKeyValueTableRowComponent
     */
    onPressEnter(): void {
        this.saveButton.nativeElement.click();
    }

    /**
     * Handle Save event emitting variable value to parent component
     * @memberof DotKeyValueTableRowComponent
     */
    saveVariable(): void {
        this.showEditMenu.set(false);
        this.save.emit({
            ...this.variable,
            value: this.currentValue,
            hidden: this.currentHiddenValue
        });
    }
}
