import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_INDEX_OPERATION } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';

/**
 * The server stores whatever it is given — `bad name with spaces` is accepted verbatim, and
 * `Blog` and `blog` become two separate indexes. Neither is useful, so the name is constrained
 * here rather than left to produce an index nobody can find again.
 */
const INDEX_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

/**
 * Builds an index, and only builds one.
 *
 * Removing content used to be a mode on this same form, which meant a destructive action sat
 * one click from a create action behind a toggle, sharing its submit button, reshaping the
 * fields underneath it. It has its own dialog now, opened from the row of the index it acts on.
 *
 * The dialog calls the store itself rather than closing with a form value for the list
 * component to submit. It has to: a build is the one action here whose *failure* is a
 * correction to the form — a malformed Lucene query — and resolving the dialog first threw
 * that message onto the tab behind a modal that had already taken the query with it. Owning
 * the submit is what lets the query survive its own error. See the documented exception under
 * CRUD Patterns in `libs/portlets/CLAUDE.md`.
 */
@Component({
    selector: 'dot-ai-index-create',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        MessageModule,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-index-create.component.html'
})
export class DotAiIndexCreateComponent {
    readonly #dialogRef = inject(DynamicDialogRef);

    protected readonly store = inject(DotAiStore);

    /**
     * A build is outstanding. Local rather than store state: it is this dialog's submit button
     * that it disables, and nothing outside this component ever reads it.
     */
    protected readonly $submitting = signal(false);

    protected readonly $indexName = signal('');
    protected readonly $query = signal('');
    protected readonly $fields = signal('');
    protected readonly $velocityTemplate = signal('');

    /**
     * The build outcome, while it is this dialog's to show.
     *
     * `built` is absent by construction — the effect below closes on it — so what is left is
     * exactly the two outcomes the user has to act on: a query that matched nothing, and a
     * query the server rejected.
     */
    protected readonly $notice = computed(() => {
        const notice = this.store.indexNotice();

        return notice?.operation === DOT_AI_INDEX_OPERATION.BUILD && notice.outcome !== 'ok'
            ? notice
            : null;
    });

    constructor() {
        // A finished build is the only thing that dismisses this dialog; anything else is an
        // outcome the form still has to show. The success message is left standing on the tab
        // behind, next to the row it just created.
        effect(() => {
            const notice = this.store.indexNotice();

            if (!notice) {
                return;
            }

            untracked(() => {
                this.$submitting.set(false);

                if (notice.operation === DOT_AI_INDEX_OPERATION.BUILD && notice.outcome === 'ok') {
                    this.#dialogRef.close();
                }
            });
        });

        // Nothing is cleared on teardown any more. A build abandoned with Escape or the
        // header X while still in flight used to fail into silence; the tab now toasts
        // whatever this dialog was not around to render, so the notice has to survive it.
        inject(DestroyRef).onDestroy(() => this.$submitting.set(false));
    }

    /** Empty until the field has been touched, so the form does not scold you on open. */
    protected readonly $nameError = computed<string | null>(() => {
        const name = this.$indexName().trim();

        if (!name) {
            return null;
        }

        if (!INDEX_NAME_PATTERN.test(name)) {
            return 'dotai.index.create.name.invalid';
        }

        if (this.store.indexes().some((index) => index.name === name)) {
            return 'dotai.index.create.name.exists';
        }

        return null;
    });

    protected readonly $canSubmit = computed(
        () =>
            !!this.$indexName().trim() &&
            !!this.$query().trim() &&
            !this.$nameError() &&
            !this.$submitting()
    );

    protected submit(): void {
        if (!this.$canSubmit()) {
            return;
        }

        this.$submitting.set(true);
        this.store.buildIndex({
            indexName: this.$indexName().trim(),
            query: this.$query().trim(),
            // Both only shape what gets embedded, so they are omitted rather than sent blank.
            ...(this.$fields().trim() ? { fields: this.$fields().trim() } : {}),
            ...(this.$velocityTemplate().trim()
                ? { velocityTemplate: this.$velocityTemplate().trim() }
                : {})
        });
    }

    protected cancel(): void {
        this.#dialogRef.close();
    }
}
