import { Injectable, signal } from '@angular/core';

import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export type InsertLinkFn = (href: string, displayText?: string) => void;

export interface LinkInitialValues {
    href: string;
    displayText: string;
}

@Injectable({ providedIn: 'root' })
export class LinkDialogService extends FloatingBlockDialogService {
    readonly initialValues = signal<LinkInitialValues | null>(null);

    private insertFn: InsertLinkFn | null = null;
    private activeLinkEl: HTMLElement | null = null;

    open(
        insertFn: InsertLinkFn,
        clientRectFn: () => DOMRect | null,
        initialValues?: LinkInitialValues,
        linkEl?: HTMLElement
    ): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
            this.initialValues.set(initialValues ?? null);
            this.activeLinkEl = linkEl ?? null;
            this.activeLinkEl?.classList.add('link-editing');
        });
    }

    insert(href: string, displayText?: string): void {
        this.insertFn?.(href, displayText);
        this.close();
    }

    close(): void {
        this.closeFloating(() => {
            this.activeLinkEl?.classList.remove('link-editing');
            this.activeLinkEl = null;
            this.initialValues.set(null);
            this.insertFn = null;
        });
    }
}
