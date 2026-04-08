import { Injectable } from '@angular/core';

import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export type InsertVideoFn = (src: string, title?: string) => void;

@Injectable({ providedIn: 'root' })
export class VideoDialogService extends FloatingBlockDialogService {
    private insertFn: InsertVideoFn | null = null;

    open(insertFn: InsertVideoFn, clientRectFn: () => DOMRect | null): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
        });
    }

    insert(src: string, title?: string): void {
        this.insertFn?.(src, title);
        this.close();
    }

    close(): void {
        this.closeFloating(() => {
            this.insertFn = null;
        });
    }
}
