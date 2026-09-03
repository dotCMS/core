import { signalMethod } from '@ngrx/signals';
import { of } from 'rxjs';

import { Location, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService, SortEvent } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { Popover, PopoverModule } from 'primeng/popover';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { catchError } from 'rxjs/operators';

import {
    AddToBundleService,
    DotCurrentUserService,
    DotFolderService,
    DotUploadFileService,
    DotWorkflowsActionsService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    ContextMenuData,
    DotCMSBaseTypesContentTypes,
    DotCMSContentTypeField,
    DotCMSDataTypes,
    DotCMSFieldTypes,
    DotContentDriveActionableFolder,
    DotContentDriveItem,
    DotContentDrivePaginateEvent
} from '@dotcms/dotcms-models';
import { DotEditContentSidePanelComponent, DotSidePanelNavController } from '@dotcms/edit-content';
import {
    DotContentDriveUploadFiles,
    DotFolderTreeNodeData,
    DotFolderTreeNodeContentData,
    DotContentDriveMoveItems,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import {
    DotAddToBundleComponent,
    DotFolderListViewComponent,
    DOT_FOLDER_LIST_VIEW_COLUMN_TYPE,
    DotFolderListViewColumn,
    DotMessagePipe,
    DotToastComponent,
    DotUploadDropzoneComponent,
    DotUploadTypeSelectorComponent
} from '@dotcms/ui';

import { DotContentDriveActionCenterComponent } from '../components/dialogs/dot-content-drive-action-center/dot-content-drive-action-center.component';
import { DotContentDriveDialogContentTypeSelectorComponent } from '../components/dialogs/dot-content-drive-dialog-content-type-selector/dot-content-drive-dialog-content-type-selector.component';
import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
import { DotContentDriveSidebarComponent } from '../components/dot-content-drive-sidebar/dot-content-drive-sidebar.component';
import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import {
    ACTION_CENTER_DIALOG_CONTENT_STYLE,
    ACTION_CENTER_DIALOG_STYLE,
    DIALOG_TYPE,
    SORT_ORDER,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE,
    ERROR_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID,
    NEW_CONTENT_MARKER
} from '../shared/constants';
import {
    DotContentDriveContentTypeSelectorPayload,
    DotContentDriveDialog,
    DotContentDriveSortOrder,
    DotContentDriveStatus,
    DotContentDriveUploadBaseType,
    DotContentDriveUploadSelection,
    DotContentDriveUploadSelectorPayload
} from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { canAddChildrenTo, encodeFilters, isFolder } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [
        DotFolderListViewComponent,
        DotContentDriveToolbarComponent,
        DotFolderListViewContextMenuComponent,
        DotAddToBundleComponent,
        DotContentDriveSidebarComponent,
        DialogModule,
        PopoverModule,
        NgTemplateOutlet,
        DotContentDriveDialogFolderComponent,
        DotContentDriveDialogContentTypeSelectorComponent,
        DotUploadTypeSelectorComponent,
        MessageModule,
        DotMessagePipe,
        DotUploadDropzoneComponent,
        DotToastComponent,
        DotEditContentSidePanelComponent,
        ProgressSpinnerModule,
        DotContentDriveActionCenterComponent
    ],
    providers: [
        DotContentDriveStore,
        // Component-scoped (not `root`) so it can inject the shell's DotContentDriveStore to read
        // the side-panel feature flag; shared with the child components in this shell's subtree.
        DotContentDriveNavigationService,
        DotWorkflowsActionsService,
        MessageService,
        DotFolderService,
        // Injected by the store's `withActionExecution` to fire Add to Bundle. Neither is
        // `providedIn: 'root'`, and the bundle service resolves the current user to reach their
        // bundles. `DotAddToBundleComponent` (single item, from the context menu) provides its own pair.
        AddToBundleService,
        DotCurrentUserService
    ],
    templateUrl: './dot-content-drive-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'grid relative h-full grid-cols-[min-content_1fr_min-content] grid-rows-[min-content_min-content_1fr]'
    }
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);

    readonly #location = inject(Location);
    readonly #navigationService = inject(DotContentDriveNavigationService);
    readonly #destroyRef = inject(DestroyRef);

    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #fileService = inject(DotUploadFileService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #sidePanelNav = inject(DotSidePanelNavController);

    /** Edit Content side panel request, driven by the navigation service; read by the template. */
    protected readonly $editPanelRequest = this.#navigationService.$editPanelRequest;

    /**
     * Whether the last `editContent` URL write reflected an open panel. Lets the effect push when
     * opening (so Back can pop the panel) but replace when closing — a push on close would leave a
     * phantom entry whose Back puts the just-removed param back with no panel rendered.
     */
    #editPanelUrlWasSet = false;
    /**
     * The folder path the URL was last written with, so a genuine folder navigation can be told apart
     * from a filter-only write. `undefined` until the first write, which is what keeps the very first
     * URL from pushing an entry on top of the one the user arrived on.
     */
    #lastWrittenPath: string | undefined = undefined;

    /**
     * The rendered side panel, so browser Back can route its close through the panel's guard.
     * Queried by template ref var (`#sidePanelRef`), not the class token: passing the class itself
     * would be a runtime reference to it outside the `@defer` block below, which disqualifies it
     * from Angular's automatic deferred-import bundling (the `<T>` here is a type-only generic,
     * erased at compile time — it leaves no runtime reference).
     */
    protected readonly $sidePanel = viewChild<DotEditContentSidePanelComponent>('sidePanelRef');

    readonly $items = this.#store.items;
    readonly $status = this.#store.status;

    /**
     * The tree's VISUAL expanded state (drives width/animation). Combines the user's real
     * preference with any transient collapse the side panel is forcing — see
     * `isTreeVisuallyExpanded` on the store for why these are kept separate.
     */
    readonly $treeExpanded = this.#store.isTreeVisuallyExpanded;

    /**
     * Folder a dropped file lands in. The shared dropzone is presentational, so the target comes
     * from here rather than the dropzone reaching into the store itself.
     */
    readonly $selectedFolder = computed(() => this.#store.selectedNode()?.data);

    /**
     * Whether the browsed folder accepts new children. Disables the drop zone where it does not:
     * an upload creates a contentlet in the target folder, so the drop would be refused server-side
     * only after the user had already committed the gesture. Shared with the toolbar's New and
     * Upload buttons via the store, so the three cannot disagree.
     */
    readonly $canAddChildren = this.#store.$canAddChildren;

    /** Inodes any in-flight run is acting on, so the grid can mark those rows. */
    readonly $busyRows = this.#store.busyRows;

    /**
     * Forces the folder tree visually collapsed while the Edit Content side panel is open on a
     * narrow viewport, and clears the override on close. Purely derived from the panel's open
     * state each time it runs — no bookkeeping needed (unlike a real preference, "should the panel
     * currently be forcing a collapse" has no history to restore: it is always correctly
     * recomputed from the CURRENT panel/viewport state, including right after a refresh with the
     * panel already open from a deep link). `untracked` guards the store read/write so the effect
     * only re-runs when the panel open/close state changes.
     */
    // eslint-disable-next-line no-unused-private-class-members -- effect() runs for its side effects; the field only holds the EffectRef
    #forceCollapseTreeWithPanelEffect = effect(() => {
        const panelOpen = !!this.$editPanelRequest();

        untracked(() => {
            this.#store.setTreeForceCollapsed(panelOpen && this.#sidePanelNav.shouldCollapse());
        });
    });

    readonly $contextMenuData = this.#store.contextMenu;

    readonly DIALOG_TYPE = DIALOG_TYPE;

    /** Drives `[visible]`: open/close state of the dialog. */
    protected readonly $dialogVisible = signal(false);

    /**
     * The dialog currently rendered in the body. Held through PrimeNG's close animation
     * (only cleared on `(onHide)`) so the body doesn't blank out before the dialog finishes
     * animating away. Synced from the store by {@link #syncDialog}.
     */
    protected readonly $activeDialog = signal<DotContentDriveDialog | undefined>(undefined);

    /**
     * The grid's checked rows, driven from the store.
     *
     * Passing this puts `dot-folder-list-view` in its controlled mode, which is what makes clearing the
     * store actually uncheck the boxes. Left uncontrolled, the grid keeps its own selection and only
     * drops it when the `items` reference changes — so a selection cleared on action hand-off stayed
     * visibly ticked until the next search returned.
     */
    protected readonly $selectedItems = this.#store.selectedItems;

    /** Folder payload for the folder dialog (narrowed from the dialog payload union by type). */
    readonly $folderPayload = computed(() => {
        const dialog = this.$activeDialog();

        return dialog?.type === DIALOG_TYPE.FOLDER
            ? (dialog.payload as DotContentDriveActionableFolder)
            : undefined;
    });

    /** List type for the content-type selector dialog (encodes which base types to show). */
    readonly $contentTypeSelectorListType = computed<DotUVEPaletteListTypes | undefined>(() => {
        const dialog = this.$activeDialog();

        return dialog?.type === DIALOG_TYPE.CONTENT_TYPE_SELECTOR
            ? (dialog.payload as DotContentDriveContentTypeSelectorPayload).listType
            : undefined;
    });

    /** Upload-type selector popover, anchored imperatively to the Upload button on click. */
    readonly $uploadSelectorPopover = viewChild<Popover>('uploadSelectorPopover');

    /** Payload (target folder + optional dropped files) driving the upload-selector body. */
    readonly $uploadSelectorPayload = signal<DotContentDriveUploadSelectorPayload | undefined>(
        undefined
    );

    /**
     * Drives the drag-and-drop upload modal. The Upload-button flow uses the popover (anchored to
     * the button); drag-and-drop has no trigger element, so it prompts with a centered modal.
     */
    readonly $uploadModalVisible = signal(false);

    /**
     * Holds the selection emitted by the upload dialog while the OS file picker is open (Upload-button
     * flow only). The dropped-files flow uploads immediately and never sets this.
     */
    readonly $activeSelection = signal<DotContentDriveUploadSelection | undefined>(undefined);

    /**
     * Content-type selector: sized to fit ~4 UVE-width cards per row. No horizontal padding so
     * the paginator/footer separators span edge-to-edge; the list and footer add their own inset.
     */
    readonly $dialogContentClass = computed(() => {
        switch (this.$activeDialog()?.type) {
            case DIALOG_TYPE.CONTENT_TYPE_SELECTOR:
                return 'w-152 max-w-[92vw] px-0! pt-0 pb-4';
            // Action Center sizes itself through `$dialogStyle` / `$dialogContentStyle` instead.
            case DIALOG_TYPE.ACTION_CENTER:
                return '';
            default:
                return 'w-175 pt-0 p-4';
        }
    });

    /**
     * @see ACTION_CENTER_DIALOG_STYLE
     */
    readonly $dialogStyle = computed(() =>
        this.$activeDialog()?.type === DIALOG_TYPE.ACTION_CENTER
            ? ACTION_CENTER_DIALOG_STYLE
            : undefined
    );

    /**
     * @see ACTION_CENTER_DIALOG_CONTENT_STYLE
     */
    readonly $dialogContentStyle = computed(() =>
        this.$activeDialog()?.type === DIALOG_TYPE.ACTION_CENTER
            ? ACTION_CENTER_DIALOG_CONTENT_STYLE
            : undefined
    );

    /**
     * Drops the header's bottom rule and locks horizontal padding to `px-6` so the title lines up
     * with the Action Center body/footer (PrimeNG's dialog header padding token may not match).
     */
    readonly $dialogHeaderClass = computed(() =>
        this.$activeDialog()?.type === DIALOG_TYPE.ACTION_CENTER ? 'border-b-0 px-6! pb-2' : ''
    );

    /**
     * Items in the current selection, for the Action Center's header sub-line.
     *
     * Counts folders as well as contentlets: Add to Bundle and Push Publish both act on a folder, so
     * excluding them would under-report what the dialog is about to operate on. Which individual
     * actions apply to which rows is the action list's job to say, not the header's.
     */
    readonly $actionCenterSelectionCount = computed(() => this.#store.selectedItems().length);

    /**
     * Action Center title. Swaps to the drilled-into screen's title (the selected workflow action)
     * when its body publishes one, so there is one header rather than the dialog's and the body's.
     */
    readonly $actionCenterHeader = computed(
        () => this.#store.dialogDrillDown()?.header ?? this.$activeDialog()?.header
    );

    /**
     * Item count for the Action Center's header sub-line: the items the drilled-into action will run
     * on, falling back to the whole contentlet selection at the top level.
     */
    readonly $actionCenterCount = computed(
        () => this.#store.dialogDrillDown()?.itemCount ?? this.$actionCenterSelectionCount()
    );

    /**
     * Syncs the dialog open/close state from the store. Opening sets the body and visibility
     * together (no blank-frame flash); closing flips visibility off but leaves the body mounted
     * so PrimeNG can animate it out — the body is cleared later in {@link onDialogHidden}.
     * `signalMethod` only tracks its input, so the writes here need no manual `untracked`.
     */
    readonly #syncDialog = signalMethod<DotContentDriveDialog | undefined>((dialog) => {
        if (dialog) {
            this.$activeDialog.set(dialog);
            this.$dialogVisible.set(true);
        } else {
            this.$dialogVisible.set(false);
        }
    });

    constructor() {
        this.#syncDialog(this.#store.dialog);

        // Shareable deep-link: `?editContent=<identifier>` reopens the edit panel on load. Read
        // once from the snapshot (the portlet is not re-created on in-session query-param changes).
        // The `new`-mode marker is ignored — creating is not shareable, so only real identifiers
        // are resolved.
        const editContent = this.#route.snapshot.queryParams['editContent'];
        if (editContent && editContent !== NEW_CONTENT_MARKER) {
            // `editContentLang` names the exact version to reopen: an identifier has one version per
            // language, so without it the resolver can only guess. Absent on a link written before it
            // was recorded, which the resolver still handles.
            const languageId = Number(this.#route.snapshot.queryParams['editContentLang']);
            this.#navigationService.openEditByIdentifier(
                editContent,
                Number.isFinite(languageId) && languageId > 0 ? languageId : undefined
            );
        }

        // Browser Back/Forward: the open panel's `editContent` param is written via `Location.go`
        // (no router navigation), so nothing else reacts to popstate. When Back removes or changes
        // that param while a panel is open (edit OR new), route the close through the panel's
        // unsaved-changes guard — a direct `closeEditPanel()` would tear the editor down and discard
        // unsaved edits silently.
        const locationSubscription = this.#location.subscribe((event) => {
            const params = new URLSearchParams(event.url?.split('?')[1] ?? '');
            const editContentParam = params.get('editContent');
            const request = this.#navigationService.$editPanelRequest();
            if (!request) {
                return;
            }

            // The param the URL should carry for the currently-open panel: the identifier for edit,
            // the marker for new. If Back changed it away from that, the panel should close.
            const expected =
                request.mode === 'edit' ? (request.identifier ?? null) : NEW_CONTENT_MARKER;

            if (expected !== editContentParam) {
                // Restore the param so the URL matches the still-open panel while the guard decides.
                // `replaceState` (not `go`) avoids piling up history entries. Discard → the panel
                // emits `closed` → onEditPanelClosed → closeEditPanel clears the param; Keep editing
                // → the panel stays open and the URL is already back in sync.
                const restoredUrl = this.#router
                    .createUrlTree([], {
                        queryParams: { editContent: expected },
                        queryParamsHandling: 'merge'
                    })
                    .toString();
                this.#location.replaceState(restoredUrl);
                this.$sidePanel()?.requestClose();
            }
        });
        this.#destroyRef.onDestroy(() => locationSubscription.unsubscribe());
    }

    readonly $offset = computed(() => this.#store.pagination().offset, {
        equal: (a, b) => a === b
    });

    readonly $loading = computed(() => this.#store.status() === DotContentDriveStatus.LOADING);

    /**
     * Extra table columns for the current selection: the selected single content type's "Show In
     * List" fields mapped to the list-view column shape, with a display type and width derived from
     * each field's data type. Empty when 0 or >1 content types are selected; the table appends
     * these after the fixed Type column.
     */
    readonly $extraColumns = computed<DotFolderListViewColumn[]>(() =>
        this.#store.showInListFields().map((field, index) => ({
            field: field.variable,
            header: field.name,
            // Sortable follows the field's `indexed` flag: the backend sorts via `sortBy` on the
            // index, so a non-indexed (but listed) field can't be sorted. The schema has no explicit
            // `sortable`; `indexed` is the determinant.
            sortable: field.indexed,
            order: index,
            type: this.#columnTypeForField(field)
        }))
    );

    /**
     * Maps a content-type field to the table's generic display type. Image/Binary/File fields render
     * as a thumbnail of the field's own asset. Date, Date-and-Time and Time all share `dataType`
     * DATE, so the date sub-type is resolved from `fieldType` first (to keep the time part). The
     * table decides each column's width from this type + its own row values.
     */
    #columnTypeForField(field: DotCMSContentTypeField): DotFolderListViewColumn['type'] {
        if (
            field.fieldType === DotCMSFieldTypes.IMAGE ||
            field.fieldType === DotCMSFieldTypes.BINARY ||
            field.fieldType === DotCMSFieldTypes.FILE
        ) {
            return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.IMAGE;
        }
        if (field.fieldType === DotCMSFieldTypes.DATE_AND_TIME) {
            return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATETIME;
        }
        if (field.fieldType === DotCMSFieldTypes.TIME) {
            return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.TIME;
        }

        switch (field.dataType) {
            case DotCMSDataTypes.DATE:
                return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATE;
            case DotCMSDataTypes.BOOLEAN:
                return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.BOOLEAN;
            case DotCMSDataTypes.INTEGER:
            case DotCMSDataTypes.FLOAT:
                return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.NUMBER;
            default:
                return DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.TEXT;
        }
    }

    readonly $fileInput = viewChild<ElementRef>('fileInput');

    readonly $totalItems = computed(() => {
        const pagination = untracked(() => this.#store.pagination());
        const currentPage = pagination.page; // 1-indexed
        const limit = pagination.limit;
        const page = this.#store.pages().at(-1);

        const items = untracked(() => this.#store.items());

        // The API uses cursor-based pagination and does not return a total count.
        // When there are more folders OR content, we return one page beyond current so PrimeNG
        // enables the next-page button (a folder with only sub-folders has hasMoreContent=false but
        // hasMoreFolders=true). When neither has more, we can calculate the exact total.
        return page?.hasMoreContent || page?.hasMoreFolders
            ? limit * (currentPage + 1)
            : limit * (currentPage - 1) + items.length;
    });

    /**
     * Reports a finished workflow action as a toast, refreshes the grid, and closes the dialog if it
     * is still open.
     *
     * Lives in the shell rather than in the Action Center because the run outlives that dialog: the
     * user may close it mid-flight and the result still has to be reported. The shell owns
     * `<p-toast>` and is never destroyed while the portlet is open, so it is the only place that can
     * present a result whose originating dialog may already be gone. It also keeps the store data-only.
     *
     * The reload lands here for the same reason, plus a mechanical one: `loadItems` belongs to the
     * base store's `withMethods`, which `withActionExecution` cannot reach from inside the
     * composition. `loadItems` clears the selection and sets `LOADING` itself, so this one call is the
     * whole post-run refresh.
     *
     * `failCount` downgrades the toast to a warning. Partial failure is a normal outcome for these
     * endpoints (a lock held by somebody else, a per-contentlet permission), and reporting it as an
     * unqualified success would be the one thing the user cannot recover from — the grid has already
     * reloaded and the selection is gone.
     */
    readonly actionExecutionResultEffect = effect(() => {
        const result = this.#store.actionExecutionResult();

        if (!result) {
            return;
        }

        const {
            actionName,
            successCount,
            skippedCount,
            failCount,
            partialDetailKey,
            backgrounded
        } = result;

        // Skips and failures are not mutually exclusive: one bulk fire over a mixed-type selection
        // can skip items whose scheme does not own the action *and* be refused on items that are
        // locked. The ladder this replaces reported whichever it checked first, so a mixed result
        // showed the failure copy alone and blamed permissions or locks for the entire shortfall —
        // sending the user off to unlock content that was never the problem.
        //
        // So anything short of a clean run reports all three numbers, each next to its own cause.
        // Both counts are always passed, meaning a fails-only run renders "0 skipped"; naming the
        // cause and its number is what keeps the message honest.
        const isPartial = failCount > 0 || skippedCount > 0;

        const detail = isPartial
            ? this.#dotMessageService.get(
                  // Actions whose failures and skips mean something other than permissions, locks and
                  // workflow steps say so themselves — see `partialDetailKey`.
                  partialDetailKey ?? 'content-drive.action-center.toast.executed-partial',
                  actionName,
                  String(successCount),
                  String(failCount),
                  String(skippedCount)
              )
            : this.#dotMessageService.get(
                  'content-drive.action-center.toast.executed-detail',
                  actionName,
                  String(successCount)
              );

        this.#messageService.add({
            // A skip is a shortfall too — those items did not get the action — so it warns rather
            // than reporting green, which is what it used to do.
            severity: isPartial ? 'warn' : 'success',
            summary: this.#dotMessageService.get('content-drive.action-center.toast.executed'),
            detail,
            life: isPartial ? WARNING_MESSAGE_LIFE : SUCCESS_MESSAGE_LIFE
        });

        untracked(() => {
            // A backgrounded outcome arrives unprompted, so it must not disturb whatever the user is
            // doing when it lands. Every other result settles a request they are waiting on.
            const dialogIsOpen = !!this.$activeDialog();

            if (!backgrounded || !dialogIsOpen) {
                // Contentlets have moved step, so the grid is stale; `loadItems` also drops the
                // selection the run consumed. Skipped for a backgrounded result while a dialog is
                // open, because reloading pulls the rows out from under the form being filled in.
                this.#store.loadItems();
            }

            if (!backgrounded) {
                // A no-op when the user already closed the dialog, which is the common path now that
                // firing hands off to the toolbar. Never done for a backgrounded result: it can land
                // minutes later, while the user is mid-way through configuring a different action,
                // and closing the dialog throws that input away.
                this.#store.closeDialog();
            }

            this.#store.clearActionExecutionResult();
        });
    });

    readonly updateQueryParamsEffect = effect(() => {
        const isTreeExpanded = this.#store.isTreeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        // `null` removes the param when queryParamsHandling is 'merge'
        const queryParams: Record<string, string | null> = {};

        queryParams['isTreeExpanded'] = isTreeExpanded.toString();

        if (path && path.length) {
            queryParams['path'] = path;
        } else {
            queryParams['path'] = null;
        }

        if (filters && Object.keys(filters).length) {
            queryParams['filters'] = encodeFilters(filters);
        } else {
            queryParams['filters'] = null;
        }

        // Reflect the open panel in the `editContent` param: the shareable identifier for edit, or
        // a non-shareable marker for new (so browser Back has an entry to pop). Cleared when the
        // panel is closed. Written via Location.go/replaceState so it triggers no navigation/reload.
        const editRequest = this.$editPanelRequest();
        const editContent = editRequest
            ? editRequest.mode === 'edit'
                ? (editRequest.identifier ?? null)
                : NEW_CONTENT_MARKER
            : null;
        queryParams['editContent'] = editContent;
        // Written alongside so the link reopens the very version that is open, not just the content.
        // `null` removes it, so it never lingers once the panel is closed or a `new` panel is open.
        queryParams['editContentLang'] =
            editRequest?.mode === 'edit' && editRequest.languageId
                ? String(editRequest.languageId)
                : null;

        const urlTree = this.#router.createUrlTree([], {
            queryParams,
            queryParamsHandling: 'merge'
        });

        // Only write when the URL actually changes (keeps it idempotent — e.g. after Back already
        // moved the URL).
        const newUrl = urlTree.toString();
        if (newUrl !== this.#location.path(true)) {
            // Push only for the two transitions a user would expect Back to undo: opening the panel
            // (AC8) and navigating to a different folder. Everything else replaces.
            //
            // Filter writes must never push, because the default seed is a filter write and is not a
            // user action at all: it lands on a cold load and again when the default language
            // resolves. Pushing those buried the entry the user arrived on, so Back took two or three
            // presses to leave the portlet. Folder navigation still pushes, so Back walks back up the
            // tree as it did before.
            //
            // The first write never pushes: `#lastWrittenPath` is undefined until then, so the URL the
            // portlet opens with replaces rather than stacking on top of the referring page.
            const isOpeningPanel = editContent !== null && !this.#editPanelUrlWasSet;
            const isFolderNavigation =
                this.#lastWrittenPath !== undefined && path !== this.#lastWrittenPath;

            if (isOpeningPanel || isFolderNavigation) {
                this.#location.go(newUrl);
            } else {
                this.#location.replaceState(newUrl);
            }
            this.#lastWrittenPath = path;
        }
        this.#editPanelUrlWasSet = editContent !== null;
    });

    /**
     * Effect that sets the path when a node is selected
     * Uses untracked to avoid creating a dependency on path signal
     */
    readonly setPathEffect = effect(() => {
        // Read both dependencies up front so the guard below doesn't drop `sidebarLoading` as a
        // dependency (the effect must re-run once the sidebar finishes resolving).
        const selectedNode = this.#store.selectedNode();
        const sidebarLoading = this.#store.sidebarLoading();

        // Don't sync the path while the sidebar is still resolving its folders. On a cold reload
        // with a `path` in the URL, `selectedNode` is still the default root node at this point;
        // syncing from it would clear the restored path back to root and the deep-linked folder
        // would never open. Once `loadFolders` resolves, it sets `selectedNode` to the matching
        // node (and flips `sidebarLoading` off), so this effect re-runs and stays in sync.
        // TreeNode.data is optional in PrimeNG's type, so guard it before reading path.
        if (sidebarLoading || !selectedNode?.data) {
            return;
        }

        // Read current path without tracking it to avoid circular dependencies
        const currentPath = untracked(() => this.#store.path()) ?? '';
        const data = selectedNode.data;

        if (!data || data.type === 'load-more') {
            return;
        }

        if (data.path != currentPath) {
            this.#store.setPath(data.path);
        }
    });

    protected onPaginate(event: DotContentDrivePaginateEvent) {
        // Explicit check because it can potentially be 0
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.#store.setPagination({
            limit: event.rows,
            page: event.page ?? 1,
            offset: event.first ?? 0
        });
    }

    protected onSort(event: SortEvent) {
        // Explicit check because it can potentially be 0
        if (event.order === undefined || !event.field) {
            return;
        }

        this.#store.setSort({
            field: event.field,
            order: SORT_ORDER[event.order] ?? DotContentDriveSortOrder.ASC
        });
    }

    /**
     * Handles right-click context menu event on a content item
     * @param event The mouse event that triggered the context menu
     * @param contentlet The content item that was right-clicked
     */
    protected onContextMenu({ event, contentlet }: ContextMenuData) {
        event.preventDefault();
        this.#store.patchContextMenu({ triggeredEvent: event, contentlet });
    }

    /**
     * Handles double click event on a content item
     * @param contentlet The content item that was double clicked
     */
    protected onDoubleClick(contentlet: DotContentDriveItem) {
        if (isFolder(contentlet)) {
            this.#store.setSelectedNode({
                data: {
                    type: 'folder',
                    path: contentlet.path,
                    hostname: this.#store.currentSite()?.hostname,
                    id: contentlet.identifier,
                    inode: contentlet.inode,
                    // Carry the folder's upload preference so the Upload button reflects it right
                    // away when navigating via the table (not only via the sidebar tree).
                    defaultBaseType: contentlet.defaultBaseType,
                    fromTable: true
                },
                key: contentlet.identifier,
                label: contentlet.path,
                leaf: false
            });
            return;
        }

        this.#navigationService.editContent(contentlet);
    }

    /**
     * Cancels the "Add to Bundle" dialog by setting its visibility to false
     */
    protected cancelAddToBundle() {
        this.#store.setShowAddToBundle(false);
    }

    /**
     * Fired by PrimeNG when the dialog visibility changes. A user-driven close (X / ESC /
     * mask) emits `false`; propagate it to the store so the dialog state stays consistent.
     */
    protected onVisibleChange(visible: boolean) {
        if (!visible) {
            this.#store.closeDialog();
        }
    }

    /**
     * Fired after the close animation completes — now safe to drop the rendered body.
     */
    protected onDialogHidden() {
        this.$activeDialog.set(undefined);
    }

    /** Closes the Edit Content side panel. */
    protected onEditPanelClosed() {
        this.#navigationService.closeEditPanel();
    }

    /** A save in the side panel can create or change an item, so refresh the list. */
    protected onEditPanelSaved() {
        this.#store.reloadContentDrive();
    }

    /**
     * Upload-button flow. When the current folder pins a base type (`defaultBaseType`), skip the
     * menu and open the OS file picker straight away; otherwise open the type menu anchored to the
     * button and defer the picker until the user picks a type in {@link onUploadTypeSelected}.
     */
    protected onUpload(event: MouseEvent) {
        const targetFolder = this.#store.selectedNode()?.data;
        const contentData =
            targetFolder && targetFolder.type !== LOAD_MORE_NODE_TYPE
                ? (targetFolder as DotFolderTreeNodeContentData)
                : undefined;
        const baseType = this.#resolvePreferredBaseType(contentData?.defaultBaseType);

        if (baseType) {
            this.$activeSelection.set({ targetFolder, baseType });
            this.$fileInput()?.nativeElement.click();

            return;
        }

        this.openUploadSelector({ targetFolder }, event);
    }

    /**
     * Drag-and-drop / sidebar flow: the files are already known. When the target folder pins a base
     * type, upload the files directly; otherwise open the type menu (anchored to the content area)
     * and carry the files into the payload to upload right after the user picks.
     */
    /**
     * Refuses a drop onto a folder the user cannot add content to, and says so.
     *
     * A drag onto a tree folder is a third route into that folder, alongside the New menu and the
     * grid drop zone, and it is the one that bypassed the gate. Creating a folder and moving a
     * contentlet are both refused server-side without this permission (`FolderAPIImpl:673`,
     * `ESContentletAPIImpl:607`), so an ungated drop hands the user a failure they could not have
     * predicted from the UI. An upload is *not* refused server-side — the contentlet checkin path
     * does not check it — which is the stronger reason to gate it here rather than the weaker one:
     * otherwise one route into a folder quietly allows what the other two forbid.
     *
     * A toast rather than a refused drop target, because the drag is over a tree node with no room
     * to explain itself, and a gesture that simply does nothing reads as a broken UI.
     *
     * @param {DotFolderTreeNodeData} [targetFolder] - The folder dropped on
     * @returns {boolean} Whether the drop may proceed
     */
    #canDropInto(targetFolder?: DotFolderTreeNodeData): boolean {
        if (canAddChildrenTo(targetFolder, this.#store.siteCanAddChildren())) {
            return true;
        }

        const isSiteRoot = !(targetFolder as { permissions?: string[] })?.permissions?.length;

        this.#messageService.add({
            severity: 'error',
            summary: this.#dotMessageService.get('content-drive.no-permission.title'),
            detail: this.#dotMessageService.get(
                isSiteRoot
                    ? 'content-drive.no-permission.add-to-site'
                    : 'content-drive.no-permission.add-to-folder'
            ),
            life: ERROR_MESSAGE_LIFE
        });

        return false;
    }

    protected onRequestUpload({ files, targetFolder }: DotContentDriveUploadFiles) {
        if (!this.#canDropInto(targetFolder)) {
            return;
        }

        const contentData =
            targetFolder && targetFolder.type !== LOAD_MORE_NODE_TYPE
                ? (targetFolder as DotFolderTreeNodeContentData)
                : undefined;
        const baseType = this.#resolvePreferredBaseType(contentData?.defaultBaseType);

        if (baseType) {
            this.resolveFilesUpload({ files, targetFolder, baseType });

            return;
        }

        // No trigger element: the prompt falls back to a modal (see openUploadSelector).
        this.openUploadSelector({ targetFolder, files });
    }

    /**
     * Resolves a folder's stored `defaultBaseType` to the upload base type, or `undefined` when the
     * folder has no preference ("ask each time"). Normalizes case and ignores unknown values.
     */
    #resolvePreferredBaseType(
        defaultBaseType?: string | null
    ): DotContentDriveUploadBaseType | undefined {
        switch (defaultBaseType?.toUpperCase()) {
            case DotCMSBaseTypesContentTypes.DOTASSET:
                return DotCMSBaseTypesContentTypes.DOTASSET;
            case DotCMSBaseTypesContentTypes.FILEASSET:
                return DotCMSBaseTypesContentTypes.FILEASSET;
            default:
                return undefined;
        }
    }

    /**
     * Single entry point for the Asset/File prompt. With a trigger event (Upload button) it shows a
     * popover anchored to the button; without one (drag-and-drop) it falls back to a centered modal.
     * Both share the same payload and resolve through {@link onUploadTypeSelected}.
     */
    protected openUploadSelector(
        payload: DotContentDriveUploadSelectorPayload,
        event?: MouseEvent
    ) {
        this.$uploadSelectorPayload.set(payload);

        // The popover and the modal are mutually exclusive: opening one dismisses the other so a
        // lingering button-popover can't sit behind the drag-and-drop modal (and vice versa).
        // The modal's visibility is set BEFORE hiding the popover so the popover's `onHide`
        // handoff guard sees the modal is taking over and keeps the shared payload.
        if (event) {
            this.$uploadModalVisible.set(false);
            this.$uploadSelectorPopover()?.show(event, event.currentTarget as HTMLElement);
        } else {
            this.$uploadModalVisible.set(true);
            this.$uploadSelectorPopover()?.hide();
        }
    }

    /**
     * Clears the drag-and-drop upload modal when it is dismissed (X / ESC / mask click).
     */
    protected onUploadModalVisibleChange(visible: boolean) {
        this.$uploadModalVisible.set(visible);

        if (!visible) {
            this.$uploadSelectorPayload.set(undefined);
        }
    }

    /**
     * Clears the shared selector payload when the Upload-button popover is dismissed without a
     * selection (click outside), keeping it symmetric with {@link onUploadModalVisibleChange}.
     * Skips clearing when the popover is only being hidden to hand off to the modal (they share the
     * payload) — otherwise the modal would render empty right as it opens.
     */
    protected onUploadSelectorPopoverHide() {
        if (this.$uploadModalVisible()) {
            return;
        }

        this.$uploadSelectorPayload.set(undefined);
    }

    /**
     * Handles the asset-type choice emitted by the upload selector (popover or modal).
     * - Drag-and-drop: the files are already in the selection, so upload immediately.
     * - Upload button: stash the selection and open the OS file picker; {@link onFileChange}
     *   completes the upload once files are chosen.
     */
    protected onUploadTypeSelected(selection: DotContentDriveUploadSelection) {
        this.$uploadSelectorPopover()?.hide();
        this.$uploadModalVisible.set(false);
        this.$uploadSelectorPayload.set(undefined);

        if (selection.files?.length) {
            this.resolveFilesUpload(selection);

            return;
        }

        this.$activeSelection.set(selection);
        this.$fileInput()?.nativeElement.click();
    }

    /**
     * Handles file change event (Upload-button flow): merges the chosen files into the pending
     * selection and triggers the upload with the previously chosen content type.
     * @param event The event that triggered the file change
     */
    protected onFileChange(event: Event) {
        const input = event.target as HTMLInputElement;

        const files = input.files;
        const selection = this.$activeSelection();

        // Consume the files BEFORE resetting the input: `input.files` is a live FileList, so
        // `input.value = ''` empties it. Resetting first would drop the selection and the upload
        // would never fire (the file is captured synchronously into FormData by resolveFilesUpload).
        if (files && files.length > 0 && selection) {
            this.resolveFilesUpload({ ...selection, files });
        }

        // Reset so a cancelled/re-opened picker can't reuse a stale selection.
        this.$activeSelection.set(undefined);
        input.value = '';
    }

    /**
     * Handles drag start event on a content item
     */
    protected onDragStart(event: DotContentDriveItem[]) {
        this.#store.patchContextMenu({ triggeredEvent: null, contentlet: null });
        this.#store.setDragItems(event);
    }

    /**
     * Handles drag end event on a content item
     */
    protected onDragEnd() {
        this.#store.cleanDragItems();
    }

    /**
     * Resolves the upload of multiple files or a single file
     * @param selection The chosen content type, target folder and files to upload
     */
    protected resolveFilesUpload({
        files,
        targetFolder,
        baseType
    }: DotContentDriveUploadSelection) {
        if (!files?.length) {
            return;
        }

        if (files.length > 1) {
            this.uploadFiles({ files, targetFolder, baseType });

            return;
        }

        this.uploadFile({ files, targetFolder, baseType });
    }

    /**
     * Shows a warning message when multiple files are uploaded
     *
     * @protected
     * @param {DotContentDriveUploadSelection} selection
     * @memberof DotContentDriveShellComponent
     */
    protected uploadFiles({ files, targetFolder, baseType }: DotContentDriveUploadSelection) {
        this.#messageService.add({
            severity: 'warn',
            summary: this.#dotMessageService.get('content-drive.work-in-progress'),
            detail: this.#dotMessageService.get('content-drive.multiple-files-warning'),
            life: WARNING_MESSAGE_LIFE
        });

        this.uploadFile({ files, targetFolder, baseType });
    }

    /**
     * Uploads a file to the content drive
     * @param selection The chosen content type, target folder and files to upload
     */
    protected uploadFile({ files, targetFolder, baseType }: DotContentDriveUploadSelection) {
        if (!files?.length) {
            return;
        }

        this.uploadByBaseType(files[0], baseType, targetFolder);
    }

    /**
     * Uploads a file to the content drive resolving the content type from the given base type
     * (`DOTASSET` for Assets, `FILEASSET` for Files).
     *
     * @protected
     * @param {File} file
     * @param {string} baseType
     * @param {DotFolderTreeNodeData} [hostFolder]
     * @memberof DotContentDriveShellComponent
     */
    protected uploadByBaseType(file: File, baseType: string, hostFolder?: DotFolderTreeNodeData) {
        this.#fileService
            .uploadFileByBaseType(file, baseType, {
                // A folder id carries its site; at the site root (no folder) fall back to the
                // current site identifier so the upload lands on the site being browsed, not the
                // backend default host.
                hostFolder: hostFolder?.id ?? this.#store.currentSite()?.identifier ?? '',
                indexPolicy: 'WAIT_FOR'
            })
            .subscribe({
                next: ({ title }) => {
                    // Tell the user which kind they uploaded (Asset vs File), based on the base
                    // type they chose in the menu — not the raw resolved content-type variable.
                    const typeLabel = this.#dotMessageService.get(
                        baseType === DotCMSBaseTypesContentTypes.FILEASSET
                            ? 'content-drive.dialog.upload-selector.file'
                            : 'content-drive.dialog.upload-selector.asset'
                    );

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-success'),
                        detail: this.#dotMessageService.get(
                            'content-drive.add-dotasset-success-detail',
                            title,
                            typeLabel
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });

                    this.#store.loadItems();
                },
                error: (error) => {
                    console.error('Content drive upload error => ', error);
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-error'),
                        detail:
                            error.error?.errors?.[0]?.message ??
                            this.#dotMessageService.get('content-drive.add-dotasset-error-detail'),
                        life: ERROR_MESSAGE_LIFE
                    });
                }
            });
    }

    /**
     * Handles when items are moved to a folder
     *
     * @param {DotContentDriveMoveItems} event - The move items event
     */
    protected onMoveItems(event: DotContentDriveMoveItems): void {
        if (!this.#canDropInto(event.targetFolder)) {
            return;
        }

        const { folderName, pathToMove, dragItems } = this.getMoveMetadata(event);

        const dragItemsInodes = dragItems.contentlets.map((item) => item.inode);
        const assetContentletsCount = dragItems.contentlets.length;

        // Reports on the toolbar indicator, not as a notification announcing a start (FR-007,
        // FR-008). The two "moving …" toasts this replaces said only that something had begun,
        // which the indicator says better and without stacking up over the outcome that follows.
        const runId = this.#store.startExternalRun({
            operation: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID,
            actionName: this.#dotMessageService.get('content-drive.context-menu.move'),
            total: assetContentletsCount,
            targetLabel: folderName,
            targets: dragItemsInodes
        });

        this.#dotWorkflowActionsFireService
            .bulkFire({
                additionalParams: {
                    assignComment: {
                        assign: '',
                        comment: ''
                    },
                    pushPublish: {},
                    additionalParamsMap: {
                        _path_to_move: pathToMove
                    }
                },
                contentletIds: dragItemsInodes,
                workflowActionId: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
            })
            .pipe(
                catchError(() => {
                    this.#store.endExternalRun(runId);
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.move-to-folder-error'),
                        detail: this.#dotMessageService.get(
                            'content-drive.move-to-folder-error-detail'
                        ),
                        life: ERROR_MESSAGE_LIFE
                    });

                    return of({ successCount: 0, fails: [] });
                })
            )
            .subscribe(({ successCount, fails }) => {
                this.#store.endExternalRun(runId);

                if (successCount > 0) {
                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get(
                            'content-drive.move-to-folder-success'
                        ),
                        detail: this.#dotMessageService.get(
                            'content-drive.move-to-folder-success-detail',
                            successCount.toString(),
                            `${successCount > 1 ? 's ' : ' '}`,
                            folderName
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });
                    this.#store.loadItems();
                }

                fails.forEach(({ errorMessage, inode }) => {
                    const item = dragItems.contentlets.find((item) => item.inode === inode);

                    // DotBulkFailItem.inode is optional; fall back so message args stay strings
                    const title = item?.title ?? inode ?? '';

                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get(
                            'content-drive.move-to-folder-error-with-title',
                            title
                        ),
                        detail: errorMessage,
                        life: ERROR_MESSAGE_LIFE
                    });
                });

                this.#store.cleanDragItems();
            });
    }

    protected onTableDrop(event: DotContentDriveItem) {
        if (!isFolder(event)) {
            return;
        }

        this.onMoveItems({
            targetFolder: {
                type: 'folder',
                path: event.path,
                hostname: this.#store.currentSite()?.hostname,
                id: event.identifier
            }
        });
    }

    protected getMoveMetadata(event: DotContentDriveMoveItems) {
        const dragItems = this.#store.dragItems();

        const path = event.targetFolder.path?.length > 0 ? event.targetFolder.path : '/';

        const pathToMove = `//${event.targetFolder.hostname}${path}`;

        const cleanPath = path.includes('/') ? path.split('/').filter(Boolean).pop() : path;

        const folderName = cleanPath && cleanPath.length > 0 ? cleanPath : pathToMove;

        return {
            pathToMove: pathToMove,
            folderName: folderName,
            assetCount: dragItems.contentlets.length + dragItems.folders.length,
            dragItems
        };
    }

    protected onSelectItems(items: DotContentDriveItem[]) {
        this.#store.setSelectedItems(items);
    }

    protected onTableScroll() {
        this.#store.resetContextMenu();
    }

    /**
     * A file drag entering the list dismisses the context menu, which would otherwise float over
     * the drop overlay. The dropzone reports the drag; deciding what it means stays here.
     */
    protected onDropzoneDragEnter() {
        this.#store.resetContextMenu();
    }
}
