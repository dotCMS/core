import { computed, effect, Signal, signal, untracked, WritableSignal } from '@angular/core';

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
 */
export function watchIndexOperation(
    operation: DotAiIndexOperation,
    deps: { notice: Signal<DotAiIndexNotice | null>; close: () => void }
): DotAiIndexOperationDialog {
    const $submitting = signal(false);
    const $target: WritableSignal<string | null> = signal(null);

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
        }
    };
}
