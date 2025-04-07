import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

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
export class DotKeyValueComponent {
    $autoFocus = input<boolean>(true, { alias: 'autoFocus' });
    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });
    $variables = input<DotKeyValue[]>([], { alias: 'variables' });

    updatedList = output<DotKeyValue[]>();
    delete = output<DotKeyValue>();
    save = output<DotKeyValue>();
    update = output<{
        variable: DotKeyValue;
        oldVariable: DotKeyValue;
    }>();

    protected variableList = signal<DotKeyValue[]>([]);
    protected $forbiddenkeys = computed(() => {
        return this.variableList().reduce(
            (acc, variable) => {
                acc[variable.key] = true;

                return acc;
            },
            {} as Record<string, boolean>
        );
    });

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
