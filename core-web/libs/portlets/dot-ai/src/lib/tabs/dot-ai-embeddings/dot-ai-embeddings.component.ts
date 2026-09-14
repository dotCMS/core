import { Component, computed, effect, inject, signal, untracked } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotSearchInputComponent,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotAiIndexCreateComponent } from './dot-ai-index-create/dot-ai-index-create.component';
import { DotAiIndexRemoveContentComponent } from './dot-ai-index-remove-content/dot-ai-index-remove-content.component';

import {
    DOT_AI_INDEX_OPERATION,
    DotAiIndexNotice,
    DotAiIndexOperation
} from '../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../store/dot-ai.store';
import { toEmptyStateConfig } from '../../utils/dot-ai-empty-state.utils';

/**
 * Button treatment shared by both confirmations: a primary accept and an outlined cancel.
 *
 * Spread rather than repeated because these two drifted apart once already — the rebuild
 * dialog kept a filled primary cancel after the delete one moved to outlined, and both render
 * through the same `<p-confirmDialog>`, so the difference showed on one screen.
 *
 * Primary is the *absence* of a severity class: `.p-button` carries the primary styling
 * itself and the theme defines no `p-button-primary` to ask for — `p-button-secondary` exists
 * there, `p-button-primary` does not. Setting one would resolve to nothing.
 */
/**
 * The message key per operation and outcome.
 *
 * Spelled out rather than assembled from the operation name at call time: interpolated keys
 * are invisible to a grep, so a copy audit reports them as orphaned and a cleanup deletes
 * them, and a combination nobody defined renders its own key to the user instead of failing.
 * `Record` makes TypeScript require every one.
 */
const NOTICE_MESSAGE_KEYS: Record<
    DotAiIndexOperation,
    Record<DotAiIndexNotice['outcome'], string>
> = {
    [DOT_AI_INDEX_OPERATION.BUILD]: {
        ok: 'dotai.embeddings.build.ok',
        empty: 'dotai.embeddings.build.empty',
        failed: 'dotai.embeddings.build.failed'
    },
    [DOT_AI_INDEX_OPERATION.REMOVE_CONTENT]: {
        ok: 'dotai.embeddings.remove-content.ok',
        empty: 'dotai.embeddings.remove-content.empty',
        failed: 'dotai.embeddings.remove-content.failed'
    },
    [DOT_AI_INDEX_OPERATION.DELETE_INDEX]: {
        ok: 'dotai.embeddings.delete.ok',
        empty: 'dotai.embeddings.delete.ok',
        failed: 'dotai.embeddings.delete.failed'
    },
    [DOT_AI_INDEX_OPERATION.REBUILD_DB]: {
        ok: 'dotai.embeddings.rebuild.ok',
        empty: 'dotai.embeddings.rebuild.ok',
        failed: 'dotai.embeddings.rebuild.failed'
    }
} as const;

/** Toast severity per outcome, and the summary key that goes with it. */
const NOTICE_SEVERITY: Record<DotAiIndexNotice['outcome'], 'success' | 'warn' | 'error'> = {
    ok: 'success',
    empty: 'warn',
    failed: 'error'
} as const;

const CONFIRM_BUTTONS = {
    rejectButtonStyleClass: 'p-button-outlined'
} as const;

/**
 * Embeddings tab: the index inventory plus the operations on it.
 *
 * Filtering and sorting are entirely client-side, because `indexCount` returns every index in
 * one response with no query parameters — a `[lazy]` table or a debounced fetch here would be
 * inventing server capability that does not exist (FR-028).
 *
 * Both destructive actions go through a confirm dialog; the legacy screen used a browser
 * `confirm()` for the rebuild (FR-031, FR-032).
 */
@Component({
    selector: 'dot-ai-embeddings',
    imports: [
        DotEmptyContainerComponent,
        ToolbarModule,
        TableModule,
        TagModule,
        ToastModule,
        ButtonModule,
        ConfirmDialogModule,
        DotSearchInputComponent,
        DotMessagePipe
    ],
    providers: [ConfirmationService, DialogService, MessageService],
    templateUrl: './dot-ai-embeddings.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiEmbeddingsComponent {
    protected readonly store = inject(DotAiStore);

    readonly #confirmationService = inject(ConfirmationService);
    readonly #toast = inject(MessageService);
    readonly #dialogService = inject(DialogService);
    readonly #messageService = inject(DotMessageService);

    protected readonly statuses = DOT_AI_INDEX_STATUS;

    /** Whether a dialog is up, and so is the one rendering what the user must act on. */
    readonly #dialogOpen = signal(false);

    /** Identity guard: each operation makes a fresh notice, so this toasts each one once. */
    #toasted: DotAiIndexNotice | null = null;

    constructor() {
        // Every outcome is announced somewhere. A dialog that is still up owns anything the
        // user has to act on, because those are corrections to a field it is still holding;
        // everything else — successes, and failures whose dialog has gone — is a toast, which
        // is what the rest of the admin does. Nothing renders a standing banner on the tab.
        effect(() => {
            const notice = this.store.indexNotice();
            const dialogOpen = this.#dialogOpen();

            if (!notice || notice === this.#toasted) {
                return;
            }

            if (dialogOpen && notice.outcome !== 'ok') {
                // The dialog is showing it inline. Marked as reported all the same, so closing
                // the dialog does not then toast the error the user has just read and dismissed.
                this.#toasted = notice;

                return;
            }

            this.#toasted = notice;
            untracked(() => this.#toast.add(this.#toastFor(notice)));
        });
    }

    /**
     * Two different empty states behind one slot: an instance with no indexes at all, and a
     * filter that matched none of the ones there are. Telling someone to create their first
     * index when they have six and mistyped the filter is the wrong instruction.
     */
    protected readonly $emptyConfig = computed<PrincipalConfiguration>(() =>
        this.store.indexFilter().trim()
            ? toEmptyStateConfig(this.#messageService, {
                  title: 'dotai.embeddings.no-matches',
                  icon: 'filter_alt_off'
              })
            : toEmptyStateConfig(this.#messageService, {
                  title: 'dotai.embeddings.empty.title',
                  subtitle: 'dotai.embeddings.empty.sub',
                  icon: 'database'
              })
    );

    protected readonly forbiddenConfig = toEmptyStateConfig(this.#messageService, {
        title: 'dotai.index.admin-required',
        subtitle: 'dotai.index.admin-required.sub',
        icon: 'lock'
    });

    /** Fixed layout plus full height keeps the empty state from collapsing the table. */
    protected readonly tablePt = {
        table: { class: 'table-fixed' },
        wrapper: { class: 'h-full' }
    };

    /**
     * Opens the build dialog and leaves the submit to it.
     *
     * `onClose` carries no form value any more — the dialog submits to the store itself, so a
     * rejected Lucene query is corrected in the form that produced it rather than reported
     * onto this tab after the modal has closed over the query. It is still subscribed, for
     * the one thing this tab needs to know: whether the dialog is still there to do the
     * reporting.
     */
    protected openCreateDialog(): void {
        this.#dialogOpen.set(true);

        // `onDestroy`, not `onClose`: `close()` fires `onClose` immediately and only then
        // plays the leave animation, so the dialog component — and the `DestroyRef` hook that
        // clears an outcome it has already shown — lives on for the length of it. Handing over
        // at `onClose` would render that outcome on the tab for those frames before the
        // dialog's own teardown withdrew it. `onDestroy` fires once, on every close path
        // (cancel, success, Escape, the header X), after the dialog has let go.
        this.#dialogService
            .open(DotAiIndexCreateComponent, {
                header: this.#messageService.get('dotai.index.create.header'),
                width: '700px',
                closable: true,
                closeOnEscape: true,
                draggable: false
            })
            .onDestroy.pipe(take(1))
            .subscribe(() => this.#dialogOpen.set(false));
    }

    protected openRemoveContentDialog(): void {
        this.#dialogOpen.set(true);

        this.#dialogService
            .open(DotAiIndexRemoveContentComponent, {
                header: this.#messageService.get('dotai.embeddings.remove-content.header'),
                width: '700px',
                closable: true,
                closeOnEscape: true,
                draggable: false
            })
            // Same reason as the build dialog: `onDestroy` fires once, on every close path,
            // after the dialog has let go — `onClose` fires before the leave animation.
            .onDestroy.pipe(take(1))
            .subscribe(() => this.#dialogOpen.set(false));
    }

    /** The one place an outcome becomes words. Severity follows the outcome, not the action. */
    #toastFor(notice: DotAiIndexNotice): {
        severity: string;
        summary: string;
        detail: string;
        life: number;
    } {
        const severity = NOTICE_SEVERITY[notice.outcome];

        return {
            severity,
            summary: this.#messageService.get(`dotai.embeddings.toast.${severity}`),
            // Every notice key takes the same two, in the same order: the index, then the
            // count or the server's reason. Anything else and one mapper cannot serve them all.
            detail: this.#messageService.get(
                NOTICE_MESSAGE_KEYS[notice.operation][notice.outcome],
                notice.indexName,
                notice.detail ?? ''
            ),
            life: severity === 'success' ? 4000 : 8000
        };
    }

    protected confirmDeleteIndex(index: DotAiIndex): void {
        this.#confirmationService.confirm({
            ...CONFIRM_BUTTONS,
            header: this.#messageService.get('dotai.embeddings.delete.header'),
            message: this.#messageService.get('dotai.embeddings.delete.message', index.name),
            accept: () => this.store.deleteIndex(index.name)
        });
    }

    protected confirmRebuild(): void {
        this.#confirmationService.confirm({
            ...CONFIRM_BUTTONS,
            header: this.#messageService.get('dotai.embeddings.rebuild.header'),
            // The message is now the only thing conveying how final this is, the accept
            // button no longer being red. It states plainly that the store is discarded.
            message: this.#messageService.get('dotai.embeddings.rebuild.message'),
            accept: () => this.store.rebuildEmbeddingsDb()
        });
    }
}
