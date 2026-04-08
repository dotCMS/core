import { Injectable } from '@angular/core';

import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export interface TableConfig {
    rows: number;
    cols: number;
    withHeaderRow: boolean;
}

@Injectable({ providedIn: 'root' })
export class TableDialogService extends FloatingBlockDialogService {
    private insertFn: ((config: TableConfig) => void) | null = null;

    open(insertFn: (config: TableConfig) => void, clientRectFn: () => DOMRect | null): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
        });
    }

    /** Commits the table dimensions and closes the dialog (same contract as other block dialogs’ `insert`). */
    insert(config: TableConfig): void {
        this.insertFn?.(config);
        this.close();
    }

    close(): void {
        this.closeFloating(() => {
            this.insertFn = null;
        });
    }
}
