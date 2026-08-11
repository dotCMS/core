import { forkJoin } from 'rxjs';

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
import { ConfirmationService } from 'primeng/api';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { finalize, map, take } from 'rxjs/operators';

import { DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import {
    DotActionCenterScheme,
    DotActionCenterWorkflowAction,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveActionPreviewComponent } from './components/dot-content-drive-action-preview/dot-content-drive-action-preview.component';

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
 * **Both sections commit the same way**: picking an action opens a **preview** screen (`$view`)
 * listing the contentlets it will run on with a checkbox each, and nothing is sent until Execute.
 *
 * This used to be workflow-only, on the reasoning that a quick action's count is derived from the
 * rows themselves and so "which items is this about to touch?" had an obvious answer. That confused
 * *knowing* the answer with *being able to change it*. The set is knowable, but the user still had no
 * way to narrow it — clicking Publish (12) published twelve items with no chance to drop one. The
 * preview is worth most on Unlock, where the row warns that some locks belong to other users and the
 * only way to act on that warning is to uncheck those rows.
 *
 * What still differs is what the count means. A quick action's count and its preview rows are the
 * same client-side filter, so they always agree. A workflow action's count comes from the backend and
 * can be lower than the rows shown, which is why only that path renders the partial-match warning.
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
    providers: [DotWorkflowsActionsService, ConfirmationService],
    templateUrl: './dot-content-drive-action-center.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex min-h-0 flex-1 flex-col'
    },
    styles: [
        `
            /*
             * Folder notice is present at open, so PrimeNG Message's hardcoded enter/leave height
             * animation (no API opt-out) reads as a late shove of the action list — kill both via CSS
             * on \`.no-enter-motion\`; \`:host ::ng-deep\` so we don't rely on \`_ngcontent\` piercing.
             */
            :host ::ng-deep p-message.no-enter-motion.p-message-enter-active,
            :host ::ng-deep p-message.no-enter-motion.p-message-leave-active {
                animation: none;
            }
        `
    ]
})
export class DotContentDriveActionCenterComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #workflowsActionsService = inject(DotWorkflowsActionsService);
    readonly #confirmationService = inject(ConfirmationService);

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
        header: {
            padding: '1rem',
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
    /**
     * True while an action is being fired; disables the whole dialog.
     *
     * Read from the store rather than held locally, because the run outlives this component: closing
     * the dialog mid-flight destroys it, and reopening must still report the run as in progress. A
     * local signal would reset to `false` on the new instance and let the same action be fired twice
     * over the same rows.
     */
    protected readonly $executing = computed(() => !!this.#store.actionExecution());
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
    /**
     * The quick action drilled into, or `null` when the preview belongs to a workflow action.
     *
     * Doubles as the discriminator for the whole preview screen: which items it lists, what Execute
     * fires, and whether the partial-match warning applies.
     */
    protected readonly $pendingQuickAction = signal<DotActionCenterQuickAction | null>(null);

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
    protected readonly $previewItems = computed(() => {
        const quickAction = this.$pendingQuickAction();

        if (quickAction) {
            // Filtered against the action's own `eligibleInodes` rather than re-deriving the
            // predicate, so the rows shown are exactly the set the row's count was built from.
            const eligible = new Set(quickAction.eligibleInodes);

            return this.$contentlets().filter((item) => eligible.has(item.inode));
        }

        return eligibleContentlets(this.$selectedAction(), this.$contentlets());
    });

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
     * Drills into a quick action's preview, listing the contentlets it applies to.
     *
     * Nothing is sent here — this only opens the screen where the user confirms or narrows the set.
     * The starting rows are `eligibleInodes`, the same set the row's count is derived from, so the
     * preview can never open on items the row never claimed: a Publish showing "(1)" lists one.
     *
     * @param quickAction - The quick action chosen by the user
     */
    protected onSelectQuickAction(quickAction: DotActionCenterQuickAction): void {
        // `pendingHint` marks an action with no working implementation yet (Add to Bundle needs a
        // bundle picker). The row is disabled, but guard here too so it can never open.
        if (!quickAction.count || quickAction.pendingHint) {
            return;
        }

        this.$pendingQuickAction.set(quickAction);
        // Keeps the two paths mutually exclusive: a workflow radio left armed from an earlier visit
        // must not decide what Execute fires now.
        this.$selectedActionId.set(null);

        const previewItems = this.$previewItems();

        this.$includedItems.set(previewItems);
        this.$view.set('preview');
        this.publishDrillDownHeader(
            // Quick action names are i18n keys, unlike workflow actions which arrive pre-translated.
            this.#dotMessageService.get(quickAction.name),
            previewItems.length
        );
    }

    /**
     * Commits whatever the preview is showing — the single Execute path for both sections.
     *
     * Both branches send `$includedItems`, so unchecking a row is honoured no matter which kind of
     * action opened the screen.
     */
    protected onExecutePreview(): void {
        const quickAction = this.$pendingQuickAction();

        if (quickAction) {
            this.executeQuickAction(quickAction);

            return;
        }

        this.onExecuteWorkflowAction();
    }

    /**
     * Fires a quick action over the rows left checked, prompting first when it warrants one.
     *
     * The prompt sits here rather than on the row click because this is the commit point: opening a
     * preview changes nothing, so confirming there would ask about a decision not yet made.
     */
    private executeQuickAction(quickAction: DotActionCenterQuickAction): void {
        const inodes = this.$includedItems().map((item) => item.inode);

        if (!inodes.length) {
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
        this.#store.executeQuickAction(
            quickAction.id,
            this.#dotMessageService.get(quickAction.name),
            inodes
        );
        this.handOffToToolbar();
    }

    /**
     * Closes the dialog the moment a run is handed to the store.
     *
     * The store owns the request now, so keeping the dialog open buys nothing and costs everything:
     * it is modal, so it dims the toolbar that is reporting the run, and it blocks the grid while
     * work happens that no longer needs the dialog to be alive. Closing here is what makes the
     * toolbar indicator observable — otherwise the only window to see it is the milliseconds between
     * the user manually closing the dialog and the request settling.
     *
     * Counts are also stale from this point on: the contentlets are moving to a new step, so the
     * numbers this dialog is showing no longer hold.
     */
    private handOffToToolbar(): void {
        this.#store.closeDialog();
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

        if (!action) {
            return;
        }

        // Cleared *before* reading `$previewItems`, which is discriminated on it: reading first
        // would resolve the rows against a stale quick action.
        this.$pendingQuickAction.set(null);

        const previewItems = this.$previewItems();

        if (!previewItems.length) {
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
        this.$pendingQuickAction.set(null);
        this.#store.clearDialogDrillDown();
    }

    /** Tracks the preview's checked rows, keeping the dialog header's count in step. */
    protected onIncludedItemsChange(items: DotCMSContentlet[]): void {
        this.$includedItems.set(items);

        const quickAction = this.$pendingQuickAction();

        if (quickAction) {
            this.publishDrillDownHeader(
                this.#dotMessageService.get(quickAction.name),
                items.length
            );

            return;
        }

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

        this.#store.executeWorkflowAction(workflowActionId, actionName, contentletIds);
        this.handOffToToolbar();
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
}
