import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
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
        CommonModule,
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
export class DotKeyValueTableRowComponent implements OnChanges {
    @ViewChild('saveButton') saveButton: ElementRef;
    @ViewChild('valueCell') valueCell: ElementRef;

    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);
    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter(false);

    @Input() showHiddenField: boolean = false;
    @Input() variable: DotKeyValue;

    protected form: FormGroup = new FormGroup({
        value: new FormControl('', Validators.required),
        hidden: new FormControl(false)
    });
    protected readonly showEditMenu = signal(false);
    protected readonly passwordPlaceholder = '*****';

    get isHiddenField(): boolean {
        return this.variable?.hidden;
    }

    get initialValue(): string {
        return this.variable.value;
    }

    get currentValue(): string {
        return this.form.get('value').value;
    }

    get inputType(): string {
        return this.variable?.hidden ? 'password' : 'text';
    }

    ngOnChanges(): void {
        this.form.setValue({
            value: this.initialValue,
            hidden: this.isHiddenField
        });
    }

    /**
     * Focus on Key input
     * @param {Event} [$event]
     * @memberof DotKeyValueTableRowComponent
     */
    focusKeyInput($event: Event): void {
        $event.stopPropagation();
        this.valueCell.nativeElement.click();
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableRowComponent
     */
    onCancel($event: KeyboardEvent): void {
        $event.stopPropagation();
        this.showEditMenu.set(false);
        this.form.get('value').setValue(this.initialValue);
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
            value: this.currentValue
        });
    }
}
