import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    ElementRef
} from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotFieldVariable } from '../../shared/dot-field-variable.interface';

@Component({
    selector: 'dot-content-type-fields-variables-table-row',
    styleUrls: ['./dot-content-type-fields-variables-table-row.component.scss'],
    templateUrl: './dot-content-type-fields-variables-table-row.component.html'
})
export class DotContentTypeFieldsVariablesTableRowComponent implements OnInit {
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

    showEditMenu: Boolean = false;
    saveDisabled: Boolean = false;
    messages: { [key: string]: string } = {};

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
                this.focusKeyInput();
            });
    }

    /**
     * Focus on Key input
     * @param {boolean} [forced]
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    focusKeyInput(forced?: boolean): void {
        if (forced || this.isFieldDisabled()) {
            this.keyCell.nativeElement.click();
        }
    }

    /**
     * Sets initial fields properties
     * @memberof DotContentTypeFieldsVariablesTableRowComponent
     */
    editFieldInit(): void {
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
    // tslint:disable-next-line:cyclomatic-complexity
    onPressEnter($event: KeyboardEvent): void {
        let elemRef: ElementRef;
        if (this.fieldVariable.key === '' && $event.srcElement.classList.contains('field-variable-key-input')) {
            elemRef = this.keyCell;
        } else if ((this.fieldVariable.key !== '' && $event.srcElement.classList.contains('field-variable-key-input')) ||
            (this.fieldVariable.key !== '' && this.fieldVariable.value === '')) {
            elemRef = this.valueCell;
        } else if (this.fieldVariable.key !== '' && this.fieldVariable.value !== '') {
            elemRef = this.saveButton;
        }
        setTimeout(() => {
            elemRef.nativeElement.click();
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
}
