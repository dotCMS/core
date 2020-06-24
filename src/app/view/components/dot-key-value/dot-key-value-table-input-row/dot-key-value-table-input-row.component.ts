import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    ElementRef
} from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotKeyValue } from '@shared/models/dot-key-value/dot-key-value.model';
import { DotKeyValueUtil } from '../util/dot-key-value-util';

@Component({
    selector: 'dot-key-value-table-input-row',
    styleUrls: ['./dot-key-value-table-input-row.component.scss'],
    templateUrl: './dot-key-value-table-input-row.component.html'
})
export class DotKeyValueTableInputRowComponent implements OnInit {
    @ViewChild('saveButton') saveButton: ElementRef;
    @ViewChild('keyCell') keyCell: ElementRef;
    @ViewChild('valueCell') valueCell: ElementRef;

    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() variablesList: DotKeyValue[] = [];

    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);

    saveDisabled: Boolean = true;
    elemRef: ElementRef;
    variable: DotKeyValue = { key: '', hidden: false, value: '' };

    constructor(
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService
    ) {}

    ngOnInit(): void {
        if (this.autoFocus) {
            this.keyCell.nativeElement.focus();
        }
    }

    /**
     * Sets initial fields properties
     *
     * @param {Event} $event
     * @memberof DotKeyValueTableInputRowComponent
     */
    editFieldInit($event?: Event): void {
        const isKeyVariableDuplicated = DotKeyValueUtil.isFieldVariableKeyDuplicated(
            this.variable,
            this.variablesList,
            true
        );
        this.saveDisabled = isKeyVariableDuplicated || DotKeyValueUtil.isEmpty(this.variable);

        if (isKeyVariableDuplicated && DotKeyValueUtil.isEventBlur($event)) {
            this.dotMessageDisplayService.push({
                life: 3000,
                message: this.dotMessageService.get(
                    'keyValue.error.duplicated.variable',
                    (<HTMLInputElement>$event.target).value
                ),
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
        }
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableInputRowComponent
     */
    onCancel($event: KeyboardEvent): void {
        $event.stopPropagation();
        this.cleanVariableValues();
        this.keyCell.nativeElement.focus();
    }

    /**
     * Handle Enter key event
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableInputRowComponent
     */
    onPressEnter($event: KeyboardEvent): void {
        if (DotKeyValueUtil.keyInputInvalid($event, this.variable)) {
            this.elemRef = this.keyCell;
        } else if (this.variable.key !== '') {
            this.getElementToFocus($event);
        }
        this.elemRef.nativeElement.type === 'text'
            ? this.elemRef.nativeElement.focus()
            : this.elemRef.nativeElement.click();
    }

    /**
     * Handle Save event emitting variable value to parent component
     * @memberof DotKeyValueTableInputRowComponent
     */
    saveVariable(): void {
        this.save.emit(this.variable);
        this.cleanVariableValues();
        this.keyCell.nativeElement.focus();
    }

    private getElementToFocus($event: KeyboardEvent): void {
        if (DotKeyValueUtil.isKeyInput($event) || this.variable.value === '') {
            this.elemRef = this.valueCell;
        } else {
            this.elemRef = this.saveButton;
        }
    }

    private cleanVariableValues(): void {
        this.variable = { key: '', hidden: false, value: '' };
        this.saveDisabled = true;
    }
}
