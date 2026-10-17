import { Injectable, Signal, computed, signal } from '@angular/core';

import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotTitleStatus } from '@dotcms/dotcms-models';

/**
 * Carries the content state chip from the Edit Content editor to the header of the dialog it sits
 * in.
 *
 * The dialog's title belongs to PrimeNG's header, which is created apart from the dialog body, so the
 * two cannot share a component. Both do resolve the same `DynamicDialogRef` (PrimeNG hands one to
 * everything it creates for a dialog), so the editor registers its signal under that ref and the
 * header reads it back: one dialog never changes the chip of another, and the entry goes away with
 * the ref.
 */
@Injectable({ providedIn: 'root' })
export class DotEditContentDialogTitleStatusService {
    readonly #sources = new WeakMap<DynamicDialogRef, ReturnType<typeof createSlot>>();

    /**
     * Hands the editor's own signal of what the chip shows to the dialog's header.
     *
     * @param ref The dialog's ref.
     * @param source The editor's signal; `null` hides the chip.
     */
    register(ref: DynamicDialogRef, source: Signal<DotTitleStatus>): void {
        this.#slot(ref).source.set(source);
    }

    /**
     * What the chip of one dialog shows: whatever the editor registered, or `null` (hidden) until it
     * has. Safe to read before the editor registers.
     *
     * @param ref The dialog's ref.
     */
    for(ref: DynamicDialogRef): Signal<DotTitleStatus> {
        return this.#slot(ref).status;
    }

    #slot(ref: DynamicDialogRef): ReturnType<typeof createSlot> {
        let slot = this.#sources.get(ref);

        if (!slot) {
            slot = createSlot();
            this.#sources.set(ref, slot);
        }

        return slot;
    }
}

/** One dialog's registered source and the status derived from it. */
function createSlot() {
    const source = signal<Signal<DotTitleStatus> | null>(null);

    return { source, status: computed<DotTitleStatus>(() => source()?.() ?? null) };
}
