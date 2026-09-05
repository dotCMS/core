import { Component, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSearchInputComponent } from '@dotcms/ui';

import { DotAiIndexCreateComponent } from './dot-ai-index-create/dot-ai-index-create.component';

import { DotAiStore } from '../../store/dot-ai.store';
import { estimateIndexCost } from '../../utils/dot-ai-index.utils';

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
        ToolbarModule,
        TableModule,
        TagModule,
        ButtonModule,
        SelectModule,
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
    protected readonly cost = estimateIndexCost;

    /** `table-layout: fixed` + full height keeps the empty state from collapsing the table. */
    protected readonly tablePt = {
        table: { style: 'table-layout: fixed' },
        wrapper: { style: 'height: 100%' }
    };

    protected readonly statusOptions = [
        { label: 'dotai.embeddings.filter.all', value: null },
        { label: 'dotai.embeddings.status.ready', value: DOT_AI_INDEX_STATUS.READY },
        { label: 'dotai.embeddings.status.building', value: DOT_AI_INDEX_STATUS.BUILDING }
    ];

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
            .onClose.pipe(take(1))
            .subscribe((result) => {
                if (!result) {
                    return;
                }

                if (result.mode === 'delete') {
                    this.store.deleteFromIndex({
                        indexName: result.indexName,
                        query: result.query
                    });

                    return;
                }

                this.store.buildIndex(result);
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
