import { AfterViewInit, Directive } from '@angular/core';

import { TableState } from 'primeng/api';
import { Table } from 'primeng/table';

/**
 * Directive to restore the state of a table component
 * This need to be done because of this issue: https://github.com/primefaces/primeng/issues/10747
 * if the sortField or sortOrder is defined in the table component, the table will not restore the state
 */

const enum StorageType {
    Session = 'session',
    Local = 'local'
}

@Directive({
    selector: '[dotStateRestore]',
    standalone: true
})
export class DotStateRestoreDirective implements AfterViewInit {
    constructor(private table: Table) {}

    ngAfterViewInit(): void {
        if (!this.table?.stateStorage && !this.table?.stateKey) {
            console.warn('DotStateRestoreDirective: stateStorage or stateKey not found');

            return;
        }

        const stateKey = this.table.stateKey;

        const stateString =
            StorageType.Session === this.table.stateStorage
                ? sessionStorage.getItem(stateKey)
                : localStorage.getItem(stateKey);

        if (stateString) {
            const state = JSON.parse(stateString);
            this.applyStateSort(state);
        }
    }

    applyStateSort(state: TableState): void {
        this.table.sortField = state.sortField;
        this.table.sortOrder = state.sortOrder;
    }
}
