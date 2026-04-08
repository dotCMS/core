import { Injectable, signal } from '@angular/core';

import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export type InsertImageFn = (src: string, title?: string, alt?: string) => void;

export interface ImageInitialValues {
    src: string;
    title: string;
    alt: string;
}

@Injectable({ providedIn: 'root' })
export class ImageDialogService extends FloatingBlockDialogService {
    readonly initialValues = signal<ImageInitialValues | null>(null);

    private insertFn: InsertImageFn | null = null;

    open(
        insertFn: InsertImageFn,
        clientRectFn: () => DOMRect | null,
        initialValues?: ImageInitialValues
    ): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
            this.initialValues.set(initialValues ?? null);
        });
    }

    insert(src: string, title?: string, alt?: string): void {
        this.insertFn?.(src, title, alt);
        this.close();
    }

    close(): void {
        this.closeFloating(() => {
            this.initialValues.set(null);
            this.insertFn = null;
        });
    }
}
