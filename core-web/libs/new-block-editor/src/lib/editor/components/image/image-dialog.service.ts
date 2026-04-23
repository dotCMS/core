import { Injectable, signal } from '@angular/core';

import { type DotImageData } from '../../extensions/image.extension';
import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export type InsertImageFn = (
    src: string,
    title?: string,
    alt?: string,
    data?: DotImageData
) => void;

export interface ImageUploadCallbacks {
    /** Called immediately when the user picks a file — inserts a placeholder and returns its id. */
    onStart: () => string;
    /** Called after a successful upload — replaces the placeholder with the real image node. */
    onFinish: (id: string, attrs: { src: string; alt?: string; data?: DotImageData }) => void;
    /** Called after a failed upload — removes the placeholder. */
    onCancel: (id: string) => void;
}

export interface ImageInitialValues {
    src: string;
    title: string;
    alt: string;
}

export interface ImageOpenOptions {
    uploadCallbacks?: ImageUploadCallbacks;
    initialValues?: ImageInitialValues;
}

@Injectable({ providedIn: 'root' })
export class ImageDialogService extends FloatingBlockDialogService {
    readonly initialValues = signal<ImageInitialValues | null>(null);

    private insertFn: InsertImageFn | null = null;
    private uploadCallbacks: ImageUploadCallbacks | null = null;

    // Saved across close() so async upload lifecycle can still call finish/cancel
    private pendingFinish: ImageUploadCallbacks['onFinish'] | null = null;
    private pendingCancel: ImageUploadCallbacks['onCancel'] | null = null;

    open(
        insertFn: InsertImageFn,
        clientRectFn: () => DOMRect | null,
        options?: ImageOpenOptions
    ): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
            this.uploadCallbacks = options?.uploadCallbacks ?? null;
            this.initialValues.set(options?.initialValues ?? null);
        });
    }

    insert(src: string, title?: string, alt?: string, data?: DotImageData): void {
        this.insertFn?.(src, title, alt, data);
        this.close();
    }

    /**
     * Inserts a placeholder at the cursor position and immediately closes the dialog.
     * The upload lifecycle callbacks are saved before close() clears dialog state.
     * Returns the placeholder id — pass it to `finishUpload` or `cancelUpload`.
     * Returns null if no upload callbacks were registered (e.g. edit mode).
     */
    startUpload(): string | null {
        if (!this.uploadCallbacks) return null;
        // Capture before close() wipes uploadCallbacks
        const { onStart, onFinish, onCancel } = this.uploadCallbacks;
        this.pendingFinish = onFinish;
        this.pendingCancel = onCancel;
        const id = onStart();
        this.close();
        return id;
    }

    finishUpload(id: string, attrs: { src: string; alt?: string; data?: DotImageData }): void {
        this.pendingFinish?.(id, attrs);
        this.pendingFinish = null;
        this.pendingCancel = null;
    }

    cancelUpload(id: string): void {
        this.pendingCancel?.(id);
        this.pendingFinish = null;
        this.pendingCancel = null;
    }

    close(): void {
        this.closeFloating(() => {
            this.initialValues.set(null);
            this.insertFn = null;
            this.uploadCallbacks = null;
        });
    }
}
