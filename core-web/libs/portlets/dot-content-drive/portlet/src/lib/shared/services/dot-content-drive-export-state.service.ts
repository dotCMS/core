import { Injectable, Signal, signal } from '@angular/core';

/**
 * Whether a Content Drive export is running, shared by every Export button (#37900, FR-036).
 *
 * The toolbar renders two Export buttons, one with nothing selected and one beside the workflow
 * actions, and changing the selection swaps one for the other while an export may still be
 * running. Each button reads this flag rather than keeping its own, so the one that appears
 * cannot start a second export until the first one ends.
 *
 * Provided in `root` because the export request is not tied to any component: it keeps running
 * when the button that started it is removed, so the flag has to outlive that button as well. It
 * is not part of `DotContentDriveStore`, which is at the `signalStore` feature limit.
 */
@Injectable({ providedIn: 'root' })
export class DotContentDriveExportStateService {
    readonly #exporting = signal(false);

    /** True from the moment an export starts until it answers, whichever button started it. */
    readonly $exporting: Signal<boolean> = this.#exporting.asReadonly();

    /** Marks an export as running. */
    start(): void {
        this.#exporting.set(true);
    }

    /** Marks the running export as ended, whether it downloaded a file, found nothing or failed. */
    end(): void {
        this.#exporting.set(false);
    }
}
