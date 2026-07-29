import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { finalize, take } from 'rxjs/operators';

import {
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotActionCenterScheme, DotActionBulkRequestOptions } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { SUCCESS_MESSAGE_LIFE } from '../../../shared/constants';
import { DotContentDriveStatus } from '../../../shared/models';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';
import {
    DotActionCenterQuickAction,
    excludeFolders,
    getQuickActions,
    toActionCenterSchemes,
    toContentletInodes
} from '../../../utils/action-center';

/**
 * Bulk action dialog for a multi-item selection in Content Drive.
 *
 * Two sections:
 *
 * 1. **Quick Actions** — system actions fired over the whole eligible selection in one request via
 *    `POST /api/v1/workflow/actions/default/fire/{systemAction}`. Counts are derived client-side
 *    from row state (see `getQuickActions`).
 * 2. **Workflow Actions** — one collapsible panel per workflow scheme, from
 *    `POST /api/v1/workflow/contentlet/actions/bulk`. Counts come from the backend's Elasticsearch
 *    aggregation on `wfstep` and are real per-action eligibility counts.
 *
 * Deliberate v1 scope limits:
 *
 * - **One action per execute.** No endpoint fires multiple different actions in one call, and firing
 *   one action moves contentlets to a new step — which invalidates the other actions' counts. The
 *   legacy JSP dialog has the same constraint (one button, one fire).
 * - **Actions needing extra input are disabled** (`requiresInput`): push-publish settings, a move
 *   target path, or an assign/comment prompt. Wiring those means reusing
 *   `DotWorkflowEventHandlerService`, which is out of scope here.
 * - **Fires synchronously** via `bulkFire`. The legacy dialog uses the SSE endpoint
 *   (`_bulkfire`) to stream live progress counters; that is the better long-term path for large
 *   batches but needs an SSE shim, since native `EventSource` cannot POST a body.
 * - **Folders are ignored**, matching the endpoints, which take contentlet inodes only.
 */
@Component({
    selector: 'dot-content-drive-action-center',
    imports: [
        AccordionModule,
        BadgeModule,
        ButtonModule,
        DotMessagePipe,
        FormsModule,
        MessageModule,
        RadioButtonModule,
        SkeletonModule,
        TooltipModule
    ],
    providers: [DotWorkflowsActionsService, DotWorkflowActionsFireService],
    templateUrl: './dot-content-drive-action-center.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveActionCenterComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #workflowsActionsService = inject(DotWorkflowsActionsService);
    readonly #workflowActionsFireService = inject(DotWorkflowActionsFireService);

    protected readonly $selectedItems = this.#store.selectedItems;

    /** Workflow schemes with a flat action list, loaded from the bulk-actions endpoint. */
    protected readonly $schemes = signal<DotActionCenterScheme[]>([]);
    /** True while the bulk-actions lookup is in flight. */
    protected readonly $loadingSchemes = signal<boolean>(true);
    /** True when the lookup failed — the workflow section renders an inline error. */
    protected readonly $schemesError = signal<boolean>(false);
    /** The single workflow action currently selected, across every scheme. */
    protected readonly $selectedActionId = signal<string | null>(null);
    /** True while an action is being fired; disables the whole dialog. */
    protected readonly $executing = signal<boolean>(false);

    /** Contentlets in the selection — folders are ignored by every bulk endpoint. */
    protected readonly $contentlets = computed(() => excludeFolders(this.$selectedItems()));
    protected readonly $contentletCount = computed(() => this.$contentlets().length);
    /** Number of folders silently excluded, surfaced as a hint so the count is not confusing. */
    protected readonly $ignoredFolderCount = computed(
        () => this.$selectedItems().length - this.$contentletCount()
    );
    protected readonly $quickActions = computed<DotActionCenterQuickAction[]>(() =>
        getQuickActions(this.$selectedItems())
    );

    ngOnInit(): void {
        this.loadWorkflowActions();
    }

    /**
     * Returns the scheme that owns the currently selected action, if any. Used to enable only that
     * scheme's Execute button, keeping execution to one action at a time.
     */
    protected schemeOwnsSelection(scheme: DotActionCenterScheme): boolean {
        const selectedId = this.$selectedActionId();

        return !!selectedId && scheme.actions.some((action) => action.id === selectedId);
    }

    /**
     * Fires a system action over every eligible contentlet in one request.
     *
     * @param quickAction - The quick action chosen by the user
     */
    protected onExecuteQuickAction(quickAction: DotActionCenterQuickAction): void {
        const inodes = toContentletInodes(this.$selectedItems());

        if (!inodes.length) {
            return;
        }

        this.$executing.set(true);

        this.#workflowActionsFireService
            .fireDefaultAction({ action: quickAction.id, inodes })
            .pipe(
                take(1),
                finalize(() => this.$executing.set(false))
            )
            .subscribe({
                next: () =>
                    this.onExecuteSuccess(
                        this.#dotMessageService.get(quickAction.name),
                        inodes.length
                    ),
                error: (error) => this.onExecuteError(error)
            });
    }

    /**
     * Fires the selected workflow action over the selection.
     *
     * Contentlets whose scheme does not own the action are skipped server-side and reported back in
     * `skippedCount`, which the result toast surfaces — a mixed-type selection will partially skip
     * by design.
     */
    protected onExecuteWorkflowAction(): void {
        const workflowActionId = this.$selectedActionId();
        const contentletIds = toContentletInodes(this.$selectedItems());

        if (!workflowActionId || !contentletIds.length) {
            return;
        }

        const actionName = this.selectedActionName() ?? workflowActionId;

        this.$executing.set(true);

        this.#workflowActionsFireService
            .bulkFire(this.buildBulkRequest(workflowActionId, contentletIds))
            .pipe(
                take(1),
                finalize(() => this.$executing.set(false))
            )
            .subscribe({
                next: (result) =>
                    this.onExecuteSuccess(actionName, result?.successCount, result?.skippedCount),
                error: (error) => this.onExecuteError(error)
            });
    }

    /** Closes the dialog without firing anything. */
    protected onDone(): void {
        this.#store.closeDialog();
    }

    /**
     * Builds the bulk-fire payload.
     *
     * `additionalParams` is required by the request model but carries nothing here: actions that
     * need real parameters are disabled in this dialog, so the empty bags are never read by the
     * backend actionlets.
     */
    private buildBulkRequest(
        workflowActionId: string,
        contentletIds: string[]
    ): DotActionBulkRequestOptions {
        return {
            workflowActionId,
            contentletIds,
            additionalParams: {
                assignComment: { assign: '', comment: '' },
                pushPublish: {},
                additionalParamsMap: { _path_to_move: '' }
            }
        };
    }

    /** Label of the selected action, for the result message. */
    private selectedActionName(): string | undefined {
        const selectedId = this.$selectedActionId();

        return this.$schemes()
            .flatMap((scheme) => scheme.actions)
            .find((action) => action.id === selectedId)?.name;
    }

    /**
     * Loads the available workflow actions for the current selection.
     *
     * Sends inodes rather than a Lucene query. If Content Drive later supports selecting beyond the
     * current page, this is where the `{ query }` variant goes — the endpoint accepts either.
     */
    private loadWorkflowActions(): void {
        const contentletIds = toContentletInodes(this.$selectedItems());

        if (!contentletIds.length) {
            this.$loadingSchemes.set(false);

            return;
        }

        this.$loadingSchemes.set(true);
        this.$schemesError.set(false);

        this.#workflowsActionsService
            .getBulkActions({ contentletIds })
            .pipe(
                take(1),
                finalize(() => this.$loadingSchemes.set(false))
            )
            .subscribe({
                next: (view) => this.$schemes.set(toActionCenterSchemes(view)),
                error: () => {
                    this.$schemes.set([]);
                    this.$schemesError.set(true);
                }
            });
    }

    /**
     * Reports a successful execution, refreshes the grid, and closes the dialog.
     *
     * Counts go stale the moment an action runs — contentlets move to a new step — so the dialog
     * closes rather than showing numbers that no longer hold.
     */
    private onExecuteSuccess(
        actionName: string,
        successCount?: number,
        skippedCount?: number
    ): void {
        const detail =
            skippedCount && skippedCount > 0
                ? this.#dotMessageService.get(
                      'content-drive.action-center.toast.executed-with-skips',
                      actionName,
                      String(successCount ?? 0),
                      String(skippedCount)
                  )
                : this.#dotMessageService.get(
                      'content-drive.action-center.toast.executed-detail',
                      actionName,
                      String(successCount ?? 0)
                  );

        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('content-drive.action-center.toast.executed'),
            detail,
            life: SUCCESS_MESSAGE_LIFE
        });

        this.#store.setStatus(DotContentDriveStatus.LOADING);
        this.#store.setSelectedItems([]);
        this.#store.loadItems();
        this.#store.closeDialog();
    }

    private onExecuteError(error: unknown): void {
        this.#messageService.add({
            severity: 'error',
            summary: this.#dotMessageService.get('content-drive.action-center.toast.error'),
            detail: this.#dotMessageService.get('content-drive.action-center.toast.error-detail')
        });

        console.error('Action Center: failed to execute action', error);
    }
}
