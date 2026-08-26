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
import { MessageService } from 'primeng/api';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { finalize, map, take } from 'rxjs/operators';

import { DotMessageService, DotWorkflowsActionsService } from '@dotcms/data-access';
import {
    DotActionCenterScheme,
    DotActionCenterWorkflowAction,
    DotBundle,
    DotContentDriveItem
} from '@dotcms/dotcms-models';
import {
    DotMessagePipe,
    DotWorkflowAssignCommentComponent,
    DotWorkflowAssignCommentValue,
    DotWorkflowPushPublishComponent,
    DotWorkflowPushPublishValue
} from '@dotcms/ui';

import {
    DotContentDriveActionBundleTargetComponent,
    rememberLastBundleUsed
} from './components/dot-content-drive-action-bundle-target/dot-content-drive-action-bundle-target.component';
import { DotContentDriveActionMoveTargetComponent } from './components/dot-content-drive-action-move-target/dot-content-drive-action-move-target.component';
import { DotContentDriveActionPreviewComponent } from './components/dot-content-drive-action-preview/dot-content-drive-action-preview.component';

import { DotContentDriveStore } from '../../../store/dot-content-drive.store';
import {
    ADD_TO_BUNDLE_ACTION_ID,
    DotActionCenterQuickAction,
    PUSH_PUBLISH_ACTION_ID,
    REFRESH_ACTION_ID,
    DotActionInputKind,
    eligibleContentlets,
    excludeFolders,
    getQuickActions,
    supportsFolders,
    groupByContentType,
    isLockedByAnotherUser,
    mergeActionCenterSchemes,
    requiredInputKinds,
    toDistinctIdentifiers
} from '../../../utils/action-center';

/** The screens the dialog switches between. */
type DotActionCenterView = 'actions' | 'configure' | 'preview';

/**
 * A section the `configure` screen can render.
 *
 * `bundle` is the quick action's own kind; the rest mirror {@link DotActionInputKind}. An action can
 * need several, and they all render on one screen.
 */
type DotActionCenterConfigureKind = DotActionInputKind | 'bundle';

/**
 * Bulk action dialog for the current Content Drive selection, offered from one contentlet upward.
 *
 * Two sections:
 *
 * 1. **Quick Actions** — the bulk operations the old search toolbar offered outside its workflow
 *    dropdown: Lock, Unlock, Add to Bundle, Refresh, and a placeholder for Push Publish. Lock and
 *    Unlock fire over the whole eligible selection in one request via
 *    `POST /api/v1/workflow/actions/default/fire/{systemAction}`; Refresh goes to its own job-backed
 *    `POST /api/v1/content/_bulkrefresh`, and its completion is pushed over the websocket. Counts are derived client-side
 *    from row state (see `getQuickActions`).
 * 2. **Workflow Actions** — one collapsible panel per workflow scheme, from
 *    `POST /api/v1/workflow/contentlet/actions/bulk`, queried **once per content type** in the
 *    selection (see {@link loadWorkflowActions}). Counts come from the backend's Elasticsearch
 *    aggregation on `wfstep` and are real per-action eligibility counts.
 *
 * **Both sections commit the same way**: picking an action opens a **preview** screen (`$view`)
 * listing the contentlets it will run on with a checkbox each, and nothing is sent until Execute.
 *
 * An action that needs input first gets a **configuration** screen ahead of the preview, so the flow is
 * `pick → configure → preview → execute`. Every input the action declares renders as a section on that
 * one screen (see {@link $configureKinds}) rather than as a page each: an approval that assigns *and*
 * push-publishes would otherwise turn one bulk action into a five-screen flow. The preview deliberately
 * stays last, keeping the rows and the Execute button together as the final screen for every action.
 *
 * This used to be workflow-only, on the reasoning that a quick action's count is derived from the
 * rows themselves and so "which items is this about to touch?" had an obvious answer. That confused
 * *knowing* the answer with *being able to change it*. The set is knowable, but the user still had no
 * way to narrow it — clicking Unlock (12) unlocked twelve items with no chance to drop one. Unlock is
 * where the preview earns its place: the row warns that some locks belong to other users, and the only
 * way to act on that warning is to uncheck those rows.
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
 * - **Every workflow action is reachable.** No row is greyed for needing input any more; whatever the
 *   action declares in `actionInputs[]` gets a section on the configuration screen.
 *
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
        DotContentDriveActionBundleTargetComponent,
        DotContentDriveActionMoveTargetComponent,
        DotContentDriveActionPreviewComponent,
        DotMessagePipe,
        DotWorkflowAssignCommentComponent,
        DotWorkflowPushPublishComponent,
        FormsModule,
        MessageModule,
        RadioButtonModule,
        SkeletonModule,
        TooltipModule
    ],
    providers: [DotWorkflowsActionsService],
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
    /**
     * Resolves to the shell's instance, so a toast added here survives this dialog closing immediately
     * afterwards.
     */
    readonly #messageService = inject(MessageService);
    readonly #workflowsActionsService = inject(DotWorkflowsActionsService);

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
    /**
     * Whether any push publish environment is reachable by this user's role.
     *
     * Read from the store, resolved once on portlet init, for the same reason the admin flag is: the
     * folder context menu gates on this too, and two independent lookups would mean two copies of
     * the three-state handling to keep in step.
     *
     * `undefined` means the lookup has not landed and reads as "disabled", so the row never enables
     * and then retracts. A failed lookup settles on `false` for the same reason: offering a push
     * with nowhere to send it is worse than one disabled row.
     */
    protected readonly $hasPushPublishEnvironments = this.#store.hasPushPublishEnvironments;
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
     * Which screen is showing.
     *
     * Quick actions always go straight to `'preview'`; a workflow action goes through `'configure'`
     * first when it needs a target path, so the order is `pick → configure → preview → execute`.
     *
     * The configuration step sits *before* the preview rather than after it so the preview stays the
     * last thing seen before committing — the same position it holds for every other action, with the
     * checkbox list and the Execute button together. Putting it after would mean the user confirms a
     * set of rows and then leaves that screen to fill in a form, which reads as a second commit.
     */
    protected readonly $view = signal<DotActionCenterView>('actions');
    /**
     * The bulk move destination as `//hostname/path`, or `''` while nothing is chosen.
     *
     * Lives here rather than in the step component because it has to survive navigating forward to the
     * preview and back again — the step is destroyed by the `@switch` on `$view`, and a user who
     * returns to correct their selection should not find the picker reset.
     */
    protected readonly $pathToMove = signal<string>('');

    /**
     * The folder Content Drive is currently browsing, as `//hostname/path`.
     *
     * Seeds the destination picker so it opens on the current location instead of the bare site list.
     * The same string the store builds for its own search (`assetPath`), so the two cannot drift.
     *
     * Note this is the *browsing* path, not any contentlet's own folder: with a search or filter
     * applied the selection can span folders, and no single "current path" exists for it. As a place
     * to start navigating from it is right either way, which is all it is used for.
     */
    protected readonly $currentPath = computed(() => {
        const hostname = this.#store.currentSite()?.hostname;

        return hostname ? `//${hostname}${this.#store.path() || '/'}` : '';
    });

    /**
     * True when the destination is still the folder the picker opened on.
     *
     * Advisory only — it warns, it does not block. Seeding the picker means a destination is present
     * from the outset, and a move to where the items already are costs a version and a reindex each
     * for no change, which is worth flagging.
     *
     * It cannot be a gate, because it compares against the *browsing* path and the selection does not
     * have to live there. With a search or filter applied `path()` can be unset, making this the site
     * root — so gating on it refused a perfectly legitimate move of filtered results to the root.
     * Contentlets carry a folder inode but no path, so there is no client-side way to compare against
     * where the items actually are. Warning is the honest amount of certainty available here.
     */
    protected readonly $destinationUnchanged = computed(
        () => !!this.$pathToMove() && this.$pathToMove() === this.$currentPath()
    );

    /** The bundle chosen in the configuration step, or `null` while none is. */
    protected readonly $selectedBundle = signal<DotBundle | null>(null);

    /**
     * Assignee and comment collected for an assignable/commentable action.
     *
     * The step reports its own validity rather than this component re-deriving it: whether an assignee
     * is required depends on roles the step loaded, which only it knows.
     */
    protected readonly $assignComment = signal<DotWorkflowAssignCommentValue>({
        assign: '',
        comment: ''
    });
    protected readonly $assignCommentValid = signal<boolean>(false);

    /** Push publish settings, already in the shape the fire request wants. */
    protected readonly $pushPublish = signal<DotWorkflowPushPublishValue | null>(null);
    protected readonly $pushPublishValid = signal<boolean>(false);

    /**
     * Every configuration section the armed action needs, in render order. Empty when it fires straight
     * from the selection.
     *
     * One list for both action sources, so the `configure` view has a single discriminator rather than
     * the template asking two unrelated questions. All of them render together on one screen: an action
     * can declare several inputs (an approval that assigns *and* push-publishes), and paging them would
     * make a four- or five-screen flow out of one bulk action.
     */
    protected readonly $configureKinds = computed<DotActionCenterConfigureKind[]>(() => {
        const quickAction = this.$pendingQuickAction();

        if (quickAction) {
            switch (quickAction.id) {
                case ADD_TO_BUNDLE_ACTION_ID:
                    return ['bundle'];
                case PUSH_PUBLISH_ACTION_ID:
                    // The same section the workflow-action path renders, so the two dialogs collect a
                    // push publish the same way.
                    return ['pushPublish'];
                default:
                    return [];
            }
        }

        return requiredInputKinds(this.$selectedAction());
    });

    /** True when more than one section is on screen, which is what earns the dividers and headings. */
    protected readonly $hasMultipleSections = computed(() => this.$configureKinds().length > 1);

    /**
     * Distinct assets an Add to Bundle would queue.
     *
     * A bundle holds one entry per identifier, so language versions of a contentlet are one asset.
     */
    protected readonly $bundleAssetCount = computed(
        () => toDistinctIdentifiers(this.$includedItems()).length
    );

    /** Rows the identifier collapse absorbs, so the step can say so before the fact. */
    protected readonly $bundleCollapsedCount = computed(
        () => this.$includedCount() - this.$bundleAssetCount()
    );

    /**
     * Whether one section has everything it needs.
     *
     * Move and bundle are judged here because the dialog owns their values; assign/comment and push
     * publish report their own, since only they know what their loaded roles or environments make
     * required.
     */
    protected sectionIsSatisfied(kind: DotActionCenterConfigureKind): boolean {
        switch (kind) {
            case 'move':
                return !!this.$pathToMove();
            case 'bundle':
                return !!this.$selectedBundle();
            case 'assignComment':
                return this.$assignCommentValid();
            case 'pushPublish':
                return this.$pushPublishValid();
            default:
                return true;
        }
    }

    /**
     * Whether every section on screen has what it needs.
     *
     * Nothing to collect leaves this true, so an action with no inputs is never blocked.
     */
    protected readonly $canLeaveConfigure = computed(() =>
        this.$configureKinds().every((kind) => this.sectionIsSatisfied(kind))
    );

    /**
     * The hint for the first section still missing something, or `''` when nothing is.
     *
     * The cost of stacking sections is that an incomplete field can be scrolled out of view, leaving a
     * disabled Continue with no visible cause. Naming the first unsatisfied section in the footer is
     * what keeps that from being a dead end.
     */
    /**
     * Advisory shown when the chosen destination is the folder being browsed.
     *
     * Separate from {@link $configureHint}, which lists what is *missing*: this one accompanies a
     * perfectly valid choice that is probably not what the user meant.
     */
    protected readonly $configureWarning = computed(() =>
        this.$configureKinds().includes('move') && this.$destinationUnchanged()
            ? 'content-drive.action-center.move.same-destination'
            : ''
    );

    protected readonly $configureHint = computed(() => {
        const unsatisfied = this.$configureKinds().find((kind) => !this.sectionIsSatisfied(kind));

        switch (unsatisfied) {
            case 'move':
                return 'content-drive.action-center.move.no-destination';
            case 'bundle':
                return 'content-drive.action-center.bundle.no-target';
            case 'assignComment':
                return 'content-drive.action-center.assign.no-assignee';
            case 'pushPublish':
                return 'content-drive.action-center.push-publish.no-environment';
            default:
                return '';
        }
    });
    /**
     * The contentlets still checked in the preview — exactly what gets fired.
     *
     * Separate from the grid selection on purpose: unchecking a row here must not deselect it in the
     * grid behind the dialog.
     */
    protected readonly $includedItems = signal<DotContentDriveItem[]>([]);
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
    /**
     * Folders in the selection, surfaced as a hint so the per-action counts are not confusing.
     *
     * They are no longer excluded outright: Add to Bundle and Push Publish take a folder identifier,
     * and the rest of the actions drop themselves from the list instead. The notice says which,
     * rather than claiming folders are ignored.
     */
    protected readonly $selectedFolderCount = computed(
        () => this.$selectedItems().length - this.$contentletCount()
    );
    protected readonly $quickActions = computed<DotActionCenterQuickAction[]>(() =>
        // Fed the whole selection: folder exclusion is per action now, and `getQuickActions` owns
        // that decision from the registry. Pre-filtering here would hide folders from the two
        // actions that accept them.
        //
        // The admin flag comes from the store, resolved once on portlet init, rather than being
        // fetched when this dialog opens: reopening the Action Center is cheap and common, and a
        // per-open request would leave the first render of every open warning as a non-admin until
        // it answered. Read as a signal so a late resolution still recomputes the rows.
        getQuickActions(this.$selectedItems(), {
            isAdmin: this.#store.currentUserIsAdmin(),
            hasPushPublishEnvironments: this.$hasPushPublishEnvironments()
        })
    );

    /** Number of contentlets still checked in the preview. */
    protected readonly $includedCount = computed(() => this.$includedItems().length);

    /**
     * Label for the preview's back control, which names where it actually goes.
     *
     * "Back to actions" would be a lie on a move, where back lands on the destination picker.
     */
    protected readonly $backLabel = computed(() => {
        const kinds = this.$configureKinds();

        if (!kinds.length) {
            return 'content-drive.action-center.back';
        }

        // "Back to destination" only when the destination picker is the whole screen; anything else is
        // a form, or several, so the generic label is the honest one.
        return kinds.length === 1 && kinds[0] === 'move'
            ? 'content-drive.action-center.back.configure'
            : 'content-drive.action-center.back.settings';
    });

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
     * The items the selected action can run on — the preview's rows.
     *
     * Narrowed by content type so an action from one scheme never lists contentlets of a type that
     * scheme is not assigned to.
     */
    protected readonly $previewItems = computed(() => {
        const quickAction = this.$pendingQuickAction();

        if (quickAction) {
            // Filtered against the action's own `eligibleInodes` rather than re-deriving the
            // predicate, so the rows shown are exactly the set the row's count was built from.
            // That key is identifiers for the folder-capable actions and inodes for the rest, so the
            // row's own id has to be read the same way — matching a folder on `inode` would drop it
            // from the preview of the very action it is about to be fired on.
            const eligible = new Set(quickAction.eligibleInodes);
            // Resolved once: it is a registry lookup with the same answer for every row, and the
            // `filter` below would otherwise repeat it per item.
            const takesFolders = supportsFolders(quickAction.id);
            const pool = takesFolders ? this.$selectedItems() : this.$contentlets();

            return this.#inTableOrder(
                pool.filter((item) => eligible.has(takesFolders ? item.identifier : item.inode))
            );
        }

        return this.#inTableOrder(eligibleContentlets(this.$selectedAction(), this.$contentlets()));
    });

    /**
     * Reorders preview rows to match the table behind the dialog.
     *
     * Selection is stored exactly as PrimeNG hands it over (`setSelectedItems`), which is the order
     * rows were *ticked*, not the order they are listed in. A preview built straight off it comes
     * out shuffled relative to the grid the user was just reading, which makes a confirmation list
     * hard to check.
     *
     * Keyed on `inode`, which every row carries — folders included, since the drive-search response
     * is backfilled at the service boundary. Anything not found in the current page keeps its
     * relative position at the end rather than being dropped.
     */
    #inTableOrder<T extends DotContentDriveItem>(items: T[]): T[] {
        const order = new Map(this.#store.items().map((item, index) => [item.inode, index]));

        return [...items].sort(
            (a, b) =>
                (order.get(a.inode) ?? Number.MAX_SAFE_INTEGER) -
                (order.get(b.inode) ?? Number.MAX_SAFE_INTEGER)
        );
    }

    /** Number of rows the preview lists for the selected action. */
    protected readonly $previewCount = computed(() => this.$previewItems().length);

    /**
     * Inodes among the preview's rows whose lock belongs to another user, for the table to mark.
     *
     * Derived from `isLockedByAnotherUser` — the same predicate behind the Unlock row's
     * `warningCount` — so the number the row advertises and the rows marked here cannot disagree,
     * and an administrator sees neither.
     *
     * Applied to every action's preview, not just Unlock: a lock held by somebody else can fail any
     * action fired over that row, and it is worth flagging wherever the row is listed.
     */
    protected readonly $lockedByOthers = computed(() => {
        const context = { isAdmin: this.#store.currentUserIsAdmin() };

        // Narrowed first: a folder carries no lock state, and the predicate reads it.
        return excludeFolders(this.$previewItems())
            .filter((item) => isLockedByAnotherUser(item, context))
            .map((item) => item.inode);
    });

    ngOnInit(): void {
        this.loadWorkflowActions();
    }

    /**
     * Whether the armed action belongs to this scheme.
     *
     * Drives the panel header's "1 Selected" badge, which is what tells the user where their armed
     * action lives once they have scrolled or collapsed the panel — the footer's Continue says an
     * action is armed but not which panel holds it.
     */
    protected schemeOwnsSelection(scheme: DotActionCenterScheme): boolean {
        const selectedId = this.$selectedActionId();

        return !!selectedId && scheme.actions.some((action) => action.id === selectedId);
    }

    /**
     * Hint shown on a quick action row. Empty for a row that can be used, so no tooltip appears.
     *
     * `comingSoon` is checked first: those rows are disabled whatever their count says, so "not
     * applicable" would explain the wrong thing about them.
     */
    protected quickActionHint(quickAction: DotActionCenterQuickAction): string {
        if (quickAction.comingSoon) {
            return 'content-drive.action-center.coming-soon';
        }

        if (quickAction.missingEnvironments) {
            // Only once the lookup has answered. While it is in flight the row is disabled with no
            // tooltip, rather than blaming a configuration we have not checked yet.
            return this.$hasPushPublishEnvironments() === undefined
                ? ''
                : 'content-drive.action-center.no-environments';
        }

        if (quickAction.missingAdminRole) {
            // Names the requirement rather than the refusal: the row is out of reach because of who
            // is asking, and nothing about the selection will change that.
            return 'content-drive.action-center.requires-admin';
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
        // Guarded here as well as by the disabled row: a placeholder has no preview to open, a push
        // with no environment has nowhere to go, and a reindex the endpoint would refuse should not
        // get as far as a confirmation screen. A stray call must not reach the preview.
        if (
            !quickAction.count ||
            quickAction.comingSoon ||
            quickAction.missingEnvironments ||
            quickAction.missingAdminRole
        ) {
            return;
        }

        this.$pendingQuickAction.set(quickAction);
        // Keeps the two paths mutually exclusive: a workflow radio left armed from an earlier visit
        // must not decide what Execute fires now.
        this.$selectedActionId.set(null);

        const previewItems = this.$previewItems();

        this.$includedItems.set(previewItems);
        // Add to Bundle needs a target first; every other quick action fires from the selection alone
        // and goes straight to its preview.
        this.$view.set(this.$configureKinds().length ? 'configure' : 'preview');
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
     * Fires a quick action over the rows left checked.
     *
     * No confirmation prompt: none of the remaining quick actions is destructive. Lock, Unlock and
     * Add to Bundle are all reversible, and the preview is already a commit point the user passes
     * through. Publish, Archive and Delete — the rows that warranted one — now live in the Workflow
     * Actions section.
     */
    private executeQuickAction(quickAction: DotActionCenterQuickAction): void {
        const inodes = this.$includedItems().map((item) => item.inode);

        if (!inodes.length) {
            return;
        }

        // Add to Bundle and Push Publish leave the workflow path entirely — different endpoints,
        // different id kind, and settings that have to be present.
        if (quickAction.id === ADD_TO_BUNDLE_ACTION_ID) {
            this.fireAddToBundle(quickAction);

            return;
        }

        if (quickAction.id === PUSH_PUBLISH_ACTION_ID) {
            this.firePushPublish(quickAction);

            return;
        }

        // Refresh speaks inodes like the workflow quick actions, but goes to its own job-backed
        // endpoint rather than the system-action fire, so it branches here rather than falling through.
        if (quickAction.id === REFRESH_ACTION_ID) {
            const actionName = this.#dotMessageService.get(quickAction.name);
            this.#store.executeRefresh(actionName, inodes);

            // The only feedback for a reindex until it finishes. It gets no "Applying ..." indicator,
            // because it runs for minutes and the endpoint reports no progress — so saying up front
            // that it is backgrounded is the honest substitute, and it is why the Action Center is left
            // usable rather than locked.
            this.#messageService.add({
                severity: 'info',
                summary: this.#dotMessageService.get(
                    'content-drive.action-center.toast.reindex-started'
                ),
                detail: this.#dotMessageService.get(
                    'content-drive.action-center.toast.reindex-started-detail',
                    actionName,
                    String(inodes.length)
                )
            });
            this.handOffToToolbar();

            return;
        }

        this.#store.executeQuickAction(
            quickAction.id,
            this.#dotMessageService.get(quickAction.name),
            inodes
        );
        this.handOffToToolbar();
    }

    /**
     * Queues the checked contentlets into the chosen bundle.
     *
     * Sends **identifiers**, deduped: the only action here that does not speak inodes. Refuses without
     * a bundle rather than posting — the servlet would create one named `""` or fail opaquely.
     *
     * The choice is remembered so the next visit — and the single-item dialog, which shares the key —
     * opens on the same bundle.
     */
    private fireAddToBundle(quickAction: DotActionCenterQuickAction): void {
        const bundle = this.$selectedBundle();
        const identifiers = toDistinctIdentifiers(this.$includedItems());

        if (!bundle || !identifiers.length) {
            return;
        }

        rememberLastBundleUsed(bundle);
        this.#store.executeAddToBundle(
            this.#dotMessageService.get(quickAction.name),
            bundle,
            identifiers
        );
        this.handOffToToolbar();
    }

    /**
     * Pushes the checked contentlets to the chosen environments.
     *
     * Sends **identifiers**, deduped, for the same reason Add to Bundle does: push publish sends the
     * asset, so every language version of a contentlet is one entry. Refuses without settings rather
     * than posting — the servlet would answer 200 having sent nothing anywhere.
     */
    private firePushPublish(quickAction: DotActionCenterQuickAction): void {
        const settings = this.$pushPublish();
        const identifiers = toDistinctIdentifiers(this.$includedItems());

        if (!settings || !identifiers.length) {
            return;
        }

        this.#store.executePushPublish(
            this.#dotMessageService.get(quickAction.name),
            identifiers,
            settings
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
     * the user manually closing the dialog and the request settling. Refresh is the exception: it shows
     * no indicator at all, having already said by toast that it runs in the background.
     *
     * Counts are also stale from this point on: the contentlets are moving to a new step, so the
     * numbers this dialog is showing no longer hold.
     */
    private handOffToToolbar(): void {
        // The selection has served its purpose the moment an action is fired, and leaving the rows
        // ticked invited firing a second action over content already being changed. Cleared here rather
        // than after the run settles, because the settle path never runs on an error or a timeout — and
        // for a reindex it is minutes away, so the boxes would sit checked for the whole job.
        //
        // Deliberately only on hand-off: dismissing the dialog with X, ESC or the mask keeps the
        // selection, because the user may still be building it.
        this.#store.setSelectedItems([]);
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

        // An action needing a destination stops here for it; every other action goes straight to the
        // preview. The header carries the item count either way, so the configuration step keeps the
        // "N items" context without repeating the row list.
        const configureKinds = this.$configureKinds();

        if (configureKinds.includes('move')) {
            // Mirrors what the seeded picker is showing, so the two agree from the first render.
            // `$destinationUnchanged` warns if the user leaves it as-is; it no longer blocks.
            this.$pathToMove.set(this.$currentPath());
        }

        this.$view.set(configureKinds.length ? 'configure' : 'preview');

        this.publishDrillDownHeader(action.name, previewItems.length);
    }

    /** Records the destination chosen in the configuration step. */
    protected onPathToMoveChange(pathToMove: string): void {
        this.$pathToMove.set(pathToMove);
    }

    /** Records the bundle chosen in the configuration step. */
    protected onBundleChange(bundle: DotBundle | null): void {
        this.$selectedBundle.set(bundle);
    }

    protected onAssignCommentChange(value: DotWorkflowAssignCommentValue): void {
        this.$assignComment.set(value);
    }

    protected onPushPublishChange(value: DotWorkflowPushPublishValue): void {
        this.$pushPublish.set(value);
    }

    /**
     * Leaves the configuration step for the preview, once a destination is chosen.
     *
     * Guarded rather than relying on the disabled button alone, so the step cannot be skipped past by
     * a stray call and reach Execute with an empty path — which the server would reject with an
     * opaque "The host path is not valid".
     */
    protected onContinueFromConfigure(): void {
        if (!this.$canLeaveConfigure() || this.$executing()) {
            return;
        }

        this.$view.set('preview');
    }

    /**
     * Steps back one screen: the preview returns to the configuration step when the action has one,
     * otherwise straight to the action list.
     *
     * A single back control that always returned to the list would throw away a chosen destination on
     * the way past it.
     */
    protected onBack(): void {
        if (this.$executing()) {
            return;
        }

        if (this.$view() === 'preview' && this.$configureKinds().length) {
            this.$view.set('configure');

            return;
        }

        this.onBackToActions();
    }

    /**
     * Returns to the action list.
     *
     * `$selectedActionId` is deliberately kept, so the radio is still armed on return and re-opening
     * the preview does not mean re-picking the action. The chosen destination is *not* kept: the radio
     * survives so the action does not need re-picking, but a path belongs to the run being set up, and
     * carrying it into a different action's configuration step would pre-fill a decision never made
     * for it.
     */
    protected onBackToActions(): void {
        if (this.$executing()) {
            return;
        }

        this.$view.set('actions');
        this.$includedItems.set([]);
        this.$pendingQuickAction.set(null);
        this.$pathToMove.set('');
        this.$selectedBundle.set(null);
        this.$assignComment.set({ assign: '', comment: '' });
        this.$assignCommentValid.set(false);
        this.$pushPublish.set(null);
        this.$pushPublishValid.set(false);
        this.#store.clearDialogDrillDown();
    }

    /** Tracks the preview's checked rows, keeping the dialog header's count in step. */
    protected onIncludedItemsChange(items: DotContentDriveItem[]): void {
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
        const action = this.$selectedAction();
        const contentletIds = this.$includedItems().map((item) => item.inode);

        if (!workflowActionId || !contentletIds.length) {
            return;
        }

        // Anything the action declared an input for must be complete before firing. Refused here as
        // well as by the disabled Continue: an empty move path answers 200 with every item failed, a
        // same-folder move burns a version and a reindex per item to change nothing, and a push
        // publish with no environment has nowhere to go.
        if (this.$configureKinds().length && !this.$canLeaveConfigure()) {
            return;
        }

        const actionName = action?.name ?? workflowActionId;

        this.#store.executeWorkflowAction(workflowActionId, actionName, contentletIds, {
            pathToMove: this.$pathToMove(),
            assignComment: this.$assignComment(),
            pushPublish: this.$pushPublish() ?? undefined
        });
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
