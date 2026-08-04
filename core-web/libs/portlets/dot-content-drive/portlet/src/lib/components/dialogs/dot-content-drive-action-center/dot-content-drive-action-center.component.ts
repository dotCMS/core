import { forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
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
import { ConfirmationService, MessageService } from 'primeng/api';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { finalize, map, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotActionCenterScheme,
    DotActionCenterWorkflowAction,
    DotActionBulkRequestOptions,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveActionPreviewComponent } from './components/dot-content-drive-action-preview/dot-content-drive-action-preview.component';

import { SUCCESS_MESSAGE_LIFE } from '../../../shared/constants';
import { DotContentDriveStatus } from '../../../shared/models';
import { DotContentDriveStore } from '../../../store/dot-content-drive.store';
import {
    DotActionCenterQuickAction,
    eligibleContentlets,
    excludeFolders,
    getQuickActions,
    groupByContentType,
    mergeActionCenterSchemes
} from '../../../utils/action-center';

/** The two screens the dialog switches between. */
type DotActionCenterView = 'actions' | 'preview';

/**
 * Bulk action dialog for the current Content Drive selection, offered from one contentlet upward.
 *
 * Two sections:
 *
 * 1. **Quick Actions** — system actions fired over the whole eligible selection in one request via
 *    `POST /api/v1/workflow/actions/default/fire/{systemAction}`. Counts are derived client-side
 *    from row state (see `getQuickActions`).
 * 2. **Workflow Actions** — one collapsible panel per workflow scheme, from
 *    `POST /api/v1/workflow/contentlet/actions/bulk`, queried **once per content type** in the
 *    selection (see {@link loadWorkflowActions}). Counts come from the backend's Elasticsearch
 *    aggregation on `wfstep` and are real per-action eligibility counts.
 *
 * The two sections differ in how they commit. A quick action fires on click, over exactly the
 * contentlets its count was derived from. A workflow action goes through a **preview** screen first
 * (`$view`), listing the contentlets with a checkbox each, so the payload can be trimmed before it is
 * sent. Only workflow actions need this: their counts come from the backend and can be lower than the
 * selection, so "which items is this about to touch?" is a real question there and not for quick
 * actions.
 *
 * The preview retitles the shell's dialog header to the action name through the store's drill-down
 * state, rather than rendering a second header of its own.
 *
 * Deliberate v1 scope limits:
 *
 * - **One action per execute.** No endpoint fires multiple different actions in one call, and firing
 *   one action moves contentlets to a new step — which invalidates the other actions' counts. The
 *   legacy JSP dialog has the same constraint (one button, one fire).
 * - **Actions needing extra input are disabled** (`requiresInput`): push-publish settings, a move
 *   target path, or an assign/comment prompt. Wiring those means reusing
 *   `DotWorkflowEventHandlerService`, which is out of scope here.
 * Renders inside the shell's shared dialog rather than owning one, so there is a single dialog and a
 * single open/close path. The shell sizes this type's content box as a flex column; this component
 * fills it with a pinned summary, a scrolling body and a pinned footer.
 *
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
        ConfirmDialogModule,
        DotContentDriveActionPreviewComponent,
        DotMessagePipe,
        FormsModule,
        MessageModule,
        RadioButtonModule,
        SkeletonModule,
        TooltipModule
    ],
    providers: [DotWorkflowsActionsService, DotWorkflowActionsFireService, ConfirmationService],
    templateUrl: './dot-content-drive-action-center.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Sit in the shell's flex content box: without this the host ignores `flex-1`/`min-h-0` and
    // the inner column grows with its content, pushing the footer out of view.
    host: {
        class: 'flex min-h-0 flex-1 flex-col'
    }
})
export class DotContentDriveActionCenterComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #workflowsActionsService = inject(DotWorkflowsActionsService);
    readonly #workflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);

    protected readonly $selectedItems = this.#store.selectedItems;

    /**
     * Required for a collapsed accordion panel to actually take up no space.
     *
     * PrimeNG 21 collapses accordion content by animating the motion wrapper's
     * `grid-template-rows` to `0fr`, but that wrapper hides with `hideStrategy: 'visibility'` and
     * stays mounted (`unmountOnLeave: false`), so it keeps its layout box. Without `overflow: hidden`
     * the content overflows the zero-height row and a collapsed panel still reserves its full
     * height. Same workaround as `dot-page-scanner-a11y-report`.
     */
    protected readonly accordionPt = {
        motion: {
            root: {
                style: {
                    overflow: 'hidden'
                }
            }
        }
    };

    /**
     * Panel styling has to go on `p-accordion-panel`'s `pt.root`: Accordion's PassThrough
     * only exposes `root`/`motion`, and `p-accordion-panel` only accepts a `value` input, so a
     * `class`/`styleClass` attribute on it is inert. `overflow-hidden` clips header/content
     * corners to the panel radius.
     */
    protected readonly accordionPanelPt = {
        root: {
            class: 'rounded-xl overflow-hidden'
        }
    };

    /**
     * Accordion design tokens.
     *
     * The border has to come from `dt` rather than a Tailwind utility: the theme ships
     * `accordion.panel.border.width` as `0 0 1px 0` (inter-panel dividers for a single stacked
     * accordion) and its runtime-injected CSS wins over a `border` utility class. Overriding the
     * token gives each scheme a full border, which is what the design shows — the panels here are
     * separate cards with a gap, not a stack.
     *
     * Content padding is zeroed so the rows own their own insets (full-bleed dividers), and the
     * content's top border supplies the divider under an expanded header.
     */
    protected readonly accordionDt = {
        panel: {
            borderWidth: '1px',
            borderColor: '{surface.200}'
        },
        // Matches the Quick Actions list: the row sits on `surface-50` and lifts to white on hover,
        // and an expanded scheme stays white so it reads as the active card.
        header: {
            background: '{surface.50}',
            hoverBackground: '{surface.0}',
            activeBackground: '{surface.0}',
            activeHoverBackground: '{surface.0}'
        },
        content: {
            padding: '0',
            borderWidth: '1px 0 0 0',
            borderColor: '{surface.100}',
            background: '{surface.0}'
        }
    };

    /** Workflow schemes with a flat action list, loaded from the bulk-actions endpoint. */
    protected readonly $schemes = signal<DotActionCenterScheme[]>([]);
    /** True while the bulk-actions lookup is in flight. */
    protected readonly $loadingSchemes = signal<boolean>(true);
    /** True when the lookup failed — the workflow section renders an inline error. */
    protected readonly $schemesError = signal<boolean>(false);
    /** The single workflow action currently selected, across every scheme. */
    protected readonly $selectedActionId = signal<string | null>(null);
    /**
     * The expanded scheme panel. Single-expand, matching the prototype: opening one scheme collapses
     * the others, which also keeps the "one action per execute" rule visually obvious.
     */
    protected readonly $openSchemeId = signal<string | undefined>(undefined);
    /** True while an action is being fired; disables the whole dialog. */
    protected readonly $executing = signal<boolean>(false);
    /**
     * Which screen is showing. Quick actions never leave `'actions'`; picking a workflow action and
     * continuing swaps to `'preview'`.
     */
    protected readonly $view = signal<DotActionCenterView>('actions');
    /**
     * The contentlets still checked in the preview — exactly what gets fired.
     *
     * Separate from the grid selection on purpose: unchecking a row here must not deselect it in the
     * grid behind the dialog.
     */
    protected readonly $includedItems = signal<DotCMSContentlet[]>([]);

    /** Contentlets in the selection — folders are ignored by every bulk endpoint. */
    protected readonly $contentlets = computed(() => excludeFolders(this.$selectedItems()));
    protected readonly $contentletCount = computed(() => this.$contentlets().length);
    /** Number of folders silently excluded, surfaced as a hint so the count is not confusing. */
    protected readonly $ignoredFolderCount = computed(
        () => this.$selectedItems().length - this.$contentletCount()
    );
    protected readonly $quickActions = computed<DotActionCenterQuickAction[]>(() =>
        // Fed the already-filtered contentlets rather than the raw selection, so folder exclusion is
        // derived once here instead of again inside the util.
        getQuickActions(this.$contentlets())
    );

    /** Number of contentlets still checked in the preview. */
    protected readonly $includedCount = computed(() => this.$includedItems().length);

    /**
     * The single armed workflow action, resolved across every scheme.
     *
     * Used by the preview header, the Execute label and the result toast, so the lookup happens once.
     */
    protected readonly $selectedAction = computed<DotActionCenterWorkflowAction | undefined>(() => {
        const selectedId = this.$selectedActionId();

        if (!selectedId) {
            return undefined;
        }

        return this.$schemes()
            .flatMap((scheme) => scheme.actions)
            .find((action) => action.id === selectedId);
    });

    /**
     * The contentlets the selected action can run on — the preview's rows.
     *
     * Narrowed by content type so an action from one scheme never lists contentlets of a type that
     * scheme is not assigned to.
     */
    protected readonly $previewItems = computed(() =>
        eligibleContentlets(this.$selectedAction(), this.$contentlets())
    );

    /** Number of rows the preview lists for the selected action. */
    protected readonly $previewCount = computed(() => this.$previewItems().length);

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
     * Hint shown on a quick action row. Empty for a row that can be used, so no tooltip appears.
     *
     * `pendingHint` wins over the not-applicable message: an action that cannot run at all yet
     * should say so rather than blame the current selection.
     */
    protected quickActionHint(quickAction: DotActionCenterQuickAction): string {
        if (quickAction.pendingHint) {
            return quickAction.pendingHint;
        }

        return quickAction.count === 0 ? 'content-drive.action-center.not-applicable' : '';
    }

    /**
     * Fires a system action over the contentlets it applies to, in one request.
     *
     * Only `eligibleInodes` are sent — the same set the row's count is derived from. Firing over the
     * whole selection instead would act on items the row never claimed: a Publish showing "(1)"
     * would publish two, and Delete would be attempted on contentlets that are not archived.
     *
     * @param quickAction - The quick action chosen by the user
     */
    protected onExecuteQuickAction(quickAction: DotActionCenterQuickAction): void {
        const inodes = quickAction.eligibleInodes;

        // `pendingHint` marks an action with no working implementation yet (Add to Bundle needs a
        // bundle picker). The row is disabled, but guard here too so it can never fire.
        if (!inodes.length || quickAction.pendingHint) {
            return;
        }

        if (quickAction.confirmMessage) {
            this.#confirmationService.confirm({
                message: this.#dotMessageService.get(quickAction.confirmMessage),
                header: this.#dotMessageService.get('content-drive.action-center.confirm.header'),
                acceptLabel: this.#dotMessageService.get('dot.common.yes'),
                rejectLabel: this.#dotMessageService.get('dot.common.no'),
                accept: () => this.fireQuickAction(quickAction, inodes)
            });

            return;
        }

        this.fireQuickAction(quickAction, inodes);
    }

    /**
     * Fires a quick action over the given inodes. Split out of {@link onExecuteQuickAction} so the
     * confirmation branch and the direct branch share one execution path.
     */
    private fireQuickAction(quickAction: DotActionCenterQuickAction, inodes: string[]): void {
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
     * Opens the preview for the armed workflow action.
     *
     * Every contentlet starts included. There is nothing to pre-uncheck: the bulk-actions endpoint
     * returns per-action *counts*, never which contentlets each action matches, so the dialog cannot
     * tell which rows the server will skip. {@link $selectedAction}'s `count` is the honest signal
     * there, and the preview surfaces it when it falls short of the selection.
     */
    protected onContinueToPreview(): void {
        const action = this.$selectedAction();
        const previewItems = this.$previewItems();

        if (!action || !previewItems.length) {
            return;
        }

        this.$includedItems.set(previewItems);
        this.$view.set('preview');
        this.publishDrillDownHeader(action.name, previewItems.length);
    }

    /**
     * Returns to the action list.
     *
     * `$selectedActionId` is deliberately kept, so the radio is still armed on return and re-opening
     * the preview does not mean re-picking the action.
     */
    protected onBackToActions(): void {
        if (this.$executing()) {
            return;
        }

        this.$view.set('actions');
        this.$includedItems.set([]);
        this.#store.clearDialogDrillDown();
    }

    /** Tracks the preview's checked rows, keeping the dialog header's count in step. */
    protected onIncludedItemsChange(items: DotCMSContentlet[]): void {
        this.$includedItems.set(items);

        const action = this.$selectedAction();

        if (action) {
            this.publishDrillDownHeader(action.name, items.length);
        }
    }

    /**
     * Fires the selected workflow action over the contentlets left checked in the preview.
     *
     * Sends `$includedItems` rather than the whole selection, so the user's unchecking is honoured
     * and the payload matches what the preview showed. Contentlets whose scheme does not own the
     * action are still skipped server-side and reported back in `skippedCount`, which the result
     * toast surfaces — a mixed-type selection will partially skip by design.
     */
    protected onExecuteWorkflowAction(): void {
        const workflowActionId = this.$selectedActionId();
        const contentletIds = this.$includedItems().map((item) => item.inode);

        if (!workflowActionId || !contentletIds.length) {
            return;
        }

        const actionName = this.$selectedAction()?.name ?? workflowActionId;

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

    /**
     * Tracks which scheme panel is expanded, and clears the pending action when a different scheme
     * takes over so the Execute button can't stay armed for a panel the user has collapsed.
     */
    protected onOpenSchemeChange(value: string | number | string[] | number[] | undefined): void {
        const openId = Array.isArray(value) ? value[0]?.toString() : value?.toString();

        this.$openSchemeId.set(openId);

        const selectedId = this.$selectedActionId();
        const stillVisible = this.$schemes().some(
            (scheme) =>
                scheme.id === openId && scheme.actions.some((action) => action.id === selectedId)
        );

        if (!stillVisible) {
            this.$selectedActionId.set(null);
        }
    }

    /**
     * Closes the dialog without firing anything.
     *
     * X / ESC / mask closes are handled by the shell, which owns the shared dialog.
     */
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

    /**
     * Hands the shell the header for the drilled-into preview, so the dialog shows the action name
     * and the number of items it will run on instead of repeating the Workflow Center title.
     */
    private publishDrillDownHeader(header: string, itemCount: number): void {
        this.#store.setDialogDrillDown({ header, itemCount });
    }

    /**
     * Loads the available workflow actions for the current selection, **one request per content
     * type**.
     *
     * Grouping is what makes per-action eligibility knowable. A single lookup over a mixed selection
     * returns counts with no indication of which contentlets each action matches, so the preview
     * would list every selected item — a Blog action showing a VtlInclude row that the server was
     * always going to skip. Since schemes are assigned per content type, asking per type maps each
     * action back to the types that can run it.
     *
     * Cost is bounded by the number of distinct content types in the selection (typically one or
     * two), not by the number of contentlets. Requests run in parallel and a single failure fails the
     * lot, which is the same all-or-nothing behaviour as the previous one-request version.
     *
     * Sends inodes rather than a Lucene query. If Content Drive later supports selecting beyond the
     * current page, this is where the `{ query }` variant goes — the endpoint accepts either.
     */
    private loadWorkflowActions(): void {
        const groups = groupByContentType(this.$contentlets());

        if (!groups.length) {
            this.$loadingSchemes.set(false);

            return;
        }

        this.$loadingSchemes.set(true);
        this.$schemesError.set(false);

        forkJoin(
            groups.map((group) =>
                this.#workflowsActionsService
                    .getBulkActions({
                        contentletIds: group.contentlets.map((item) => item.inode)
                    })
                    .pipe(map((view) => ({ contentType: group.contentType, view })))
            )
        )
            .pipe(
                take(1),
                finalize(() => this.$loadingSchemes.set(false))
            )
            .subscribe({
                next: (results) => this.$schemes.set(mergeActionCenterSchemes(results)),
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

    /**
     * Routes execution failures through the shared error manager rather than a bespoke toast.
     *
     * This matters beyond convention here: per-item permission failures are an expected outcome of
     * these endpoints (the bulk-actions lookup filters by role permission only, not per contentlet),
     * and the error manager distinguishes 401/403 from a generic failure instead of flattening
     * everything into one message.
     */
    private onExecuteError(error: HttpErrorResponse): void {
        this.#httpErrorManagerService.handle(error);
    }
}
