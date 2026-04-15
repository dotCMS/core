import { Injectable, signal } from '@angular/core';

import { FloatingBlockDialogService } from '../floating-block-dialog.base';

export type InsertLinkFn = (href: string, displayText?: string, openInNewTab?: boolean) => void;

export interface LinkInitialValues {
    href: string;
    displayText: string;
    target?: string | null;
}

@Injectable({ providedIn: 'root' })
export class LinkDialogService extends FloatingBlockDialogService {
    readonly initialValues = signal<LinkInitialValues | null>(null);

    private insertFn: InsertLinkFn | null = null;
    private activeLinkEl: HTMLElement | null = null;

    open(
        insertFn: InsertLinkFn,
        clientRectFn: () => DOMRect | null,
        initialValues?: { href?: string; displayText?: string; target?: string | null },
        linkEl?: HTMLElement
    ): void {
        this.openFloating(clientRectFn, () => {
            this.insertFn = insertFn;
            this.initialValues.set(
                initialValues
                    ? {
                          href: initialValues.href ?? '',
                          displayText: initialValues.displayText ?? '',
                          target: initialValues.target ?? null
                      }
                    : null
            );
            this.activeLinkEl = linkEl ?? null;
            this.activeLinkEl?.classList.add('link-editing');
        });
    }

    insert(href: string, displayText?: string, openInNewTab?: boolean): void {
        this.insertFn?.(href, displayText, openInNewTab);
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
