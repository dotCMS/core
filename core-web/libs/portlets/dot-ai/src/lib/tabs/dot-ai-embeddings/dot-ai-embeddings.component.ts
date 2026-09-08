import { Component, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSearchInputComponent } from '@dotcms/ui';

import {
    DotAiIndexCreateComponent,
    DotAiIndexCreateResult
} from './dot-ai-index-create/dot-ai-index-create.component';

import { DotAiEmptyStateComponent } from '../../components/dot-ai-empty-state/dot-ai-empty-state.component';
import { DotAiIndexBuildNotice } from '../../models/dot-ai-portlet.models';
import { DotAiStore } from '../../store/dot-ai.store';

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
        DotAiEmptyStateComponent,
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

    /** Fixed layout plus full height keeps the empty state from collapsing the table. */
    protected readonly tablePt = {
        table: { class: 'table-fixed' },
        wrapper: { class: 'h-full' }
    };

    /** p-message severities for the three build outcomes. */
    protected noticeSeverity(kind: DotAiIndexBuildNotice['kind']): 'success' | 'warn' | 'error' {
        if (kind === 'built') {
            return 'success';
        }

        return kind === 'empty' ? 'warn' : 'error';
    }

    protected openCreateDialog(): void {
        this.#dialogService
            .open(DotAiIndexCreateComponent, {
                header: this.#messageService.get('dotai.index.create.header'),
                width: '700px',
                closable: true,
                closeOnEscape: true,
                draggable: false,
                data: { indexes: this.store.indexes().map((index) => index.name) }
            })
            // `DialogService.onClose` is `Observable<any>`, so the annotation here is what
            // makes the "mode must not travel any further" invariant below a compiler rule
            // rather than a convention.
            .onClose.pipe(take(1))
            .subscribe((result: DotAiIndexCreateResult | undefined) => {
                if (!result) {
                    return;
                }

                // `mode` picks the branch and must not travel any further: it is a dialog
                // concept, and EmbeddingsForm rejects the whole request with
                // "Unrecognized field 'mode'" rather than ignoring it.
                const { mode, ...form } = result;

                if (mode === 'delete') {
                    this.store.deleteFromIndex({
                        indexName: form.indexName,
                        query: form.query
                    });

                    return;
                }

                this.store.buildIndex(form);
            });
    }

    protected confirmDeleteIndex(index: DotAiIndex): void {
        this.#confirmationService.confirm({
            header: this.#messageService.get('dotai.embeddings.delete.header'),
            message: this.#messageService.get('dotai.embeddings.delete.message', index.name),
            acceptButtonStyleClass: 'p-button-danger',
            accept: () => this.store.deleteIndex(index.name)
        });
    }

    protected confirmRebuild(): void {
        this.#confirmationService.confirm({
            header: this.#messageService.get('dotai.embeddings.rebuild.header'),
            // States plainly that the store is discarded — this is not undoable.
            message: this.#messageService.get('dotai.embeddings.rebuild.message'),
            acceptButtonStyleClass: 'p-button-danger',
            accept: () => this.store.rebuildEmbeddingsDb()
        });
    }
}
