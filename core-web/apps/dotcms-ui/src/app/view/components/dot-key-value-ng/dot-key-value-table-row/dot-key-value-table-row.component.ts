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
import * as _ from 'lodash';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';
import { DotKeyValueUtil } from '../util/dot-key-value-util';

@Component({
    selector: 'dot-key-value-table-row',
    styleUrls: ['./dot-key-value-table-row.component.scss'],
    templateUrl: './dot-key-value-table-row.component.html'
})
export class DotKeyValueTableRowComponent implements OnInit, OnChanges {
    @ViewChild('saveButton') saveButton: ElementRef;
    @ViewChild('valueCell') valueCell: ElementRef;

    @Input() showHiddenField: boolean;
    @Input() isHiddenField: boolean;
    @Input() variable: DotKeyValue;
    @Input() variableIndex: number;
    @Input() variablesList: DotKeyValue[] = [];

    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);
    @Output() cancel: EventEmitter<number> = new EventEmitter(false);
    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter(false);

    variableCopy: DotKeyValue;
    showEditMenu = false;
    saveDisabled = false;
    elemRef: ElementRef;

    ngOnInit(): void {
        this.variableCopy = _.cloneDeep(this.variable);
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.variable) {
            this.variableCopy = _.cloneDeep(this.variable);
        }
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
     * Sets initial fields properties
     *
     * @memberof DotKeyValueTableRowComponent
     */
    editFieldInit(): void {
        this.showEditMenu = true;
        const isKeyVariableDuplicated = DotKeyValueUtil.isFieldVariableKeyDuplicated(
            this.variableCopy,
            this.variablesList
        );

        this.saveDisabled = isKeyVariableDuplicated || DotKeyValueUtil.isEmpty(this.variableCopy);
    }

    /**
     * Handle Cancel event event emmitting variable index to parent component
     * @param {KeyboardEvent} $event
     * @memberof DotKeyValueTableRowComponent
     */
    onCancel($event: KeyboardEvent): void {
        $event.stopPropagation();
        this.showEditMenu = false;
        this.cancel.emit(this.variableIndex);
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
        this.showEditMenu = false;
        this.save.emit(this.variableCopy);
    }
}
