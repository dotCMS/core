import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    ElementRef,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotFieldVariable } from '../../models/dot-field-variable.interface';

@Component({
    selector: 'dot-content-type-fields-variables-table-row',
    styleUrls: ['./dot-content-type-fields-variables-table-row.component.scss'],
    templateUrl: './dot-content-type-fields-variables-table-row.component.html'
})
export class DotContentTypeFieldsVariablesTableRowComponent implements OnInit, OnChanges {
    @ViewChild('saveButton')
    saveButton: ElementRef;
    @ViewChild('keyCell')
    keyCell: ElementRef;
    @ViewChild('valueCell')
    valueCell: ElementRef;

    @Input()
    fieldVariable: DotFieldVariable;
    @Input()
    variableIndex: number;

    @Output()
    save: EventEmitter<number> = new EventEmitter(false);
    @Output()
    cancel: EventEmitter<number> = new EventEmitter(false);
    @Output()
    delete: EventEmitter<number> = new EventEmitter(false);

    rowActiveHighlight: Boolean = false;
    showEditMenu: Boolean = false;
    saveDisabled: Boolean = false;
    messages: { [key: string]: string } = {};
    elemRef: ElementRef;
    isEditing = false;

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.variables.key_input.placeholder',
                'contenttypes.field.variables.value_input.placeholder',
                'contenttypes.action.save',
                'contenttypes.action.cancel'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messages = messages;
                if (!this.isEditing) {
                    this.keyCell.nativeElement.click();
                }
            });
    }

    ngOnChanges(changes: SimpleChanges) {
        this.isEditing = changes.fieldVariable && !!changes.fieldVariable.currentValue.value;
    }

    /**
     * Focus on Key input
     * @param {Event} [$event]
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    focusKeyInput($event: Event): void {
        $event.stopPropagation();
        this.valueCell.nativeElement.click();
    }

    /**
     * Sets initial fields properties
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    editFieldInit(): void {
        this.rowActiveHighlight = true;
        this.showEditMenu = true;
        this.saveDisabled = this.isFieldDisabled();
    }

    /**
     * Handle Cancel event
     * @param {KeyboardEvent} $event
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    onCancel($event: KeyboardEvent): void {
        $event.stopPropagation();
        this.cancel.emit(this.variableIndex);
    }

    /**
     * Handle Enter key event
     * @param {KeyboardEvent} $event
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    onPressEnter($event: KeyboardEvent): void {
        if (this.keyInputInvalid($event)) {
            this.elemRef = this.keyCell;
        } else if (this.fieldVariable.key !== '') {
            this.getElementToFocus($event);
        }

        setTimeout(() => {
            this.elemRef.nativeElement.click();
        });
    }

    /**
     * Handle Save event
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    saveVariable(): void {
        this.save.emit(this.variableIndex);
    }

    private isFieldDisabled(): boolean {
        return this.fieldVariable.key === '' || this.fieldVariable.value === '' ? true : false;
    }

    private keyInputInvalid($event: KeyboardEvent): boolean {
        return this.fieldVariable.key === '' && this.isKeyInput($event);
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private getElementToFocus($event: KeyboardEvent): void {
        if (this.isKeyInput($event) || this.fieldVariable.value === '') {
            this.elemRef = this.valueCell;
        } else if (this.fieldVariable.value !== '') {
            this.elemRef = this.saveButton;
        }
    }

    private isKeyInput($event: KeyboardEvent): boolean {
        return $event.srcElement.classList.contains('field-variable-key-input');
    }
}
