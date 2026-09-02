import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { TableModule } from 'primeng/table';

import { DotKeyValueTableHeaderRowComponent } from './dot-key-value-table-header-row/dot-key-value-table-header-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotKeyValue {
    key: string;
    hidden?: boolean;
    value: string;
}

@Component({
    selector: 'dot-key-value-ng',
    templateUrl: './dot-key-value-ng.component.html',
    imports: [
        TableModule,
        DotKeyValueTableHeaderRowComponent,
        DotKeyValueTableRowComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueComponent {
    /**
     * Controls whether the hidden field option is displayed in the UI
     */
    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });

    /**
     * The list of key-value pairs to be displayed and manipulated
     */
    $variables = input<DotKeyValue[]>([], { alias: 'variables' });

    /**
     * Controls whether drag and drop functionality is enabled for reordering items
     */
    $dragAndDrop = input<boolean>(false, { alias: 'dragAndDrop' });

    /**
     * Emits the updated list of key-value pairs after any change operation
     */
    updatedList = output<DotKeyValue[]>();

    /**
     * Emits when a key-value pair is deleted
     */
    delete = output<DotKeyValue>();

    /**
     * Emits when a new key-value pair is saved
     */
    save = output<DotKeyValue>();

    /**
     * Emits when an existing key-value pair is updated, containing both new and old values
     */
    update = output<{
        variable: DotKeyValue;
        oldVariable: DotKeyValue;
    }>();

    /**
     * Computed signal that holds the current list of variables
     */
    $variableList = computed(() => signal(this.$variables()));

    /**
     * Computed hash map of existing keys to prevent duplicates
     */
    $forbiddenkeys = computed(() => {
        const variableList = this.$variableList();

        return variableList().reduce(
            (acc, variable) => {
                acc[variable.key] = true;

                return acc;
            },
            {} as Record<string, boolean>
        );
    });

    /**
     * Computed value for table column span based on component configuration
     */
    $colspan = computed(() => {
        const showHiddenField = this.$showHiddenField();
        const dragAndDrop = this.$dragAndDrop();

        return showHiddenField ? (dragAndDrop ? 5 : 4) : 3;
    });

    /**
     * Handles the deletion of a key-value pair
     * Removes the variable from the local list and emits events for parent components
     *
     * @param {number} index - The index of the variable to delete in the array
     */
    deleteVariable(index: number): void {
        const variableList = this.$variableList();
        const deletedVariable = variableList()[index];
        variableList.update((variables) => {
            variables.splice(index, 1);

            return [...variables];
        });
        this.delete.emit(deletedVariable);
        this.updatedList.emit(variableList());
    }

    /**
     * Handles saving a new key-value pair
     * Adds the variable to the local list and emits events for parent components
     *
     * @param {DotKeyValue} variable - The new key-value pair to save
     */
    saveVariable(variable: DotKeyValue): void {
        const variableList = this.$variableList();
        variableList.update((variables) => {
            return [variable, ...variables];
        });
        this.save.emit(variable);
        this.updatedList.emit(variableList());
    }

    /**
     * Handles updating an existing key-value pair
     * Updates the variable in the local list and emits events for parent components
     *
     * @param {DotKeyValue} variable - The updated key-value pair
     * @param {number} index - The index of the variable to update in the array
     */
    updateKeyValue(variable: DotKeyValue, index: number): void {
        const variableList = this.$variableList();
        const oldVariable = variableList()[index];
        variableList.update((variables) => {
            variables[index] = variable;

            return [...variables];
        });
        this.update.emit({ variable, oldVariable });
        this.updatedList.emit(variableList());
    }

    /**
     * Handles reordering of variables after drag and drop operations
     * Emits the updated list for parent components to process
     */
    reorderVariables(): void {
        const variableList = this.$variableList();
        this.updatedList.emit(variableList());
    }
}
