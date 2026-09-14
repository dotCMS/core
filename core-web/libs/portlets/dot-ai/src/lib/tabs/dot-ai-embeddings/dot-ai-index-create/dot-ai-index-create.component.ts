import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import { DotAiStore } from '../../../store/dot-ai.store';

type DotAiIndexCreateMode = 'add' | 'delete';

/**
 * The server stores whatever it is given — `bad name with spaces` is accepted verbatim, and
 * `Blog` and `blog` become two separate indexes. Neither is useful, so the name is constrained
 * here rather than left to produce an index nobody can find again.
 */
const INDEX_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

/**
 * One dialog, two modes.
 *
 * Add mode embeds the content the query matches; delete mode removes it from the index. The
 * submit label flips with the toggle so the destructive mode never hides behind a neutral
 * word (FR-030) — the legacy screen did the same remap.
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
        SelectButtonModule,
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

    protected readonly $mode = signal<DotAiIndexCreateMode>('add');
    protected readonly $indexName = signal('');
    protected readonly $query = signal('');
    protected readonly $fields = signal('');
    protected readonly $velocityTemplate = signal('');

    protected readonly modes = [
        { label: 'dotai.index.create.mode.add', value: 'add' as const },
        { label: 'dotai.index.create.mode.delete', value: 'delete' as const }
    ];

    protected readonly $submitLabel = computed(() =>
        this.$mode() === 'delete'
            ? 'dotai.index.create.submit.delete'
            : 'dotai.index.create.submit.add'
    );

    /**
     * The build outcome, while it is this dialog's to show.
     *
     * `built` is absent by construction — the effect below closes on it — so what is left is
     * exactly the two outcomes the user has to act on: a query that matched nothing, and a
     * query the server rejected.
     */
    protected readonly $notice = computed(() => {
        const notice = this.store.indexBuildNotice();

        return notice?.kind === 'built' ? null : notice;
    });

    constructor() {
        // A finished build is the only thing that dismisses this dialog; anything else is an
        // outcome the form still has to show. The success message is left standing on the tab
        // behind, next to the row it just created.
        effect(() => {
            const notice = this.store.indexBuildNotice();

            if (!notice) {
                return;
            }

            untracked(() => {
                this.$submitting.set(false);

                if (notice.kind === 'built') {
                    this.#dialogRef.close();
                }
            });
        });

        // On teardown, not in `cancel()`: PrimeNG's own header X and the Escape key call
        // `DynamicDialogRef.close` directly, so a notice dismissed that way would be left set
        // with nothing rendering it — the tab shows only `built`, and this dialog is gone.
        inject(DestroyRef).onDestroy(() => {
            if (this.store.indexBuildNotice()?.kind !== 'built') {
                this.store.dismissBuildNotice();
            }
        });
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

        // Only for a build: deleting names an index that must already exist.
        if (this.$mode() === 'add' && this.store.indexes().some((index) => index.name === name)) {
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
        const query = this.$query().trim();

        // A delete has no outcome to report back into the form — it either works or goes
        // through the shared error handler — so it keeps the original resolve-and-close shape.
        if (this.$mode() === 'delete') {
            this.store.deleteFromIndex({ indexName, query });
            this.#dialogRef.close();

            return;
        }

        this.$submitting.set(true);
        this.store.buildIndex({
            indexName,
            query,
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
