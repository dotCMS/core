import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    computed,
    signal
} from '@angular/core';

import { TableModule } from 'primeng/table';

import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row/dot-key-value-table-input-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotKeyValue {
    key: string;
    hidden?: boolean;
    value: string;
}

@Component({
    selector: 'dot-key-value-ng',
    styleUrls: ['./dot-key-value-ng.component.scss'],
    templateUrl: './dot-key-value-ng.component.html',
    standalone: true,
    imports: [
        TableModule,
        DotKeyValueTableInputRowComponent,
        DotKeyValueTableRowComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueComponent implements OnChanges {
    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() variables: DotKeyValue[] = [];

    @Output() updatedList: EventEmitter<DotKeyValue[]> = new EventEmitter();

    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter();
    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter();
    @Output() update: EventEmitter<{
        variable: DotKeyValue;
        oldVariable: DotKeyValue;
    }> = new EventEmitter();

    protected variableList = signal<DotKeyValue[]>([]);
    protected forbiddenkeys = computed<Record<string, boolean>>(() => {
        return this.variableList().reduce((acc, variable) => {
            acc[variable.key] = true;

            return acc;
        }, {});
    });

    ngOnChanges(): void {
        this.variableList.set([...this.variables]);
    }

    /**
     * Handle Delete event, deleting the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    deleteVariable(index: number): void {
        const deletedVariable = this.variableList()[index];
        this.variableList.update((variables) => {
            variables.splice(index, 1);

            return [...variables];
        });
        this.delete.emit(deletedVariable);
        this.updatedList.emit(this.variableList());
    }

    /**
     * Handle Save event, saving the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    saveVariable(variable: DotKeyValue): void {
        this.variableList.update((variables) => {
            return [variable, ...variables];
        });
        this.save.emit(variable);
        this.updatedList.emit(this.variableList());
    }

    /**
     * Handle Save event, saving the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    updateKeyValue(variable: DotKeyValue, index: number): void {
        const oldVariable = this.variableList()[index];
        this.variableList.update((variables) => {
            variables[index] = variable;

            return [...variables];
        });
        this.update.emit({ variable, oldVariable });
        this.updatedList.emit(this.variableList());
    }
}
