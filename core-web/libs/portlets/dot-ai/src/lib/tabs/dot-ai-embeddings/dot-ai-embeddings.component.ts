import { Component, computed, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotSearchInputComponent,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotAiIndexCreateComponent } from './dot-ai-index-create/dot-ai-index-create.component';

import { DotAiStore } from '../../store/dot-ai.store';

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
        MessageModule,
        TableModule,
        TagModule,
        ButtonModule,
        ConfirmDialogModule,
        DotSearchInputComponent,
        DotMessagePipe
    ],
    providers: [ConfirmationService, DialogService],
    templateUrl: './dot-ai-embeddings.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiEmbeddingsComponent {
    protected readonly store = inject(DotAiStore);

    readonly #confirmationService = inject(ConfirmationService);
    readonly #dialogService = inject(DialogService);
    readonly #messageService = inject(DotMessageService);

    protected readonly statuses = DOT_AI_INDEX_STATUS;

    /**
     * Two different empty states behind one slot: an instance with no indexes at all, and a
     * filter that matched none of the ones there are. Telling someone to create their first
     * index when they have six and mistyped the filter is the wrong instruction.
     */
    protected readonly $emptyConfig = computed<PrincipalConfiguration>(() =>
        this.store.indexFilter().trim()
            ? {
                  title: this.#messageService.get('dotai.embeddings.no-matches'),
                  icon: 'filter_alt_off',
                  iconStyle: 'material-symbols-rounded'
              }
            : {
                  title: this.#messageService.get('dotai.embeddings.empty.title'),
                  subtitle: this.#messageService.get('dotai.embeddings.empty.sub'),
                  icon: 'database',
                  iconStyle: 'material-symbols-rounded'
              }
    );

    protected readonly forbiddenConfig: PrincipalConfiguration = {
        title: this.#messageService.get('dotai.index.admin-required'),
        subtitle: this.#messageService.get('dotai.index.admin-required.sub'),
        icon: 'lock',
        iconStyle: 'material-symbols-rounded'
    };

    /** Fixed layout plus full height keeps the empty state from collapsing the table. */
    protected readonly tablePt = {
        table: { class: 'table-fixed' },
        wrapper: { class: 'h-full' }
    };

    /**
     * Opens the build dialog and leaves it to it.
     *
     * No `onClose` handling any more: the dialog submits to the store itself so that a rejected
     * Lucene query can be corrected in the form that produced it, rather than being reported
     * onto this tab after the modal has closed over the query.
     */
    protected openCreateDialog(): void {
        this.store.dismissBuildNotice();

        this.#dialogService.open(DotAiIndexCreateComponent, {
            header: this.#messageService.get('dotai.index.create.header'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false
        });
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
