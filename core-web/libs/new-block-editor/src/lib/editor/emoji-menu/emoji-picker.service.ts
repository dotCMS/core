import { Injectable, NgZone, inject, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class EmojiPickerService {
    private readonly zone = inject(NgZone);

    readonly isOpen = signal(false);
    readonly clientRectFn = signal<(() => DOMRect | null) | null>(null);

    private insertFn: ((emoji: string) => void) | null = null;

    open(insertFn: (emoji: string) => void, clientRectFn: () => DOMRect | null): void {
        this.zone.run(() => {
            this.insertFn = insertFn;
            this.clientRectFn.set(clientRectFn);
            this.isOpen.set(true);
        });
    }

    close(): void {
        this.zone.run(() => {
            this.isOpen.set(false);
            this.clientRectFn.set(null);
            this.insertFn = null;
        });
    }

    insert(emoji: string): void {
        this.insertFn?.(emoji);
    }
}
