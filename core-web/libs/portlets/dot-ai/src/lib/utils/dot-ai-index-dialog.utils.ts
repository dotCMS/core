import { DOCUMENT } from '@angular/common';
import {
    computed,
    DestroyRef,
    effect,
    inject,
    Signal,
    signal,
    untracked,
    WritableSignal
} from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotAiIndexNotice, DotAiIndexOperation } from '../models/dot-ai-portlet.models';

export interface DotAiIndexOperationDialog {
    /** A request this dialog submitted is outstanding. */
    readonly $submitting: Signal<boolean>;
    /** The outcome of *this dialog's* request, while it is still this dialog's to show. */
    readonly $notice: Signal<DotAiIndexNotice | null>;
    /** Call as the request goes out, with the index it is for. */
    readonly submitted: (indexName: string) => void;
}

/**
 * The half of an index dialog that watches for its own outcome.
 *
 * Both dialogs need identically subtle behaviour — settle on my outcome, close on my success,
 * stay open on anything I can still correct — and writing it twice is what let them drift:
 * one filtered by operation and the other did not, so any operation's result cleared the
 * build dialog's spinner mid-build.
 *
 * Matching on `indexName` as well as operation is what makes "my own" mean anything. Both
 * store methods are `rxMethod`s on the shell-scoped store, so they outlive the dialog that
 * started them: abandon a slow build and open the dialog again, and without this the second
 * dialog closes on the first one's success and reports an index the user never asked for.
 *
 * While a request is outstanding the dialog cannot be dismissed at all. The answer is coming
 * back *into this form* — a rejected query belongs in the field that produced it — so letting
 * it be closed first is what created the abandoned-mid-flight case in the first place.
 *
 * The header X is a live binding off `DynamicDialogConfig`, so hiding it is a matter of
 * setting `closable`. Escape is not: PrimeNG binds a document listener **once**, when the
 * dialog opens, and never rereads the flag — so both dialogs open with `closeOnEscape: false`
 * and Escape is handled here instead. It still closes the dialog, as the portlet guide
 * requires; it just declines to while the server is mid-answer.
 */
export function watchIndexOperation(
    operation: DotAiIndexOperation,
    deps: {
        notice: Signal<DotAiIndexNotice | null>;
        close: () => void;
        config: DynamicDialogConfig;
        /** Declares to the tab that this dialog will render the outcome itself. */
        claim: (operation: DotAiIndexOperation, indexName: string) => void;
        release: () => void;
    }
): DotAiIndexOperationDialog {
    const $submitting = signal(false);
    const $target: WritableSignal<string | null> = signal(null);

    /**
     * Shows or hides PrimeNG's header X.
     *
     * `closable` is a plain property the dialog host reads in a template binding, so it lands
     * on that host's next change-detection pass. Locking gets one for free — it runs from a
     * click handler — but the unlock runs inside an effect, already part of the pass in
     * progress, so the X comes back on the user's next interaction rather than instantly. A
     * `setTimeout` was tried and changes nothing, so it is not carried here for the illusion.
     *
     * The gap is cosmetic and self-healing: it only occurs on a failed or empty outcome, which
     * is exactly when the user is about to type in the field again, and Cancel and Escape are
     * both live throughout.
     */
    const dismissable = (allowed: boolean) => {
        deps.config.closable = allowed;
    };

    const documentRef = inject(DOCUMENT);
    const unbindEscape = () => documentRef.removeEventListener('keydown', onEscape);

    function onEscape(event: KeyboardEvent) {
        // `defaultPrevented` leaves nested overlays — a select, a picker — to close themselves
        // first. PrimeNG's own handler is gone (`closeOnEscape: false`), because it binds once
        // at open and would not stand down mid-request.
        if (event.key !== 'Escape' || event.defaultPrevented || $submitting()) {
            return;
        }

        // Unbound here rather than at teardown: DynamicDialog is destroyed only after its
        // leave animation, and a live handler on a dialog already closing would close it twice.
        unbindEscape();
        deps.close();
    }

    documentRef.addEventListener('keydown', onEscape);
    inject(DestroyRef).onDestroy(() => {
        unbindEscape();
        deps.release();
    });

    const $own = computed(() => {
        const notice = deps.notice();

        return notice?.operation === operation && notice.indexName === $target() ? notice : null;
    });

    effect(() => {
        const notice = $own();

        if (!notice) {
            return;
        }

        untracked(() => {
            $submitting.set(false);
            dismissable(true);

            // Only a real success dismisses the dialog. Everything else — a query that matched
            // nothing, a query the server rejected — is a correction to a field still on
            // screen, which is the whole reason the dialog owns its submit.
            if (notice.outcome === 'ok') {
                deps.close();
            }
        });
    });

    return {
        $submitting,
        $notice: computed(() => {
            const notice = $own();

            return notice && notice.outcome !== 'ok' ? notice : null;
        }),
        submitted: (indexName: string) => {
            $target.set(indexName);
            $submitting.set(true);
            dismissable(false);
            deps.claim(operation, indexName);
        }
    };
}
