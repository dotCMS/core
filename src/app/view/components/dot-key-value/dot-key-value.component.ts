import {
    Component,
    Input,
    SimpleChanges,
    OnChanges,
    OnInit,
    Output,
    EventEmitter
} from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';
import * as _ from 'lodash';
import { DotKeyValue } from '@shared/models/dot-key-value/dot-key-value.model';
import { DotKeyValueUtil } from './util/dot-key-value-util';

@Component({
    selector: 'dot-key-value',
    styleUrls: ['./dot-key-value.component.scss'],
    templateUrl: './dot-key-value.component.html'
})
export class DotKeyValueComponent implements OnInit, OnChanges {
    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() variables: DotKeyValue[] = [];
    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter(false);
    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);

    variablesBackup: DotKeyValue[] = [];
    messages: { [key: string]: string } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'keyValue.key_header.label',
                'keyValue.value_header.label',
                'keyValue.actions_header.label',
                'keyValue.value_no_rows.label',
                'keyValue.hidden_header.label'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messages = messages;
            });
    }

    ngOnChanges(_changes: SimpleChanges): void {
        this.variablesBackup = _.cloneDeep(this.variables);
    }

    /**
     * Handle Delete event, deleting the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    deleteVariable(variable: DotKeyValue): void {
        [this.variables, this.variablesBackup] = [
            this.variables,
            this.variablesBackup
        ].map((variables: DotKeyValue[]) =>
            variables.filter((item: DotKeyValue) => item.key !== variable.key)
        );
        this.delete.emit(variable);
    }

    /**
     * Handle Save event, saving the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    saveVariable(variable: DotKeyValue): void {
        this.save.emit(variable);

        variable = this.setHiddenValue(variable);
        const indexChanged = DotKeyValueUtil.getVariableIndexChanged(variable, this.variables);
        if (indexChanged !== null) {
            this.variables[indexChanged] = _.cloneDeep(variable);
        } else {
            this.variables = [variable, ...this.variables];
        }
        this.variablesBackup = [...this.variables];
    }

    /**
     * Handle Cancel event, restoring the original value of the variable
     * @param {number} fieldIndex
     * @memberof DotKeyValueComponent
     */
    onCancel(fieldIndex: number): void {
        this.variablesBackup[fieldIndex] = _.cloneDeep(this.variables[fieldIndex]);
    }

    private setHiddenValue(variable: DotKeyValue): DotKeyValue {
        variable.value = variable.hidden ? '********' : variable.value;
        return variable;
    }
}
