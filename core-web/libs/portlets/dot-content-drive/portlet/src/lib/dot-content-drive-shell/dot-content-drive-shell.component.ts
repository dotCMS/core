import { signalMethod } from '@ngrx/signals';
import { of, SubscriptionLike } from 'rxjs';

import { Location, NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    OnDestroy,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService, SortEvent } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
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
    DotBulkUploadForm,
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
    DotKeyboardShortcutService,
    DotKeyboardShortcutUnregister,
    hasOverlayAbove,
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
    UPLOAD_BATCH_OPERATION,
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
import { provideContentDriveFieldFilterHost } from '../store/content-drive-field-filter-host';
import { provideContentDriveFilterFacade } from '../store/content-drive-filter-facade';
import { provideContentDriveRelationshipPicker } from '../store/content-drive-relationship-picker';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import {
    canAddChildrenTo,
    encodeFilters,
    isFolder,
    normalizeFolderRef,
    toFolderRef
} from '../utils/functions';
import { refuseOverCeiling } from '../utils/upload-ceilings';
import { describeUploadFailures } from '../utils/upload-failures';

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
        // Exposes the store to the shared filter chips through a store-agnostic seam, so a chip
        // written once serves this toolbar and the AssetPicker's. Must sit alongside the store, not
        // in `root`: the facade closes over whichever store instance this shell owns.
        provideContentDriveFilterFacade(),
        // The field-filter chips' own seam: which chips are shown, and the field metadata one fetch
        // feeds to the chips, this shell's results table and the store's request builder.
        provideContentDriveFieldFilterHost(),
        // The optional capability the shared field filter needs for Relationship fields. Content
        // Drive can supply it — `DotSelectExistingContentComponent` lives in a library this portlet
        // may import and `@dotcms/ui` may not — so the drive keeps exactly today's behaviour.
        DialogService,
        provideContentDriveRelationshipPicker(),
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
        class: 'grid relative h-full grid-cols-[min-content_1fr_min-content] grid-rows-[min-content_min-content_1fr]',
        // Bound here rather than with addEventListener: Angular unbinds it when the shell is
        // destroyed. A hand-rolled window listener outlives the portlet unless every teardown path
        // remembers to remove it, and then a stale closure keeps guarding the page on a count that
        // belongs to a component that is gone.
        '(window:beforeunload)': 'onBeforeUnload($event)'
    }
})
export class DotContentDriveShellComponent implements OnDestroy {
    readonly #store = inject(DotContentDriveStore);

    readonly #router = inject(Router);
    readonly #route = inject(ActivatedRoute);

    readonly #location = inject(Location);
    readonly #navigationService = inject(DotContentDriveNavigationService);

    readonly #shortcuts = inject(DotKeyboardShortcutService);

    /** Withdraws the portlet-level shortcut claims. Released in {@link ngOnDestroy}. */
    #withdrawShortcuts?: DotKeyboardShortcutUnregister;

    /** Browser history subscription for the edit-panel guard. Released in {@link ngOnDestroy}. */
    #locationSubscription?: SubscriptionLike;

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

    /**
     * Reports a successful Add to Bundle through the shared outcome pipeline.
     *
     * An arrow property, not a method: it is handed to the dialog as data and invoked from there,
     * so it has to carry its own `this`.
     *
     * Publishing a result rather than raising a toast is the point — the Workflow Center fires the
     * same operation and its wording, severity and reload all come from one place. A second toast
     * here would say the same thing differently and drift the moment either is edited.
     */
    protected readonly onBundleAdded = (): void => {
        this.#store.reportExternalResult({
            actionName: this.#dotMessageService.get('content-drive.action-center.add-to-bundle'),
            successCount: 1,
            skippedCount: 0,
            failedCount: 0,
            // Nothing in the listing changes when an asset joins a bundle, so this is one of the
            // few successes that still has to be said out loud.
            confirmSuccess: true
        });
    };

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
        // Called rather than assigned: its only purpose is the side effect, and a private field
        // nothing reads is exactly what `no-unused-private-class-members` is for.
        this.#registerShortcuts();

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
        this.#locationSubscription = locationSubscription;
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
     * `failedCount` downgrades the toast to a warning. Partial failure is a normal outcome for these
     * endpoints (a lock held by somebody else, a per-contentlet permission), and reporting it as an
     * unqualified success would be the one thing the user cannot recover from — the grid has already
     * reloaded and the selection is gone.
     */
    /**
     * Asks before the page is unloaded while a batch still has bytes in flight.
     *
     * The one moment the interface can intervene. Until the handle comes back there is no run: if
     * the page goes, the request dies with it, nothing is recorded and nobody is notified, so there
     * is nothing to resume and no outcome to report. Past the handle the run is the server's and
     * leaving is safe, which is why this stops asking then instead of guarding the whole
     * upload-and-run — the feature explicitly promises the author can walk away.
     *
     * `returnValue` alongside `preventDefault()`: the modern call is enough in current browsers,
     * the legacy assignment is what older ones read.
     */
    /**
     * Whether the route may be left, asked by this portlet's own `canDeactivate`.
     *
     * **Answers, rather than holding.** The shared `CanDeactivateGuardService` refuses by filtering
     * a subject, which leaves the navigation *pending* rather than cancelling it: releasing the
     * lock later lets that same navigation complete, so an author who clicked a link, was told to
     * wait and stayed put would be thrown out of the portlet at a moment they did not choose. UVE
     * wants that, because it force-saves and then continues. There is nothing to save here, and
     * nothing to continue: the answer is no, the navigation is cancelled, and the author decides
     * when to try again.
     *
     * Refusing silently reads as a broken link, so it says why.
     *
     * Only while bytes are still going. Past the handle the run is the server's, leaving is safe,
     * and holding the route would contradict the message that just said so.
     */
    canLeaveRoute(): boolean {
        if (!this.#uploadsInFlight()) {
            return true;
        }

        this.#messageService.add({
            severity: 'warn',
            summary: this.#dotMessageService.get('content-drive.upload'),
            detail: this.#dotMessageService.get('content-drive.file-upload-in-progress-detail'),
            life: WARNING_MESSAGE_LIFE
        });

        return false;
    }

    protected onBeforeUnload(event: BeforeUnloadEvent): void {
        if (!this.#uploadsInFlight()) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    }

    /**
     * Whether reloading the listing right now would take something away from the author.
     *
     * `loadItems` empties `selectedItems` unconditionally and replaces every row, so firing it
     * mid-task is not merely noisy: a run settling while rows are checked for a workflow action
     * silently discards that selection (FR-026, FR-043).
     *
     * Reads `$dialogVisible` rather than `$activeDialog`, which is deliberately held through
     * PrimeNG's close animation and so still reports a dialog that is already gone.
     */
    protected readonly $authorIsMidTask = computed(
        () =>
            this.$dialogVisible() || this.$selectedItems().length > 0 || !!this.$editPanelRequest()
    );

    /**
     * A backgrounded reload waiting for {@link $authorIsMidTask} to clear, carrying the folders the
     * run changed so {@link #currentFolderIsAffected} can be re-checked when it finally runs —
     * the author may have navigated in between.
     */
    readonly #reloadHeld = signal<{ affectedFolders?: string[] } | undefined>(undefined);

    /**
     * How many batches still have bytes in flight.
     *
     * A count, not a flag: uploads can overlap, and the page has to stay guarded until the last of
     * them has a handle.
     */
    readonly #uploadsInFlight = signal(0);

    /** Distinguishes overlapping upload runs, which share an operation and have no targets. */
    #uploadSequence = 0;

    /**
     * Whether the listing on screen can show what a run changed (FR-044).
     *
     * A run that declares no folders reloads regardless: every synchronous caller acts on rows in
     * front of the author, so the browsed folder is the changed one by construction, and only a
     * backgrounded run can settle after they have moved on.
     */
    readonly #currentFolderIsAffected = (affectedFolders?: string[]): boolean =>
        !affectedFolders?.length ||
        affectedFolders
            .map(normalizeFolderRef)
            .includes(toFolderRef(this.#store.currentSite()?.hostname, this.#store.path()));

    readonly actionExecutionResultEffect = effect(() => {
        const result = this.#store.actionExecutionResult();

        if (!result) {
            return;
        }

        const {
            actionName,
            successCount,
            skippedCount,
            failedCount,
            partialDetailKey,
            backgrounded,
            confirmSuccess,
            affectedFolders,
            failures,
            duplicateSubmission,
            baseType
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
        // A recognised resubmission is not a shortfall, whatever its counts say. Under the
        // collision branch a retry that worked collides on every file, so by the numbers it is a
        // total failure — and reporting it that way sends the author to delete and re-upload files
        // that were already correctly there, which is worse than offering no retry at all.
        const isPartial = !duplicateSubmission && (failedCount > 0 || skippedCount > 0);

        // Silent on a clean success, unless the operation leaves no visible trace.
        //
        // For most operations the listing already shows the outcome — the row published, moved,
        // unlocked or disappeared — so a notification repeats what the author can see, which is the
        // noise this feature set out to remove. A shortfall is different: the numbers and their
        // causes are not visible anywhere, and it is the case the author has to act on.
        //
        // `confirmSuccess` is for the operations whose success genuinely shows nowhere, such as Add
        // to Bundle and Push Publish.
        //
        // Only the *notification* is suppressed. The grid still reloads and the dialog still closes:
        // those are how the author sees the outcome, so skipping them would replace a redundant
        // message with no feedback at all.
        // `backgrounded` too: that outcome arrived unprompted, minutes after the author moved on, so
        // by definition nothing on screen reflects it — and with a dialog open the grid does not
        // even reload. Staying silent there would mean a run finished and the author never learned.
        const announce = isPartial || confirmSuccess || backgrounded;

        // A resubmission means opposite things by base type, so the copy cannot be one sentence
        // (FR-040b). For a file asset the unique index refuses the second writer, so the batch
        // collided and nothing was duplicated — the case this copy was written for. For a dotAsset
        // the index can never contend, so the batch ran again and every file now exists twice;
        // saying "nothing was duplicated" there points the author away from a folder they need to
        // look at.
        const detail = duplicateSubmission
            ? this.#dotMessageService.get(
                  'DOTASSET' === baseType
                      ? 'content-drive.upload.toast.already-uploaded-again'
                      : 'content-drive.upload.toast.already-uploaded',
                  String(failedCount + successCount)
              )
            : isPartial
              ? this.#dotMessageService.get(
                    // Actions whose failures and skips mean something other than permissions, locks and
                    // workflow steps say so themselves — see `partialDetailKey`.
                    partialDetailKey ?? 'content-drive.action-center.toast.executed-partial',
                    actionName,
                    String(successCount),
                    String(failedCount),
                    String(skippedCount)
                )
              : this.#dotMessageService.get(
                    'content-drive.action-center.toast.executed-detail',
                    actionName,
                    String(successCount)
                );

        // Named files and their reasons, grouped one line per reason, appended to the counts.
        // The counts say how many; only this says which and why, and that is the part the author
        // can act on. Empty for a clean run, so a success never grows a list.
        // Nothing to list for a recognised retry: its "failures" are the files already in place,
        // and naming them would be telling the author to fix what is correctly there.
        // What a folder itself refuses is not on the wire: a failure carries the file name and the
        // reason, never the mask that refused it. So the sentence that names what the folder *does*
        // accept is available only while the batch's target is the folder on screen, and the
        // generic one stands for every other case.
        //
        // Strictly one affected folder, and strictly the selected one. A result for somewhere else
        // — or a run spanning several folders — would otherwise explain this folder's rule to an
        // author who was refused by another's, which is worse than saying nothing about the rule.
        const affectedRefs = (affectedFolders ?? []).map(normalizeFolderRef);
        const refusingFolderIsOnScreen =
            affectedRefs.length === 1 &&
            affectedRefs[0] ===
                toFolderRef(this.#store.currentSite()?.hostname, this.#store.path());

        // Narrowed the same way the upload itself narrows the selection: the tree's load-more row
        // is a node without a folder behind it, so it carries no filter to name.
        const selectedNodeData = this.#store.selectedNode()?.data;
        const selectedFolder =
            selectedNodeData && selectedNodeData.type !== LOAD_MORE_NODE_TYPE
                ? (selectedNodeData as DotFolderTreeNodeContentData)
                : undefined;

        const failureGroups = duplicateSubmission
            ? []
            : describeUploadFailures(
                  failures,
                  (key, ...args) => this.#dotMessageService.get(key, ...args),
                  {
                      folderFilter: refusingFolderIsOnScreen
                          ? selectedFolder?.filesMasks
                          : undefined
                  }
              );

        if (announce) {
            // One message per severity (developer's call), and the counts ride with the first of
            // them. Two reasons for the split: an author reading "2 failed" wants to know which of
            // those they can go and fix, and a wall they cannot pass should not arrive wearing the
            // same colour as a file that needs renaming.
            //
            // A run with no per-file detail still gets exactly one message, because the counts
            // alone are an outcome — the groups are what varies, never whether anything is said.
            const messages = failureGroups.length
                ? failureGroups.map((group, index) => ({
                      severity: group.severity,
                      summary: this.#dotMessageService.get(
                          'error' === group.severity
                              ? 'content-drive.upload.toast.failed'
                              : 'content-drive.upload.toast.incomplete'
                      ),
                      // The counts belong to the batch, not to a severity, so they are stated once
                      // and in the message the author reads first.
                      detail: [...(index === 0 ? [detail] : []), ...group.lines].join('<br>'),
                      life: WARNING_MESSAGE_LIFE
                  }))
                : [
                      {
                          // A skip is a shortfall too — those items did not get the action — so it
                          // warns rather than reporting green, which is what it used to do.
                          //
                          // A recognised resubmission warns as well, for a different reason:
                          // nothing the author asked for happened. It is not the failure the counts
                          // describe, but it is not an accomplishment either, and a green message
                          // invites them to move on when they should look at the folder.
                          severity: isPartial || duplicateSubmission ? 'warn' : 'success',
                          summary: this.#dotMessageService.get(
                              isPartial || duplicateSubmission
                                  ? 'content-drive.upload.toast.incomplete'
                                  : 'content-drive.action-center.toast.executed'
                          ),
                          detail,
                          life:
                              isPartial || duplicateSubmission
                                  ? WARNING_MESSAGE_LIFE
                                  : SUCCESS_MESSAGE_LIFE
                      }
                  ];

            messages.forEach((message) => this.#messageService.add(message));
        }

        untracked(() => {
            // A backgrounded outcome arrives unprompted, so it must not disturb whatever the user is
            // doing when it lands. Every other result settles a request they are waiting on, so it
            // reloads straight away — holding it would read as the action having done nothing.
            if (!backgrounded || !this.$authorIsMidTask()) {
                // Contentlets have moved step, so the grid is stale; `loadItems` also drops the
                // selection the run consumed.
                //
                // Quiet: the run marked its rows, so a skeleton here would be a second load
                // right after the first and would read as a jump.
                if (this.#currentFolderIsAffected(affectedFolders)) {
                    this.#store.loadItems({ quiet: true });
                }
            } else {
                // Held, not dropped (FR-043). Dropping it left the grid stale for as long as the
                // author stayed in the portlet: the run settled, the rows changed, and nothing
                // would ever fetch them again. `#flushHeldReload` runs it at the next boundary.
                this.#reloadHeld.set({ affectedFolders });
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

    /**
     * Runs a reload that was held while the author was mid-task, as soon as they are not.
     *
     * The guard is read first and tracked so this re-runs the moment it clears; the held flag is
     * read untracked and consumed on the way out, so one held reload produces exactly one refetch
     * rather than one per later dialog open and close.
     */
    readonly flushHeldReloadEffect = effect(() => {
        const midTask = this.$authorIsMidTask();

        untracked(() => {
            const held = this.#reloadHeld();

            if (midTask || !held) {
                return;
            }

            this.#reloadHeld.set(undefined);

            if (this.#currentFolderIsAffected(held.affectedFolders)) {
                this.#store.loadItems({ quiet: true });
            }
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

    /**
     * Claims the portlet-level shortcuts (issue #32591).
     *
     * Registered as one batch so there is a single withdrawal to hold, and withdrawn when the shell
     * is destroyed — which is what lets a dialog opening over the portlet take a combination and
     * hand it back on close.
     */
    #registerShortcuts(): void {
        this.#withdrawShortcuts = this.#shortcuts.register([
            {
                combination: 'escape',
                label: 'content-drive.shortcut.escape',
                handler: () => this.#onEscape()
            },
            {
                combination: 'mod+b',
                label: 'content-drive.shortcut.toggle-tree',
                handler: () => this.#onToggleTree()
            }
        ]);
    }

    /**
     * One teardown path for the whole shell.
     *
     * Both of these outlive Angular's own cleanup if left alone: the shortcut claims sit in a
     * root-provided registry, and the history subscription is a plain RxJS one. Withdrawing the
     * claims here is also what lets a dialog that shadowed a combination get it handed back.
     */
    ngOnDestroy(): void {
        this.#withdrawShortcuts?.();
        this.#locationSubscription?.unsubscribe();
    }

    /**
     * Collapses or expands the folder tree.
     *
     * Stands down while an overlay is above the portlet, for the same reason Escape does: a dialog
     * covers the tree, so the toggle would rearrange a layout the user cannot see and they would
     * find it changed when the dialog closes. Declining also leaves the combination free for
     * whatever is on top, which may want it — a rich text surface inside a dialog reads Cmd+B as
     * bold.
     */
    #onToggleTree(): boolean {
        if (hasOverlayAbove()) {
            return false;
        }

        this.#store.setIsTreeExpanded(!this.#store.isTreeExpanded());

        return true;
    }

    /**
     * Clears the selection, and nothing else.
     *
     * Deliberately does *not* clear filters, though an earlier revision did. Escape is a
     * high-frequency "back out" key and an assembled filter set is expensive to rebuild by hand, so
     * putting a destructive, hard-to-undo action behind a single stray keypress is the wrong trade.
     * Clearing filters stays on the toolbar's "Clear all" control, which is visible, labelled, and
     * only offered when there is something to clear.
     *
     * Declines when there is no selection, so Escape keeps whatever meaning it has elsewhere instead
     * of being silently swallowed here.
     */
    #onEscape(): boolean {
        // Stand down while any overlay is above this listing. PrimeNG keeps its own `closeOnEscape`
        // handling — a `<p-dialog>` binds its own document listener and closes on a z-index
        // comparison, never consulting `defaultPrevented` — so without this both fire: Escape
        // dismisses the dialog *and* silently wipes every active filter, or wipes the very selection
        // the Action Center is operating on.
        //
        // Asked of the z-index stack rather than a list of visibility signals, so confirm popups,
        // select panels and any dialog added later are covered without anyone remembering to extend
        // a list. The side panel asks the same question against its own container.
        if (hasOverlayAbove()) {
            return false;
        }

        if (this.#store.selectedItems().length) {
            this.#store.setSelectedItems([]);

            return true;
        }

        return false;
    }

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

        this.uploadByBaseType(Array.from(files), baseType, targetFolder);
    }

    /**
     * Submits the chosen files as one batch, resolving the content type from the given base type
     * (`DOTASSET` for Assets, `FILEASSET` for Files).
     *
     * One path for any number of files: a lone file is a batch of length one, so nothing forks on
     * count and there is a single set of gates to keep right. The warning that only one file would
     * be uploaded went with the fork that made it true.
     *
     * @protected
     * @param {File[]} files Every file the author chose, in the order they chose them
     * @param {string} baseType
     * @param {DotFolderTreeNodeData} [hostFolder]
     * @memberof DotContentDriveShellComponent
     */
    protected uploadByBaseType(
        files: File[],
        baseType: string,
        hostFolder?: DotFolderTreeNodeData
    ) {
        // The courtesy refusal, in front of the server's own. Both ceilings are the server's and it
        // stays the enforcement point; what changes is that the author is told in the file chooser
        // instead of after waiting out the upload of a batch that was never going to be accepted,
        // and the sentence can name the limit rather than saying "fewer".
        //
        // No advertised ceiling means no check here: the server refuses as it always did, with the
        // copy that names no number (see {@link #describeSubmissionRefusal}).
        const refusal = refuseOverCeiling(files, this.#store.uploadCeilings());

        if (refusal) {
            this.#messageService.add({
                severity: 'error',
                summary: this.#dotMessageService.get('content-drive.add-dotasset-error'),
                detail: this.#dotMessageService.get(refusal.key, ...refusal.args),
                life: ERROR_MESSAGE_LIFE
            });

            // Before the run is registered, deliberately: nothing was submitted, so nothing is in
            // flight, and a run started here would leave the indicator lit and the route guarded
            // for an upload that never happened.
            return;
        }

        // Reported from here rather than from the 202, because this is the part that takes time.
        // Until the handle comes back the author has no sign anything is happening, and a thirty-file
        // batch can spend a long while in exactly that state.
        const runId = this.#store.startExternalRun({
            // Unique per batch, because the run key is `operation:targets` and the targets below
            // are deliberately empty — every upload would otherwise share one key, the second
            // overwriting the first and the first to finish deregistering both. Unlike a workflow
            // action there is nothing to guard against here: each submission carries its own
            // freshly chosen files, so two uploads at once is legitimate rather than a double-fire.
            operation: `${UPLOAD_BATCH_OPERATION}:${(this.#uploadSequence += 1)}`,
            actionName: this.#dotMessageService.get('content-drive.upload'),
            total: files.length,
            // `||`, not `??`: the site root's node carries an *empty* path, which is present but
            // names nothing, so the indicator would read "Applying Upload to " with a blank target.
            targetLabel: hostFolder?.path || this.#store.currentSite()?.hostname,
            // Empty on purpose. The indicator speaks only for runs with nothing to mark, since a
            // run over rows is already reported by those rows dimming. An upload's content does not
            // exist until the run creates it, so the indicator is its only surface — naming the
            // files here is what excluded it from the one place it can be seen.
            targets: []
        });

        // The route answers from this count directly (see {@link canLeaveRoute}), so there is no
        // lock to set: the guard asks, and while this is above zero the answer is no.
        this.#uploadsInFlight.update((count) => count + 1);

        // The upload phase ends at the handle, whichever way it ends. Leaving its run registered
        // would spin the indicator for a run that has become the server's to report.
        const settleUploadPhase = () => {
            this.#store.endExternalRun(runId);

            const remaining = Math.max(this.#uploadsInFlight() - 1, 0);
            this.#uploadsInFlight.set(remaining);

            // Nothing to release: the count *is* the answer, and uploads overlap, so the route
            // reopens exactly when the last of them reaches its handle.
        };

        this.#fileService
            .uploadFilesByBaseType(files, {
                baseType: baseType as DotBulkUploadForm['baseType'],
                // Two fields because the contract states the intent rather than overloading one,
                // and the distinction is `path`, not `id`.
                //
                // The tree's root row stands for the *site*, and `createSiteNode` gives it the
                // site's identifier as `id` and `type: 'folder'` like every other row — so "has an
                // id" reads as "is a folder" and sends a site id as `folderId`, which the server
                // answers 404 to, correctly: that folder does not exist. An empty `path` is what
                // marks the row as the site itself.
                ...(hostFolder?.id && hostFolder.path
                    ? { folderId: hostFolder.id }
                    : {
                          siteId: hostFolder?.id ?? this.#store.currentSite()?.identifier ?? ''
                      })
            })
            .subscribe({
                next: (event) => {
                    if (event.kind === 'progress') {
                        // Only where the browser could compute a length. Without one, reporting 0%
                        // would render a bar stuck at nothing, which reads as stalled rather than
                        // as unmeasurable — the indicator falls back to a bare spinner instead.
                        if (event.total) {
                            this.#store.updateExternalRun(runId, {
                                percent: Math.round((event.loaded / event.total) * 100)
                            });
                        }

                        return;
                    }

                    // Discriminated explicitly rather than treating "not progress" as the handle:
                    // the union can grow, and assuming an unknown event carries one would settle
                    // the phase on nothing and then read a jobId off undefined.
                    if (event.kind !== 'accepted') {
                        return;
                    }

                    settleUploadPhase();

                    // The batch is not over, only this half of it: dotCMS is still creating and
                    // publishing the files, and that is usually the longer wait. A second run
                    // carries it, so the indicator stays lit until the completion arrives instead
                    // of going dark at the handle and reading as "it stopped".
                    //
                    // Its own copy, because the words have to change with the guarantee: the first
                    // phase was an operation the author had to stay for, this one is work they have
                    // just been told they can walk away from.
                    const backgroundRunId = this.#store.startExternalRun({
                        operation: `${UPLOAD_BATCH_OPERATION}:${event.handle.jobId}`,
                        actionName: this.#dotMessageService.get('content-drive.upload'),
                        labelKey: 'content-drive.upload.indicator.background',
                        total: files.length,
                        targetLabel: hostFolder?.path || this.#store.currentSite()?.hostname,
                        targets: []
                    });

                    // The handle is the only way to tell this batch's completion from another
                    // tab's: the event is scoped to the submitting user, not to a window. The
                    // destination travels with it because by the time it lands the author may be
                    // looking at a different folder, and the outcome decides whether the listing
                    // they are on can show the result at all.
                    this.#store.trackUploadJob(
                        event.handle.jobId,
                        [
                            toFolderRef(
                                hostFolder?.hostname ?? this.#store.currentSite()?.hostname,
                                // Same reason: an empty path is the site root, which normalises to
                                // `//hostname` — the ref the listing computes when browsing it.
                                hostFolder?.path || '/'
                            )
                        ],
                        backgroundRunId,
                        // Carried to the outcome because a resubmission means opposite things by
                        // base type, and by the time the completion lands nothing else knows which
                        // one ran (FR-040b).
                        baseType
                    );

                    // The one notification this flow raises, and the only in-flight fact worth
                    // one: until the handle existed, leaving lost the batch and the page guard
                    // said so; now leaving costs nothing. That rule changed with no visible
                    // cause, and the indicator cannot report it — it says work is happening, not
                    // that the author is released from it.
                    this.#messageService.add({
                        severity: 'info',
                        summary: this.#dotMessageService.get(
                            'content-drive.upload.toast.backgrounded'
                        ),
                        detail: this.#dotMessageService.get(
                            'content-drive.upload.toast.backgrounded-detail',
                            String(files.length)
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });

                    // Nothing else to do, and deliberately nothing. A `202` means the batch is queued,
                    // not that any file exists, so reloading here refetches a folder whose files
                    // have not been created — the author watches the listing refresh to show
                    // nothing. The reload belongs to the completion event, which arrives with the
                    // outcome and knows which folders the run actually changed.
                    //
                    // Nor is anything announced: an accepted submission is not an outcome, and the
                    // toasts that used to say "started" are what the in-flight indicator replaced.
                },
                error: (error) => {
                    settleUploadPhase();

                    // Only a refused *submission* lands here. Once a handle exists the run is the
                    // server's, and its failures arrive as per-file reasons in the outcome.
                    // A log, not `DotHttpErrorManagerService`: that service answers a status with
                    // its own dialog and can redirect, which would stack a vaguer second account of
                    // the same refusal on top of the toast below. Carries the status and the batch
                    // size so a ceiling refusal can be told from a transport failure without
                    // reproducing it.
                    console.error(
                        `Content drive upload refused: status ${error?.status ?? 'none'}, ${files.length} file(s)`,
                        error
                    );
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-error'),
                        detail: this.#describeSubmissionRefusal(error, baseType),
                        life: ERROR_MESSAGE_LIFE
                    });
                }
            });
    }

    /**
     * The sentence shown when a submission never became a run at all.
     *
     * The endpoint enforces two ceilings and keeps them distinguishable by *status*: too much data
     * is answered `413`, too many files `400` (`BulkUploadRefusedExceptionMapper`). They have
     * different fixes, so the status picks the copy rather than the body: the server's own message
     * names byte counts and part limits, which is a sentence written for a developer reading a log,
     * not for the author who just dropped the files.
     *
     * Anything else gets the generic copy, and the server's sentence goes to the log instead of the
     * toast. FR-030 draws that line for every outcome in this portlet, and the folder dialogs were
     * corrected to it earlier on this branch: a message written for whoever reads the log names
     * staging paths, byte counts and class names, none of which an author can act on. The two
     * ceilings are the cases worth distinguishing, and they now have copy of their own, so there is
     * nothing left the raw sentence would say better.
     */
    #describeSubmissionRefusal(error: HttpErrorResponse, baseType: string): string {
        if (error?.status === HttpStatusCode.PayloadTooLarge) {
            return this.#dotMessageService.get('content-drive.upload.refused.too-large');
        }

        // A `400` is only *attributed* to the file count where nothing else could have caused it.
        // The ceiling mapper is not this endpoint's only source of one: the resource rejects a bad
        // referer with a `400`, and a malformed `form` part produces one from Jackson before any of
        // this feature's code runs. Naming the count for those sends the author to remove files
        // from a batch whose size was never the problem.
        //
        // "Nothing else could have caused it" means no ceiling was advertised, so the client could
        // not check up front — and a count refusal is then the only `400` the contract documents.
        // Where a ceiling *is* advertised, an over-ceiling batch was already refused in the chooser
        // with the number named, so a `400` arriving here is something else by construction.
        if (error?.status === HttpStatusCode.BadRequest && !this.#store.uploadCeilings()) {
            return this.#dotMessageService.get('content-drive.upload.refused.too-many-files');
        }

        // Whether the batch's fate is *knowable* decides the advice, and the status says which.
        // A 4xx is the server answering and refusing: nothing was created, so retrying is free
        // whatever the base type. No response at all or a 5xx is the uncertain case — the request
        // may have been accepted and then failed — and there the base type matters: retrying a
        // file asset costs nothing because the unique index refuses the second copy (FR-037),
        // while retrying a dotAsset can leave two copies of everything because that index can
        // never contend for one (FR-037a). Only then is the folder named as the thing to check,
        // which is the single case overriding FR-037's "never asks them to check the folder
        // first".
        const outcomeUnknown = !error?.status || error.status >= HttpStatusCode.InternalServerError;

        return this.#dotMessageService.get(
            outcomeUnknown && 'DOTASSET' === baseType
                ? 'content-drive.add-dotasset-error-detail-check-folder'
                : 'content-drive.add-dotasset-error-detail'
        );
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
                    // Silent on success: the rows left the folder in front of the author.
                    // Quiet: the moved rows were marked busy.
                    this.#store.loadItems({ quiet: true });
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
