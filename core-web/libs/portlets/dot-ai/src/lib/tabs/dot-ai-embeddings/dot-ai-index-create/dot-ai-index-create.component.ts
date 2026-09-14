import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import { DOT_AI_INDEX_OPERATION } from '../../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../../store/dot-ai.store';
import { watchIndexOperation } from '../../../utils/dot-ai-index-dialog.utils';

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
     * Settles and closes on this dialog's own build, and nothing else. Shared with the remove
     * dialog because the two had already drifted apart once.
     */
    readonly #operation = watchIndexOperation(DOT_AI_INDEX_OPERATION.BUILD, {
        notice: this.store.indexNotice,
        close: () => this.#dialogRef.close()
    });

    protected readonly $submitting = this.#operation.$submitting;
    protected readonly $notice = this.#operation.$notice;

    protected readonly $indexName = signal('');
    protected readonly $query = signal('');
    protected readonly $fields = signal('');
    protected readonly $velocityTemplate = signal('');

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

        const indexName = this.$indexName().trim();

        this.#operation.submitted(indexName);
        this.store.buildIndex({
            indexName,
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
