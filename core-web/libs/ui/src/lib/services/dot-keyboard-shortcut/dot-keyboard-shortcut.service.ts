import { DOCUMENT } from '@angular/common';
import { DestroyRef, inject, Injectable } from '@angular/core';

import { DotKeyboardShortcut, DotKeyboardShortcutUnregister } from './models';

/** Modifier names understood in a combination string. `mod` is the platform's primary modifier. */
const MODIFIERS = ['mod', 'shift', 'alt'] as const;

/**
 * Normalises a combination so `shift+mod+k` and `mod+shift+k` are the same claim.
 * Modifiers are ordered canonically and the key comes last.
 */
function normalize(combination: string): string {
    const parts = combination
        .toLowerCase()
        .split('+')
        .map((part) => part.trim());
    const key = parts.filter((part) => !MODIFIERS.includes(part as never)).join('+');
    const modifiers = MODIFIERS.filter((modifier) => parts.includes(modifier));

    return [...modifiers, key].join('+');
}

/** Reads a keyboard event as the same canonical form a registration is normalised to. */
function combinationOf(event: KeyboardEvent): string {
    const modifiers: string[] = [];

    // Command and Control are deliberately conflated: a single `mod` claim then serves every
    // platform, and no shortcut in this application needs to tell them apart.
    if (event.metaKey || event.ctrlKey) {
        modifiers.push('mod');
    }

    if (event.shiftKey) {
        modifiers.push('shift');
    }

    if (event.altKey) {
        modifiers.push('alt');
    }

    return [...modifiers, event.key.toLowerCase()].join('+');
}

/**
 * Arbitrates keyboard shortcuts between surfaces that can be on screen at the same time.
 *
 * Root-provided rather than a store feature: this holds callbacks whose correct lifetime is that of
 * the surface that registered them, not application state. A root service gives the same single
 * shared instance to every portlet with none of the state semantics.
 *
 * **Arbitration is per-combination and last-in-wins.** Each combination keeps its own stack of
 * claims; the newest receives the key, and withdrawing it restores the one beneath. That is what
 * makes a dialog opening over a portlet take the search shortcut and hand it back on close, without
 * ever swallowing combinations it did not claim.
 *
 * **One listener, in the bubble phase.** A component that handles a key on its own element is closer
 * to the event target, runs first, and marks the event handled; the registry then ignores it. So
 * nesting is resolved by ordinary DOM semantics and the registry needs no knowledge of, say, rich
 * text surfaces. Listening in the capture phase would break this silently, which is why it must not
 * change.
 *
 * **Browser defaults are suppressed only for a combination something actually claims**, so the
 * registry can never eat a browser or assistive-technology key it does not own.
 */
@Injectable({ providedIn: 'root' })
export class DotKeyboardShortcutService {
    readonly #document = inject(DOCUMENT);

    /** Claim stacks keyed by normalised combination. The last entry is the newest and wins. */
    readonly #claims = new Map<string, DotKeyboardShortcut[]>();

    /** Bound once so attach and detach reference the same listener. */
    readonly #onKeyDown = (event: KeyboardEvent) => this.#dispatch(event);

    #listening = false;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.#detach());
    }

    /**
     * The listener exists only while something is claimed, so an application that registers no
     * shortcuts pays nothing and never appears in the document's listener list.
     */
    #attach(): void {
        if (this.#listening) {
            return;
        }

        // Bubble phase, deliberately. See the class comment.
        this.#document.addEventListener('keydown', this.#onKeyDown);
        this.#listening = true;
    }

    #detach(): void {
        if (!this.#listening) {
            return;
        }

        this.#document.removeEventListener('keydown', this.#onKeyDown);
        this.#listening = false;
    }

    /**
     * Claims one or more combinations for the calling surface.
     *
     * Accepts an array because a surface almost always wants several at once, and handing back a
     * separate withdrawal for each puts the burden of remembering all of them on every caller. One
     * call in, one withdrawal out, however many combinations are involved.
     *
     * @returns a function that withdraws everything registered by this call. Call it when the surface
     * is destroyed; it is safe to call more than once.
     */
    register(
        shortcuts: DotKeyboardShortcut | DotKeyboardShortcut[]
    ): DotKeyboardShortcutUnregister {
        const batch = Array.isArray(shortcuts) ? shortcuts : [shortcuts];

        // Duplicate *combinations* are deliberately allowed: the claim stack resolves them, and the
        // most recent claim wins. Duplicate *labels* have no such rule. The label is the key the
        // author-facing documentation is generated from, so two claims sharing one produce an
        // ambiguous entry that nothing in the model sorts out — and in a single call it is almost
        // always a registration that was copied and only half edited.
        //
        // Rejected before anything is registered, so a bad batch leaves no partial state behind.
        const seen = new Set<string>();

        for (const { label } of batch) {
            if (seen.has(label)) {
                throw new Error(
                    `DotKeyboardShortcutService: the label "${label}" is used twice in the same ` +
                        `register() call. Labels are what the shortcut documentation is generated ` +
                        `from, so each claim needs its own. (Claiming the same *combination* twice ` +
                        `is fine — the most recent claim wins.)`
                );
            }

            seen.add(label);
        }

        const withdrawals = batch.map((shortcut) => this.#registerOne(shortcut));

        let withdrawn = false;

        return () => {
            if (withdrawn) {
                return;
            }

            withdrawn = true;
            withdrawals.forEach((withdraw) => withdraw());
        };
    }

    /** Claims a single combination. Returns the withdrawal for that one claim. */
    #registerOne(shortcut: DotKeyboardShortcut): DotKeyboardShortcutUnregister {
        const combination = normalize(shortcut.combination);
        const claim: DotKeyboardShortcut = { ...shortcut, combination };
        const stack = this.#claims.get(combination) ?? [];

        stack.push(claim);
        this.#claims.set(combination, stack);
        this.#attach();

        let withdrawn = false;

        return () => {
            if (withdrawn) {
                return;
            }

            withdrawn = true;

            const current = this.#claims.get(combination);

            if (!current) {
                return;
            }

            // Spliced by identity rather than popped, so withdrawing an older claim out of order
            // leaves the newer one in charge.
            const index = current.indexOf(claim);

            if (index !== -1) {
                current.splice(index, 1);
            }

            if (!current.length) {
                this.#claims.delete(combination);
            }

            if (!this.#claims.size) {
                this.#detach();
            }
        };
    }

    /**
     * The shortcut that would receive each claimed combination **right now**, with its label.
     *
     * A runtime snapshot, not a manifest. It reports the winner of each claim stack, so a
     * combination shadowed by an open dialog is listed under the dialog's label and the surface
     * underneath does not appear at all. Anything generating author documentation from this must
     * read it with no dialogs open, or it will document whatever happened to be on screen.
     *
     * This is why a label is required at registration, and why labels must be unique within a single
     * `register()` call: a duplicate would collapse two shortcuts into one indistinguishable entry.
     */
    activeShortcuts(): { combination: string; label: string }[] {
        return Array.from(this.#claims.entries())
            .map(([combination, stack]) => ({
                combination,
                label: stack[stack.length - 1].label
            }))
            .filter((shortcut) => Boolean(shortcut.label));
    }

    #dispatch(event: KeyboardEvent): void {
        // Already handled by something closer to the event target.
        if (event.defaultPrevented) {
            return;
        }

        const stack = this.#claims.get(combinationOf(event));

        if (!stack?.length) {
            return;
        }

        // Newest first. A handler returning `false` declines and the key falls through; if every
        // claimant declines, the browser default stands untouched.
        for (let index = stack.length - 1; index >= 0; index--) {
            if (stack[index].handler(event) !== false) {
                event.preventDefault();

                return;
            }
        }
    }
}
