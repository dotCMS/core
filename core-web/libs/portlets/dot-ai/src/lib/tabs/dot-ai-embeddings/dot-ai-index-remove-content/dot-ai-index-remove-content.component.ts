import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_INDEX_OPERATION } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

/**
 * Removes the embeddings for content matching a query, from one index.
 *
 * A sibling of the build dialog, not a row action. The two are the same shape of operation —
 * a Lucene query plus an index to scope it to, run against current content — and hanging this
 * one off a row implied it knew what was in that index. Nothing records the query that built
 * an index, so the query typed here is written blind either way; pretending otherwise was the
 * problem with putting it on the row.
 *
 * The index is picked from the real list rather than typed, which is the one thing the row
 * placement was right about: as a free-text field on the old delete mode, a typo removed
 * nothing and a near-miss hit a different index, both in silence.
 *
 * Owns its submit for the same reason the build dialog does — a rejected query is a correction
 * to the field still on screen — and stays open when the query matched nothing, which the
 * server reports as a perfectly successful `deleted: 0`.
 */
@Component({
    selector: 'dot-ai-index-remove-content',
    imports: [
        FormsModule,
        ButtonModule,
        TextareaModule,
        SelectModule,
        MessageModule,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-index-remove-content.component.html'
})
export class DotAiIndexRemoveContentComponent {
    readonly #dialogRef = inject(DynamicDialogRef);

    protected readonly store = inject(DotAiStore);

    /** Every index in the table, the cache pseudo-index included — it is removable like any other. */
    protected readonly $indexOptions = computed(() =>
        this.store.indexes().map((index) => index.name)
    );

    protected readonly $indexName = signal(this.store.indexes()[0]?.name ?? '');
    protected readonly $query = signal('');
    protected readonly $submitting = signal(false);

    /** This dialog's own outcomes, and only the ones it can do something about. */
    protected readonly $notice = computed(() => {
        const notice = this.store.indexNotice();

        return notice?.operation === DOT_AI_INDEX_OPERATION.REMOVE_CONTENT &&
            notice.outcome !== 'ok'
            ? notice
            : null;
    });

    protected readonly $canSubmit = computed(
        () => !!this.$indexName() && !!this.$query().trim() && !this.$submitting()
    );

    constructor() {
        effect(() => {
            const notice = this.store.indexNotice();

            if (notice?.operation !== DOT_AI_INDEX_OPERATION.REMOVE_CONTENT) {
                return;
            }

            untracked(() => {
                this.$submitting.set(false);

                // Only an actual removal closes this. "Nothing matched" leaves the query on
                // screen to be corrected, which is the whole reason the dialog owns the submit.
                if (notice.outcome === 'ok') {
                    this.#dialogRef.close();
                }
            });
        });

        // The tab reports whatever is still unrendered when this goes, so nothing is cleared
        // here — see DotAiEmbeddingsComponent. Kept to drop a notice the tab has no business
        // repeating once the user has already read it inline and dismissed the dialog.
        inject(DestroyRef).onDestroy(() => this.$submitting.set(false));
    }

    protected submit(): void {
        if (!this.$canSubmit()) {
            return;
        }

        this.$submitting.set(true);
        this.store.removeFromIndex({
            indexName: this.$indexName(),
            query: this.$query().trim()
        });
    }

    protected cancel(): void {
        this.#dialogRef.close();
    }
}
