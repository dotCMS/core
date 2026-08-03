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

import { finalize, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
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
 * Bulk action dialog for the current Content Drive selection, offered from one contentlet upward.
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
        DotMessagePipe,
        FormsModule,
        MessageModule,
        RadioButtonModule,
        SkeletonModule,
        TooltipModule
    ],
    providers: [DotWorkflowsActionsService, DotWorkflowActionsFireService, ConfirmationService],
    templateUrl: './dot-content-drive-action-center.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
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
