import { NgZone, inject, signal } from '@angular/core';

/**
 * Shared open/close + signals for floating block insert dialogs.
 * Subclasses own insert callbacks and any extra state (e.g. initialValues).
 */
export abstract class FloatingBlockDialogService {
    protected readonly zone = inject(NgZone);

    readonly isOpen = signal(false);
    readonly clientRectFn = signal<(() => DOMRect | null) | null>(null);

    protected openFloating(clientRectFn: () => DOMRect | null, arm: () => void): void {
        this.zone.run(() => {
            arm();
            this.clientRectFn.set(clientRectFn);
            this.isOpen.set(true);
        });
    }

    protected closeFloating(disarm: () => void): void {
        this.zone.run(() => {
            disarm();
            this.isOpen.set(false);
            this.clientRectFn.set(null);
        });
    }
}
