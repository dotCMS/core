import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_INDEX_OPERATION } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

/**
 * Removes the embeddings for content matching a query, from one index.
 *
 * Opened from the row of the index it acts on, so the index is context rather than an input.
 * It used to be a mode on the build dialog, where the name was a free-text field: a typo
 * removed nothing and a near-miss hit a different index, both in silence.
 *
 * Owns its submit for the same reason the build dialog does — a rejected query is a correction
 * to the field still on screen — and stays open when the query matched nothing, which the
 * server reports as a perfectly successful `deleted: 0`.
 */
@Component({
    selector: 'dot-ai-index-remove-content',
    imports: [FormsModule, ButtonModule, TextareaModule, MessageModule, DotMessagePipe],
    templateUrl: './dot-ai-index-remove-content.component.html'
})
export class DotAiIndexRemoveContentComponent {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<{ indexName: string }>);

    protected readonly store = inject(DotAiStore);

    protected readonly indexName = this.#config.data?.indexName ?? '';

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

    protected readonly $canSubmit = computed(() => !!this.$query().trim() && !this.$submitting());

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
            indexName: this.indexName,
            query: this.$query().trim()
        });
    }

    protected cancel(): void {
        this.#dialogRef.close();
    }
}
