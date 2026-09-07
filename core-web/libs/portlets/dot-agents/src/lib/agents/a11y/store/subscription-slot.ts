import { Subscription } from 'rxjs';

/**
 * A single-occupancy slot for an in-flight subscription.
 *
 * The run store holds three long-lived streams (the primary scan / fix SSE, the
 * comparison LIVE scan, and the mid-fix rescan) that all need the same discipline:
 * starting a new one cancels whatever was already there, and every teardown path
 * has to both unsubscribe AND drop the reference. Written inline that was seven
 * scattered `sub?.unsubscribe(); sub = null;` pairs — each one a place to forget
 * half of it. Unsubscribing is what aborts the underlying `fetch`, so a missed
 * cancel leaks an open stream rather than merely a dead object.
 *
 * Not reactive state: nothing here is read by the UI.
 */
export class SubscriptionSlot {
    #sub: Subscription | null = null;

    /**
     * Take ownership of `next`, cancelling any subscription already in the slot —
     * so a burst of triggers can't stampede or let an older, slower response write
     * stale results after a newer one.
     */
    set(next: Subscription): void {
        this.cancel();
        this.#sub = next;
    }

    /** Cancel and clear whatever occupies the slot. Safe to call when empty. */
    cancel(): void {
        this.#sub?.unsubscribe();
        this.#sub = null;
    }
}
