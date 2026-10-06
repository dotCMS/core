import { signalMethod } from '@ngrx/signals';
import { forkJoin, Observable, of, SubscriptionLike } from 'rxjs';

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
import { toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService, SortEvent, ToastMessageOptions } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { Popover, PopoverModule } from 'primeng/popover';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { catchError, filter, take } from 'rxjs/operators';

import {
    AddToBundleService,
    DotCurrentUserService,
    DotFolderBulkDeleteRefusalKind,
    DotFolderBulkDuplicateRefusalKind,
    DotFolderService,
    DotUploadFileService,
    DotWorkflowsActionsService,
    DotMessageService,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotHttpErrorManagerService,
    DotPermissionsService
} from '@dotcms/data-access';
import {
    ContextMenuData,
    DotBulkUploadForm,
    DotCMSBaseTypesContentTypes,
    DotCMSContentTypeField,
    DotCMSDataTypes,
    DotCMSFieldTypes,
    DotContentDriveActionableFolder,
    DotContentDriveBrowseItem,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    DotFolderBean,
    DotSite,
    isActionableBrowseItem,
    DotCMSContentlet
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
    DotStatusToastComponent,
    DotToastComponent,
    DotUploadDropzoneComponent,
    DotUploadTypeSelectorComponent,
    STATUS_TOAST_KEY,
    DotJspIframeDialogComponent
} from '@dotcms/ui';

import { DotContentDriveActionCenterComponent } from '../components/dialogs/dot-content-drive-action-center/dot-content-drive-action-center.component';
import { DotContentDriveDialogContentTypeSelectorComponent } from '../components/dialogs/dot-content-drive-dialog-content-type-selector/dot-content-drive-dialog-content-type-selector.component';
import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
import { DotContentDriveScopeBarComponent } from '../components/dot-content-drive-scope-bar/dot-content-drive-scope-bar.component';
import { DotContentDriveSidebarComponent } from '../components/dot-content-drive-sidebar/dot-content-drive-sidebar.component';
import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import { DotLegacyEditorSidePanelComponent } from '../components/dot-legacy-editor-side-panel/dot-legacy-editor-side-panel.component';
import {
    ACTION_CENTER_DIALOG_CONTENT_STYLE,
    ACTION_CENTER_DIALOG_CLASS,
    CONTENT_DRIVE_URL_PARAM,
    DIALOG_TYPE,
    SORT_ORDER,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE,
    ERROR_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID,
    UPLOAD_BATCH_OPERATION,
    ROOT_PATH,
    SYSTEM_HOST
} from '../shared/constants';
import { DotLegacyEditorPageRequest, DotLegacyEditorSaved } from '../shared/legacy-editor.models';
import {
    DotContentDriveActionExecutionResult,
    DotContentDriveContentTypeSelectorPayload,
    DotContentDriveDialog,
    DotContentDriveOutcomeKind,
    DotContentDriveOutcomeReading,
    DotContentDriveSortOrder,
    DotContentDriveStatus,
    DotContentDriveUploadBaseType,
    DotContentDriveUploadSelection,
    DotContentDriveUploadSelectorPayload,
    OUTCOME_KIND,
    DotContentDriveFolderPermissionsPayload
} from '../shared/models';
import {
    DotContentDriveNavigationService,
    provideContentDriveNavigationOverride
} from '../shared/services';
import { provideContentDriveFieldFilterHost } from '../store/content-drive-field-filter-host';
import { provideContentDriveFilterFacade } from '../store/content-drive-filter-facade';
import { provideContentDriveRelationshipPicker } from '../store/content-drive-relationship-picker';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { describeFolderDeleteOutcome } from '../utils/folder-delete-outcome';
import { describeFolderDuplicateOutcome } from '../utils/folder-duplicate-outcome';
import { folderPermissionsDialogConfig } from '../utils/folder-permissions-dialog';
import {
    canAddChildrenTo,
    encodeFilters,
    isFolder,
    browsedFolderRef,
    normalizeFolderRef,
    panelParamOf,
    resolveContentDriveUrlIntent,
    toFolderRef,
    uploadIndicatorKey,
    folderDialogParamOf,
    toActionableFolder
} from '../utils/functions';
import { refuseOverCeiling } from '../utils/upload-ceilings';
import { describeUploadFailures, DotUploadFailureGroup } from '../utils/upload-failures';

/**
 * Whether the store holds the admin's real site. Until the site loads, `initContentDrive` stores
 * `SYSTEM_HOST` in its place, the same placeholder `loadItems` skips the search for.
 *
 * @param site The store's current site.
 * @returns Whether it is a loaded site.
 */
function isLoadedSite(site: DotSite | undefined): site is DotSite {
    return !!site && site.identifier !== SYSTEM_HOST.identifier;
}

/** What the user may do to a folder, as `DotPermissionsService.getUserAccess` answers it. */
type DotFolderUserAccess = { canEdit: boolean; canEditPermissions: boolean };

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
        DotStatusToastComponent,
        DotToastComponent,
        DotEditContentSidePanelComponent,
        // Remove with the legacy editor.
        DotLegacyEditorSidePanelComponent,
        ProgressSpinnerModule,
        DotContentDriveActionCenterComponent,
        DotContentDriveScopeBarComponent,
        ButtonModule
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
        // Drive can supply it — the dialog lives in a library this portlet
        // may import and `@dotcms/ui` may not — so the drive keeps exactly today's behaviour.
        DialogService,
        provideContentDriveRelationshipPicker(),
        // Component-scoped (not `root`) so it can inject the shell's DotContentDriveStore to read
        // the list's language filter and default language; shared with the child components in
        // this shell's subtree.
        DotContentDriveNavigationService,
        // Lets the new-editor side panel's "switch to the old editor" and load error stay in
        // Content Drive. Only this shell provides it (#37759, FR-028, FR-029).
        // Remove with the legacy editor.
        provideContentDriveNavigationOverride(),
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
        class: 'grid relative h-full grid-cols-[min-content_1fr_min-content] grid-rows-[min-content_min-content_min-content_1fr]',
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
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dialogService = inject(DialogService);
    readonly #folderService = inject(DotFolderService);
    readonly #permissionsService = inject(DotPermissionsService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    /**
     * The admin's current site, once it has loaded. Built here because `toObservable` needs the
     * injection context; read through {@link #currentSite$}.
     */
    readonly #siteLoaded$ = toObservable(this.#store.currentSite).pipe(
        filter(isLoadedSite),
        take(1)
    );

    /** The open Edit Permissions dialog, which the shell opens itself (see {@link #syncDialog}). */
    #permissionsDialogRef: DynamicDialogRef | null = null;

    /** Whether the last URL write named an open folder dialog; drives push vs replace. */
    #folderDialogUrlWasSet = false;

    /**
     * A `createFolder` link asked for New Folder, until it has opened (#37759, FR-031). Waits for
     * the tree, like a `createContent` link, so the folder `path` names is the one it opens on.
     */
    readonly #pendingNewFolder = signal(false);

    // eslint-disable-next-line no-unused-private-class-members -- effect() runs for its side effects; the field only holds the EffectRef
    #openPendingNewFolderEffect = effect(() => {
        if (!this.#pendingNewFolder()) {
            return;
        }

        if (this.#store.folders().length === 0 || this.#store.sidebarLoading()) {
            return;
        }

        untracked(() => {
            this.#pendingNewFolder.set(false);

            // Same gate as the toolbar's New Folder entry: where it is not offered, nothing opens
            // and the link says why (US8/AC5).
            if (!this.#store.$canAddChildren()) {
                this.#reportForbidden();

                return;
            }

            this.#store.setDialog({
                type: DIALOG_TYPE.FOLDER,
                header: this.#dotMessageService.get('content-drive.dialog.folder.header')
            });
        });
    });

    /**
     * The content type a `createContent` link asked to create, until its form has opened. Read from
     * the URL once, on load (#37759, FR-024).
     */
    readonly #pendingCreate = signal<string | null>(null);

    /**
     * Opens the create form a `createContent` link asked for, once Content Drive knows where and in
     * which language: after the folder tree's first load (so the folder `path` names is resolved)
     * and after the default language (which a create starts in without a language filter). Waiting
     * on the tree rather than on a selected folder matters: whole-site content has no tree row, so
     * a link with no `path` never gets one.
     */
    // eslint-disable-next-line no-unused-private-class-members -- effect() runs for its side effects; the field only holds the EffectRef
    #openPendingCreateEffect = effect(() => {
        const contentType = this.#pendingCreate();

        // Checked first, so nothing else is tracked once the link has been handled (or never was).
        if (!contentType) {
            return;
        }

        const ready =
            this.#store.folders().length > 0 &&
            !this.#store.sidebarLoading() &&
            this.#store.defaultLanguageLoaded();

        if (!ready) {
            return;
        }

        untracked(() => {
            this.#pendingCreate.set(null);
            this.#navigationService.createContent(
                contentType,
                this.#navigationService.currentFolder()
            );
        });
    });

    /** Edit Content side panel request, driven by the navigation service; read by the template. */
    protected readonly $editPanelRequest = this.#navigationService.$editPanelRequest;

    /**
     * Legacy-editor side panel request, driven by the navigation service; read by the template.
     * Remove with the legacy editor.
     */
    protected readonly $legacyPanelRequest = this.#navigationService.$legacyPanelRequest;

    /** Whether either side panel is open. */
    readonly #anyPanelOpen = computed(
        () => !!this.$editPanelRequest() || !!this.$legacyPanelRequest()
    );

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

    /**
     * The open side panel, whichever editor it hosts, so browser Back can route its close through
     * that panel's unsaved-changes guard.
     *
     * @returns The rendered panel, or `undefined` when none is rendered yet.
     */
    #activePanel(): Pick<DotEditContentSidePanelComponent, 'requestClose'> | undefined {
        return this.$legacyPanel() ?? this.$sidePanel();
    }

    /**
     * The rendered legacy-editor panel, so browser Back can route its close through it.
     * Remove with the legacy editor.
     */
    protected readonly $legacyPanel =
        viewChild<DotLegacyEditorSidePanelComponent>('legacyPanelRef');

    readonly $items = this.#store.items;

    /**
     * The tree's VISUAL expanded state (drives width/animation). Combines the user's real
     * preference with any transient collapse the side panel is forcing — see
     * `isTreeVisuallyExpanded` on the store for why these are kept separate.
     */
    readonly $treeExpanded = this.#store.isTreeVisuallyExpanded;

    /**
     * Whether the scope bar has anything to say.
     *
     * Only all site content gets one: the site root and System Host each answer the question the
     * bar asks simply by being chosen, leaving nothing for its sentence to qualify or its toggle to
     * decide.
     */
    readonly $allSiteContentSelected = this.#store.$allSiteContentSelected;

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
    protected onBundleAdded(): void {
        this.#store.reportExternalResult({
            actionName: this.#dotMessageService.get('content-drive.action-center.add-to-bundle'),
            successCount: 1,
            skippedCount: 0,
            failedCount: 0,
            // Nothing in the listing changes when an asset joins a bundle, so this is one of the
            // few successes that still has to be said out loud.
            confirmSuccess: true
        });
    }

    /** Inodes any in-flight run is acting on, so the grid can mark those rows. */
    readonly $busyRows = this.#store.allBusyRows;

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
        const panelOpen = this.#anyPanelOpen();

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
     * @see ACTION_CENTER_DIALOG_CLASS
     */
    readonly $dialogRootClass = computed(() =>
        this.$activeDialog()?.type === DIALOG_TYPE.ACTION_CENTER ? ACTION_CENTER_DIALOG_CLASS : ''
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
        // Edit Permissions is the legacy permissions JSP in a dialog of its own, not this shell's
        // `p-dialog` (#37759, FR-030).
        if (dialog?.type === DIALOG_TYPE.FOLDER_PERMISSIONS) {
            this.#openPermissionsDialog(dialog);

            return;
        }

        if (this.#permissionsDialogRef) {
            const ref = this.#permissionsDialogRef;
            this.#permissionsDialogRef = null;
            ref.close();
        }

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
        // Each of these reacts to its one signal only; whatever else it reads is a snapshot.
        this.#reportActionExecutionResult(this.#store.actionExecutionResult);
        this.#reportFolderDeleteRefusal(this.#store.folderDeleteRefusal);
        this.#reportFolderDuplicateRefusal(this.#store.folderDuplicateRefusal);

        // Shareable deep-link: a panel or folder-dialog param reopens what it names on load. Read
        // once from the snapshot (the portlet is not re-created on in-session query-param changes).
        // `editContentLang` names the exact version to reopen: an identifier has one version per
        // language, so without it the resolver can only guess.
        const intent = resolveContentDriveUrlIntent(this.#route.snapshot.queryParams);
        if (intent.kind === 'edit') {
            this.#navigationService.openEditByIdentifier(intent.identifier, intent.languageId);
        } else if (intent.kind === 'create') {
            this.#pendingCreate.set(intent.contentType);
        } else if (intent.kind === 'createFolder') {
            this.#pendingNewFolder.set(true);
        } else if (intent.kind === 'editFolder') {
            this.#openFolderSettingsFromUrl(intent.identifier);
        } else if (intent.kind === 'folderPermissions') {
            this.#openFolderPermissionsFromUrl(intent.identifier);
        } else if (intent.kind === 'conflict') {
            // Params that can't hold together: Content Drive can't tell which one the author meant,
            // so it opens nothing and removes all of them, keeping the rest of the URL (FR-023).
            const cleared = Object.fromEntries(
                Object.values(CONTENT_DRIVE_URL_PARAM).map((param) => [param, null])
            );
            this.#location.replaceState(
                this.#router
                    .createUrlTree([], { queryParams: cleared, queryParamsHandling: 'merge' })
                    .toString()
            );
        }

        // Browser Back/Forward: the open panel's params are written via `Location.go` (no router
        // navigation), so nothing else reacts to popstate. When Back removes or changes the param
        // that names the open panel (edit OR create), route the close through the panel's
        // unsaved-changes guard — a direct `closeEditPanel()` would tear the editor down and discard
        // unsaved edits silently.
        const locationSubscription = this.#location.subscribe((event) => {
            const params = new URLSearchParams(event.url?.split('?')[1] ?? '');

            // An open folder dialog closes when Back drops its param, the same way its own close
            // does (#37759, FR-032). It has no unsaved-changes guard to go through.
            const folderParam = folderDialogParamOf(this.#store.dialog());
            if (folderParam) {
                if (params.get(folderParam.key) !== folderParam.value) {
                    this.#closeFolderDialog();
                }

                return;
            }

            const location = this.#navigationService.$panelLocation();
            if (!location) {
                return;
            }

            const { key, value } = panelParamOf(location);

            if (params.get(key) !== value) {
                // Restore the param so the URL matches the still-open panel while the guard decides.
                // `replaceState` (not `go`) avoids piling up history entries. Discard → the panel
                // emits `closed` → onEditPanelClosed → closeEditPanel clears the param; Keep editing
                // → the panel stays open and the URL is already back in sync.
                const restoredUrl = this.#router
                    .createUrlTree([], {
                        queryParams: { [key]: value },
                        queryParamsHandling: 'merge'
                    })
                    .toString();
                this.#location.replaceState(restoredUrl);
                this.#activePanel()?.requestClose();
            }
        });
        this.#locationSubscription = locationSubscription;
    }

    readonly $offset = computed(() => this.#store.pagination().offset, {
        equal: (a, b) => a === b
    });

    readonly $loading = computed(() => this.#store.status() === DotContentDriveStatus.LOADING);

    /**
     * Whether the last search failed to run, as opposed to running and matching nothing.
     *
     * The two used to be indistinguishable: a query that could not execute was logged and reported
     * as an empty result, so a user searching for content they were looking at was told it did not
     * exist (issue #37532). The store now records the failure; this is what puts it on screen.
     */
    readonly $searchFailed = computed(() => this.#store.status() === DotContentDriveStatus.ERROR);

    /**
     * Re-runs the current search after a failure. `loadItems` re-reads the live filter state and
     * resets the status itself, so the retry needs nothing beyond the call.
     */
    protected onRetrySearch(): void {
        this.#store.loadItems();
    }

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
        () => this.$dialogVisible() || this.$selectedItems().length > 0 || this.#anyPanelOpen()
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
        // All site content lists what is inside folders, so a run anywhere on the site can
        // change it.
        this.#store.$allSiteContentSelected() ||
        affectedFolders
            .map(normalizeFolderRef)
            .includes(browsedFolderRef(this.#store.currentSite()?.hostname, this.#store.path()));

    /**
     * The action currently being applied, surfaced here because the run outlives the Action Center
     * dialog. Once the user closes that dialog the toolbar is the only place still reporting the run,
     * so without this the work would continue with no indication until the completion toast fired.
     */
    readonly $actionExecution = this.#store.toolbarRun;

    /**
     * How many runs are in flight. With several at once the store leaves the run undefined on
     * purpose, so keying anything off the run alone would go quiet exactly when the most is
     * happening.
     */
    readonly $activeRunCount = this.#store.toolbarRunCount;

    readonly $hasRunInFlight = computed(() => this.$activeRunCount() > 0);

    /**
     * Resolved indicator label. Built here rather than in the template because `DotMessagePipe` takes
     * `string[]` arguments and the item count is a number.
     *
     * The action name is escaped because this label is bound with `[innerHTML]` — the message itself
     * carries a `<b>`, which is the only reason it is not plain interpolation. For a workflow action
     * that name is `WorkflowAction.name` straight from the backend, so without this a name containing
     * markup becomes real DOM. Angular's sanitizer already drops event-handler attributes, so this is
     * not an XSS fix; what it stops is structural injection that survives sanitizing — an `<img>`
     * pointing at an arbitrary URL, a link, or markup that simply breaks the toolbar's layout.
     */
    readonly $actionExecutionLabel = computed(() => {
        const execution = this.$actionExecution();

        // Several at once: name none of them and report the number instead (FR-017).
        if (!execution) {
            return this.$activeRunCount() > 1
                ? this.#dotMessageService.get(
                      'content-drive.action-center.applying-many',
                      String(this.$activeRunCount())
                  )
                : '';
        }

        // A run says whatever it brought, and nothing otherwise.
        //
        // There used to be an "Applying X to Y" fallback here for runs with no copy of their own.
        // Nothing could reach it: only an *unmarked* run arrives here (`toolbarRun` filters to
        // `targets.length === 0`, because a run whose rows are marked in the grid is already
        // telling the author where it is), and every unmarked run is an upload, which names
        // itself. Its one real effect was on uploads before they had their own words, where it
        // produced "Applying Upload to demo.dotcms.com" -- a sentence for an action performed ON
        // content rather than for files going INTO a place.
        //
        // So a run arriving with no `labelKey` is one nobody has written words for, and inventing
        // some is what caused that. Silence is the honest answer, and the effect above raises
        // nothing for an empty label.
        //
        // The count is the only thing interpolated. It is a number this code produced, so nothing
        // here needs escaping even though the message carries its own `<b>` and is therefore bound
        // with `[innerHTML]`. Anything author-written that is ever added to these messages does.
        return execution.labelKey
            ? this.#dotMessageService.get(execution.labelKey, String(execution.total))
            : '';
    });

    /** What the status toast is currently saying, so an unchanged run is not re-raised. */
    #shownRunLabel: string | undefined;

    /**
     * Mirrors the run in flight into the status toast.
     *
     * The toolbar used to draw this itself, at the end of the filter row. It moved because the row
     * is where the user works — filter chips come and go beside it — and a status that appears and
     * disappears there shifts the controls under the pointer. A toast says the same thing without
     * competing for that space, and gives the in-flight state and its outcome one surface instead
     * of an indicator here and a toast elsewhere.
     *
     * Sticky while the run lasts and cleared when it settles: the outcome toast that follows is
     * raised by the shell, which is where results are turned into copy.
     *
     * The percentage the old indicator could show is deliberately not carried over. Nothing ever
     * sets a run's `processed` — `updateExternalRun` has no callers — so it could not render, and
     * the app's HTTP backend does not report upload progress either.
     */
    readonly runToastSync = effect(() => {
        const running = this.$hasRunInFlight();
        const label = this.$actionExecutionLabel();

        untracked(() => {
            if (!running) {
                this.#shownRunLabel = undefined;
                this.#messageService.clear(STATUS_TOAST_KEY);

                return;
            }

            // A run with nothing to say raises nothing rather than an empty pill.
            if (!label) {
                this.#shownRunLabel = undefined;
                this.#messageService.clear(STATUS_TOAST_KEY);

                return;
            }

            // Only when the wording actually changes. PrimeNG has no update, so re-reporting means
            // clearing and raising again — and the old inline indicator simply changed its text,
            // so re-animating on every store touch would be a behaviour this replaced, not kept.
            // The label does change while runs are in flight: a second run starting collapses it to
            // the count form, and finishing brings the named form back.
            if (label === this.#shownRunLabel) {
                return;
            }

            this.#shownRunLabel = label;
            this.#messageService.clear(STATUS_TOAST_KEY);
            this.#messageService.add({
                key: STATUS_TOAST_KEY,
                severity: 'info',
                summary: label,
                icon: 'pi pi-spin pi-spinner',
                sticky: true,
                // PrimeNG reads this off the message, not the outlet, and defaults to closable.
                // A status is not the reader's to dismiss: it reports work already under way and
                // clears itself when that work settles.
                closable: false
            });
        });
    });

    /**
     * Reports a finished workflow action as a toast, refreshes the grid, and closes the dialog if it
     * is still open.
     *
     * Lives in the shell because the run outlives the Action Center: the user may close it
     * mid-flight and the result still has to be reported, and the shell owns `<p-toast>` for the
     * portlet's whole life. The reload lands here too because `loadItems` belongs to the base store's
     * `withMethods`, which `withActionExecution` cannot reach from inside the composition.
     *
     * A `signalMethod` fed only the result, like {@link #syncDialog}: the site, path and tree
     * selection it reads to word the toast are snapshots, so changing any of them does not re-enter
     * the outcome path.
     */
    readonly #reportActionExecutionResult = signalMethod<
        DotContentDriveActionExecutionResult | undefined
    >((result) => {
        if (!result) {
            return;
        }

        const outcome = this.#readOutcome(result);

        if (outcome.isFolderOutcome) {
            // The listing and the tree load separately, and a tree still offering a folder the
            // listing dropped is how an author navigates into nothing (FR-036). A duplication adds
            // folders, so the tree needs them as much as a delete needs them gone (FR-030). An
            // upload changes contents, not the hierarchy, so it skips this.
            this.#store.loadFolders();
        }

        // Silent on a clean success, because the listing already shows it. A shortfall is not
        // visible anywhere; `confirmSuccess` marks operations whose success shows nowhere (Add to
        // Bundle, Push Publish); a backgrounded outcome arrived after the author moved on. Only the
        // toast is suppressed: the reload and the dialog close below still happen.
        if (outcome.isPartial || result.confirmSuccess || result.backgrounded) {
            this.#announceOutcome(outcome);
        }

        this.#reloadAfterOutcome(result.backgrounded, result.affectedFolders);

        if (!result.backgrounded) {
            // A no-op when the user already closed the dialog. Never for a backgrounded result: it
            // can land while the user is configuring a different action, and closing throws that away.
            this.#store.closeDialog();
        }

        this.#store.clearActionExecutionResult();
    });

    /** Resolves a message key, in the shape the failure describers take. */
    readonly #resolveMessage = (key: string, ...args: string[]): string =>
        this.#dotMessageService.get(key, ...args);

    /**
     * The summary key for a failure group that was not cancelled, by outcome kind and severity.
     * An outcome with no kind is an upload.
     */
    readonly #failureSummaryKeys: Record<
        DotContentDriveOutcomeKind,
        Record<DotUploadFailureGroup['severity'], string>
    > = {
        [OUTCOME_KIND.UPLOAD]: {
            error: 'content-drive.upload.toast.failed',
            warn: 'content-drive.upload.toast.incomplete'
        },
        [OUTCOME_KIND.FOLDER_DELETE]: {
            error: 'content-drive.delete.toast.failed',
            warn: 'content-drive.delete.toast.incomplete'
        },
        [OUTCOME_KIND.FOLDER_DUPLICATE]: {
            error: 'content-drive.duplicate.toast.failed',
            warn: 'content-drive.duplicate.toast.incomplete'
        }
    };

    /**
     * Derives the counts that decide how an outcome is worded.
     *
     * A folder skipped because a selected parent already covered it is not a shortfall: the author
     * got the child once, inside the parent (FR-021a). A recognised resubmission is not one either,
     * whatever its counts say: a retry that worked collides on every file, and calling that a failure
     * sends the author to re-upload files that are already there.
     */
    #readOutcome(result: DotContentDriveActionExecutionResult): DotContentDriveOutcomeReading {
        const isFolderOutcome =
            OUTCOME_KIND.FOLDER_DELETE === result.outcomeKind ||
            OUTCOME_KIND.FOLDER_DUPLICATE === result.outcomeKind;
        const coveredCount = isFolderOutcome
            ? (result.failures ?? []).filter(
                  (item) => 'SKIPPED' === item.status && 'COVERED_BY_PARENT' === item.reason
              ).length
            : 0;
        const isPartial =
            !result.duplicateSubmission &&
            (result.failedCount > 0 || result.skippedCount - coveredCount > 0);

        return { result, isFolderOutcome, coveredCount, isPartial };
    }

    /**
     * Shows the outcome's toasts: one per failure group, or a single counts-only one when there is
     * no per-item detail. The counts ride only on the first, since they belong to the batch.
     */
    #announceOutcome(outcome: DotContentDriveOutcomeReading): void {
        const detail = this.#outcomeDetail(outcome);
        const groups = this.#failureGroups(outcome);
        const messages = groups.length
            ? this.#failureGroupMessages(outcome, groups, detail)
            : [this.#countsOnlyMessage(outcome, detail)];

        messages.forEach((message) => this.#messageService.add(message));
    }

    /**
     * The counts sentence. Anything short of a clean run states success, failed and skipped each
     * next to its own cause, because skips and failures can happen in the same run and naming only
     * one blames it for the whole shortfall.
     */
    #outcomeDetail({ result, coveredCount, isPartial }: DotContentDriveOutcomeReading): string {
        const { actionName, successCount, failedCount, skippedCount } = result;

        // A stopped run says so whatever its counts, or one stopped after only successes would read
        // as clean. Only the folders the stop left out count as skipped: a covered one went with
        // its parent.
        if (result.cancelled) {
            return this.#dotMessageService.get(
                this.#cancelledKeys(result.outcomeKind).detail,
                actionName,
                String(successCount),
                String(failedCount),
                String(skippedCount - coveredCount)
            );
        }

        // A resubmission means opposite things by base type (FR-040b): a file batch collided and
        // nothing was duplicated, a dotAsset batch ran again and every file now exists twice.
        if (result.duplicateSubmission) {
            return this.#dotMessageService.get(
                'DOTASSET' === result.baseType
                    ? 'content-drive.upload.toast.already-uploaded-again'
                    : 'content-drive.upload.toast.already-uploaded',
                String(failedCount + successCount)
            );
        }

        if (isPartial) {
            // Actions whose shortfalls mean something other than permissions, locks and workflow
            // steps bring their own sentence (`partialDetailKey`).
            return this.#dotMessageService.get(
                result.partialDetailKey ?? 'content-drive.action-center.toast.executed-partial',
                actionName,
                String(successCount),
                String(failedCount),
                String(skippedCount)
            );
        }

        return this.#dotMessageService.get(
            'content-drive.action-center.toast.executed-detail',
            actionName,
            String(successCount)
        );
    }

    /**
     * The named items and their reasons, one line per reason. The counts say how many; only this
     * says which and why. Empty for a recognised resubmission, whose "failures" are files already
     * correctly in place.
     */
    #failureGroups(outcome: DotContentDriveOutcomeReading): DotUploadFailureGroup[] {
        const { result } = outcome;

        if (outcome.isFolderOutcome) {
            return this.#folderFailureGroups(outcome);
        }

        if (result.duplicateSubmission) {
            return [];
        }

        return describeUploadFailures(result.failures, this.#resolveMessage, {
            folderFilter: this.#onScreenFolderFilter(result.affectedFolders)
        });
    }

    /**
     * A folder operation's lines as a single group: every line already reads as "this folder
     * survived, here is why", so upload's warn/error split would divide one list. Red only when
     * something failed, and nothing to list when the only entries are covered folders.
     *
     * Each vocabulary has its own describer because the reason sets barely overlap: a delete's
     * `IN_USE` resolved through upload's mapping would fall back to the unclassified copy.
     */
    #folderFailureGroups({
        result,
        isPartial
    }: DotContentDriveOutcomeReading): DotUploadFailureGroup[] {
        const { failures, failedCount, cancelled, outcomeKind } = result;

        if (!failures?.length || !(isPartial || cancelled)) {
            return [];
        }

        const describeLines =
            OUTCOME_KIND.FOLDER_DUPLICATE === outcomeKind
                ? describeFolderDuplicateOutcome
                : describeFolderDeleteOutcome;

        return [
            {
                severity: failedCount > 0 ? 'error' : 'warn',
                lines: describeLines(failures, this.#resolveMessage)
            }
        ];
    }

    /**
     * The on-screen folder's file filter, when the batch targeted exactly that folder.
     *
     * A failure never carries the mask that refused it, so the sentence naming what a folder
     * accepts is only honest for the folder on screen. For any other folder, or a run spanning
     * several, it would explain the wrong folder's rule.
     */
    #onScreenFolderFilter(affectedFolders: string[] | undefined): string | undefined {
        const affectedRefs = (affectedFolders ?? []).map(normalizeFolderRef);
        const refusingFolderIsOnScreen =
            affectedRefs.length === 1 &&
            affectedRefs[0] ===
                browsedFolderRef(this.#store.currentSite()?.hostname, this.#store.path());

        return refusingFolderIsOnScreen ? this.#selectedTreeFolder()?.filesMasks : undefined;
    }

    /**
     * The folder selected in the tree, skipping the load-more row, which has no folder behind it.
     */
    #selectedTreeFolder(): DotFolderTreeNodeContentData | undefined {
        const selectedNodeData = this.#store.selectedNode()?.data;

        return selectedNodeData && selectedNodeData.type !== LOAD_MORE_NODE_TYPE
            ? (selectedNodeData as DotFolderTreeNodeContentData)
            : undefined;
    }

    /**
     * One toast per failure group, so a wall the author cannot pass does not arrive in the same
     * colour as a file that needs renaming. The counts go in the first one only.
     */
    #failureGroupMessages(
        { result }: DotContentDriveOutcomeReading,
        groups: DotUploadFailureGroup[],
        detail: string
    ): ToastMessageOptions[] {
        return groups.map((group, index) => ({
            severity: group.severity,
            summary: this.#dotMessageService.get(
                this.#failureGroupSummaryKey(result, group.severity)
            ),
            detail: [...(index === 0 ? [detail] : []), ...group.lines].join('<br>'),
            life: WARNING_MESSAGE_LIFE
        }));
    }

    /** The summary key for one failure group. */
    #failureGroupSummaryKey(
        result: DotContentDriveActionExecutionResult,
        severity: DotUploadFailureGroup['severity']
    ): string {
        if (result.cancelled) {
            return this.#cancelledKeys(result.outcomeKind).summary;
        }

        return this.#failureSummaryKeys[result.outcomeKind ?? OUTCOME_KIND.UPLOAD][severity];
    }

    /**
     * The single toast for an outcome with no per-item detail: the counts alone are still an outcome.
     */
    #countsOnlyMessage(
        outcome: DotContentDriveOutcomeReading,
        detail: string
    ): ToastMessageOptions {
        const { result, isPartial } = outcome;
        const isShortfall = !!result.cancelled || isPartial || !!result.duplicateSubmission;

        return {
            severity: this.#countsOnlySeverity(outcome),
            summary: this.#dotMessageService.get(this.#countsOnlySummaryKey(outcome)),
            detail,
            life: isShortfall ? WARNING_MESSAGE_LIFE : SUCCESS_MESSAGE_LIFE
        };
    }

    /**
     * The level of a counts-only toast. A skip is a shortfall, so it warns. A resubmission warns
     * only when something is left to do (FR-040b): a dotAsset batch left two of everything, while a
     * file batch was refused its second copy, and `info` says there is nothing to look at.
     */
    #countsOnlySeverity({
        result,
        isPartial
    }: DotContentDriveOutcomeReading): 'success' | 'info' | 'warn' {
        if (result.cancelled) {
            return 'warn';
        }

        if (result.duplicateSubmission) {
            return 'DOTASSET' === result.baseType ? 'warn' : 'info';
        }

        return isPartial ? 'warn' : 'success';
    }

    /** The summary key of a counts-only toast. */
    #countsOnlySummaryKey({ result, isPartial }: DotContentDriveOutcomeReading): string {
        if (result.cancelled) {
            return this.#cancelledKeys(result.outcomeKind).summary;
        }

        return isPartial || result.duplicateSubmission
            ? 'content-drive.upload.toast.incomplete'
            : 'content-drive.action-center.toast.executed';
    }

    /** The summary and detail keys for a stopped run. Anything but a delete reads as a duplication. */
    #cancelledKeys(outcomeKind: DotContentDriveOutcomeKind | undefined): {
        summary: string;
        detail: string;
    } {
        return OUTCOME_KIND.FOLDER_DELETE === outcomeKind
            ? {
                  summary: 'content-drive.delete.toast.cancelled',
                  detail: 'content-drive.delete.toast.cancelled-detail'
              }
            : {
                  summary: 'content-drive.duplicate.toast.cancelled',
                  detail: 'content-drive.duplicate.toast.cancelled-detail'
              };
    }

    /**
     * Reloads the grid after an outcome, or holds the reload while the author is mid-task.
     *
     * A backgrounded outcome arrives unprompted, so it must not disturb what the author is doing.
     * Every other result settles a request they are waiting on, so holding it would read as the
     * action having done nothing.
     */
    #reloadAfterOutcome(backgrounded: boolean | undefined, affectedFolders?: string[]): void {
        if (backgrounded && this.$authorIsMidTask()) {
            this.#holdReload(affectedFolders);

            return;
        }

        // Quiet: the run already marked its rows, so a skeleton here would read as a jump.
        // `loadItems` also drops the selection the run consumed.
        if (this.#currentFolderIsAffected(affectedFolders)) {
            this.#store.loadItems({ quiet: true });
        }
    }

    /**
     * Holds a reload for {@link flushHeldReloadEffect} to run at the next boundary (FR-043).
     * Dropping it would leave the grid stale for as long as the author stayed.
     *
     * Merged into what is already held, so a later outcome for another folder does not drop this
     * one's reload. No folders named means reload regardless, and that wins.
     */
    #holdReload(affectedFolders?: string[]): void {
        const held = this.#reloadHeld();
        const reloadRegardless =
            !affectedFolders?.length || (!!held && !held.affectedFolders?.length);

        this.#reloadHeld.set({
            affectedFolders: reloadRegardless
                ? undefined
                : [...new Set([...(held?.affectedFolders ?? []), ...affectedFolders])]
        });
    }

    /**
     * The words for each refusal the delete endpoint reasoned about.
     *
     * Here rather than in the store for the same reason {@link #describeSubmissionRefusal} is: the
     * store carries the kind, the component decides what an author reads. `UNCLASSIFIED` is a
     * transport failure or a body with no code, said in the product's words rather than the
     * server's (FR-024).
     *
     * None of them names a number or a folder. The ceiling is in the server's prose, which is not
     * localised and so is not rendered, and the overlap body carries no structured field naming the
     * folder it collided on — a shortfall against FR-040 recorded in the contract rather than
     * papered over by parsing a sentence.
     */
    readonly #folderDeleteRefusalKeys: Record<DotFolderBulkDeleteRefusalKind, string> = {
        EMPTY_SELECTION: 'content-drive.delete.refused.empty-selection',
        OVER_MAX_PATHS: 'content-drive.delete.refused.over-max-paths',
        NOT_ENTITLED: 'content-drive.delete.refused.not-entitled',
        OVERLAPPING_RUN: 'content-drive.delete.refused.overlapping-run',
        UNCLASSIFIED: 'content-drive.delete.refused.unclassified'
    };

    /**
     * Says why a bulk folder delete never became a run, and consumes the refusal.
     *
     * Its own effect rather than a branch of the outcome drain: a refusal is not an outcome. Nothing
     * ran, so there are no counts to report, nothing to reload, and no dialog state to settle — the
     * only thing owed to the author is the sentence (FR-041).
     */
    readonly #reportFolderDeleteRefusal = signalMethod<DotFolderBulkDeleteRefusalKind | undefined>(
        (kind) => {
            if (!kind) {
                return;
            }

            this.#messageService.add({
                severity: 'error',
                summary: this.#dotMessageService.get('content-drive.delete.refused.title'),
                detail: this.#refusalDetail(
                    kind,
                    this.#folderDeleteRefusalKeys[kind],
                    'content-drive.delete.refused.over-max-paths-limit',
                    this.#store.folderDeleteMaxPaths()
                ),
                life: ERROR_MESSAGE_LIFE
            });

            this.#store.clearFolderDeleteRefusal();
        }
    );

    /**
     * The sentence for each way the server can refuse a folder duplication before any run exists
     * (#37062). Delete's kinds minus the overlap one, which duplication cannot produce.
     * `UNCLASSIFIED` is a transport failure or a body with no code, said in the product's words.
     */
    readonly #folderDuplicateRefusalKeys: Record<DotFolderBulkDuplicateRefusalKind, string> = {
        EMPTY_SELECTION: 'content-drive.duplicate.refused.empty-selection',
        OVER_MAX_PATHS: 'content-drive.duplicate.refused.over-max-paths',
        NOT_ENTITLED: 'content-drive.duplicate.refused.not-entitled',
        UNCLASSIFIED: 'content-drive.duplicate.refused.unclassified'
    };

    /**
     * The sentence for a refusal, naming the ceiling when it was one and the server advertises it.
     *
     * The Action Center already sends at most the advertised ceiling, so this is reached only when
     * the limit was not known to the client, or changed under it. Naming the number then still tells the
     * author how far to narrow the selection, which "fewer" does not.
     *
     * @param kind the refusal
     * @param key the refusal's own sentence
     * @param limitKey the sentence naming the ceiling, for `OVER_MAX_PATHS`
     * @param maxPaths the advertised ceiling, or `null` when none is
     */
    #refusalDetail(
        kind: DotFolderBulkDeleteRefusalKind | DotFolderBulkDuplicateRefusalKind,
        key: string,
        limitKey: string,
        maxPaths: number | null
    ): string {
        return kind === 'OVER_MAX_PATHS' && maxPaths !== null
            ? this.#dotMessageService.get(limitKey, String(maxPaths))
            : this.#dotMessageService.get(key);
    }

    /**
     * Says why a folder duplication never became a run, and consumes the refusal. Mirrors
     * {@link #reportFolderDeleteRefusal}: a refusal is not an outcome, so there are no counts and
     * nothing to reload.
     */
    readonly #reportFolderDuplicateRefusal = signalMethod<
        DotFolderBulkDuplicateRefusalKind | undefined
    >((kind) => {
        if (!kind) {
            return;
        }

        this.#messageService.add({
            severity: 'error',
            summary: this.#dotMessageService.get('content-drive.duplicate.refused.title'),
            detail: this.#refusalDetail(
                kind,
                this.#folderDuplicateRefusalKeys[kind],
                'content-drive.duplicate.refused.over-max-paths-limit',
                this.#store.folderDuplicateMaxPaths()
            ),
            life: ERROR_MESSAGE_LIFE
        });

        this.#store.clearFolderDuplicateRefusal();
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

        // Reflect the open panel, in either editor: `editContent` + `editContentLang` for an edit
        // (the language, so the link reopens the very version that is open), `createContent` for a
        // create (#37759, FR-020). Every one is `null` (removed) unless it describes the open panel,
        // so none lingers after a close or a switch. Written via Location.go/replaceState so it
        // triggers no navigation/reload.
        const panelLocation = this.#navigationService.$panelLocation();
        const isEdit = panelLocation?.kind === 'edit';
        queryParams[CONTENT_DRIVE_URL_PARAM.EDIT_CONTENT] = isEdit
            ? panelLocation.editContent
            : null;
        queryParams[CONTENT_DRIVE_URL_PARAM.EDIT_CONTENT_LANG] =
            isEdit && panelLocation.editContentLang ? String(panelLocation.editContentLang) : null;
        queryParams[CONTENT_DRIVE_URL_PARAM.CREATE_CONTENT] =
            panelLocation?.kind === 'create' ? panelLocation.createContent : null;
        const panelOpen = panelLocation !== null;

        // The open folder dialog, the same way (#37759, FR-030).
        const folderParam = folderDialogParamOf(this.#store.dialog());
        for (const key of [
            CONTENT_DRIVE_URL_PARAM.CREATE_FOLDER,
            CONTENT_DRIVE_URL_PARAM.EDIT_FOLDER,
            CONTENT_DRIVE_URL_PARAM.FOLDER_PERMISSIONS
        ]) {
            queryParams[key] = folderParam?.key === key ? folderParam.value : null;
        }
        const folderDialogOpen = folderParam !== null;

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
            const isOpeningPanel =
                (panelOpen && !this.#editPanelUrlWasSet) ||
                (folderDialogOpen && !this.#folderDialogUrlWasSet);
            const isFolderNavigation =
                this.#lastWrittenPath !== undefined && path !== this.#lastWrittenPath;

            if (isOpeningPanel || isFolderNavigation) {
                this.#location.go(newUrl);
            } else {
                this.#location.replaceState(newUrl);
            }
            this.#lastWrittenPath = path;
        }
        this.#editPanelUrlWasSet = panelOpen;
        this.#folderDialogUrlWasSet = folderDialogOpen;
    });

    /**
     * Opens Edit Permissions for the folder the store dialog names, and clears the store dialog
     * when it closes, so the URL follows (#37759, FR-030).
     *
     * @param dialog The `FOLDER_PERMISSIONS` dialog the store holds.
     */
    #openPermissionsDialog(dialog: DotContentDriveDialog): void {
        if (this.#permissionsDialogRef) {
            return;
        }

        const { identifier } = dialog.payload as DotContentDriveFolderPermissionsPayload;
        const ref = this.#dialogService.open(
            DotJspIframeDialogComponent,
            folderPermissionsDialogConfig(identifier, dialog.header)
        );

        this.#permissionsDialogRef = ref;
        ref?.onClose.pipe(take(1)).subscribe(() => {
            if (this.#permissionsDialogRef === ref) {
                this.#permissionsDialogRef = null;
                this.#store.closeDialog();
            }
        });
    }

    /** Closes the open folder dialog the way its own close does. */
    #closeFolderDialog(): void {
        if (this.#permissionsDialogRef) {
            // Its `onClose` clears the store dialog.
            this.#permissionsDialogRef.close();

            return;
        }

        this.#store.closeDialog();
    }

    /**
     * Opens Folder Settings for a folder an `editFolder` link names (#37759, FR-031). The link only
     * carries the identifier, so the folder and the user's access to it are resolved first, and the
     * same rule as the context menu applies: the user must be able to edit the folder (FR-033).
     *
     * @param identifier The folder's identifier.
     */
    #openFolderSettingsFromUrl(identifier: string): void {
        this.#openFolderDialogFromUrl(
            identifier,
            (access) => access.canEdit,
            (folder, access) => ({
                type: DIALOG_TYPE.FOLDER,
                header: this.#dotMessageService.get('content-drive.dialog.folder.header.edit'),
                payload: toActionableFolder(folder, access)
            })
        );
    }

    /**
     * Opens Edit Permissions for a folder a `folderPermissions` link names (#37759, FR-031), when
     * the user may edit that folder's permissions, the same rule as the context menu (FR-033).
     *
     * @param identifier The folder's identifier.
     */
    #openFolderPermissionsFromUrl(identifier: string): void {
        this.#openFolderDialogFromUrl(
            identifier,
            (access) => access.canEditPermissions,
            () => ({
                type: DIALOG_TYPE.FOLDER_PERMISSIONS,
                header: this.#dotMessageService.get('Edit-Permissions'),
                payload: { identifier }
            })
        );
    }

    /**
     * Opens the dialog a folder link names, under the same rules for every folder dialog: the folder
     * and the user's access to it are resolved first, a folder in another site is refused as not
     * found, and a user without the access the dialog needs gets the standard permission error.
     *
     * @param identifier The folder's identifier.
     * @param allowed Whether the user's access lets them open this dialog.
     * @param dialogFor The dialog to open for the resolved folder.
     */
    #openFolderDialogFromUrl(
        identifier: string,
        allowed: (access: DotFolderUserAccess) => boolean,
        dialogFor: (folder: DotFolderBean, access: DotFolderUserAccess) => DotContentDriveDialog
    ): void {
        forkJoin({
            folder: this.#folderService.getFolderById(identifier),
            access: this.#permissionsService.getUserAccess(identifier),
            site: this.#currentSite$()
        })
            .pipe(take(1))
            .subscribe({
                next: ({ folder, access, site }) => {
                    if (this.#refuseIfInAnotherSite(folder, site)) {
                        return;
                    }

                    if (!allowed(access)) {
                        this.#reportForbidden();

                        return;
                    }

                    this.#store.setDialog(dialogFor(folder, access));
                },
                error: (error: HttpErrorResponse) => this.#httpErrorManager.handle(error)
            });
    }

    /**
     * A folder link names a folder in another site, which is out of reach from this one: its dialog
     * would act on it under the wrong site (#37759, edge case "folder … moved to another site").
     * Reported as not found, as for a folder that is gone.
     *
     * @param folder The folder the link resolved to.
     * @param site The admin's current site, from {@link #currentSite$}.
     * @returns Whether the folder was refused, and the error shown.
     */
    #refuseIfInAnotherSite(folder: DotFolderBean, site: DotSite): boolean {
        if (folder.hostId === site.identifier) {
            return false;
        }

        this.#httpErrorManager.handle(new HttpErrorResponse({ status: 404 }));

        return true;
    }

    /**
     * The admin's current site, waiting for it if it hasn't loaded yet. A folder link is read in
     * the constructor, and on a cold load the folder lookups can answer before the site does.
     * Until then the store holds `SYSTEM_HOST`, which no real folder matches, so comparing against
     * it would refuse a folder in the author's own site.
     *
     * @returns The current site, emitted once.
     */
    #currentSite$(): Observable<DotSite> {
        const site = this.#store.currentSite();

        return isLoadedSite(site) ? of(site) : this.#siteLoaded$;
    }

    /**
     * The user may not open what the link names. The standard permission message, the same one a
     * refused request shows, rather than a message of this portlet's own.
     */
    #reportForbidden(): void {
        this.#httpErrorManager.handle(new HttpErrorResponse({ status: 403 }));
    }

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

        // The tree tells its site row apart from a folder by giving it an empty path. As a
        // *location* that means the site root, `/`, which is a different thing from all site
        // content — and all site content is what an absent location means. Translating here keeps
        // the tree's own representation untouched while stopping the two collapsing into one.
        const location = data.path === '' ? ROOT_PATH : data.path;

        if (location != currentPath) {
            this.#store.setPath(location);
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
    protected onDoubleClick(contentlet: DotContentDriveBrowseItem) {
        // Same narrowing as the selection, for the same reason: a link has no editor to open.
        if (!isActionableBrowseItem(contentlet)) {
            return;
        }

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

    /**
     * A save in the new-editor side panel can create or change an item, so refresh the list. The
     * first save of a create also switches the URL to the saved content (#37759, FR-025).
     *
     * @param contentlet The saved content.
     */
    protected onEditPanelSaved(contentlet: DotCMSContentlet) {
        this.#store.reloadContentDrive();
        this.#navigationService.panelSaved({
            identifier: contentlet.identifier,
            languageId: contentlet.languageId
        });
    }

    /**
     * A save in the legacy panel, including a workflow action: refresh the list quietly and keep
     * the panel open (#37759, FR-013). Remove with the legacy editor.
     */
    protected onLegacyPanelSaved({ identifier, languageId }: DotLegacyEditorSaved) {
        this.#store.reloadContentDrive({ quiet: true });
        // The first save of a create switches the URL to the saved content (FR-025).
        this.#navigationService.panelSaved({ identifier, languageId });
    }

    /**
     * The author switched language inside the legacy editor: the URL follows (#37759, FR-020).
     * Remove with the legacy editor.
     *
     * @param languageId The language the editor now shows.
     */
    protected onLegacyPanelLanguage(languageId: number) {
        this.#navigationService.panelLanguageChanged(languageId);
    }

    /**
     * The author switched language inside the new-editor panel, which reloads in place. The URL
     * follows, as for the legacy panel, so a refresh or a switch to the old editor (a page reload)
     * reopens that language (#37759, FR-020, FR-028).
     *
     * @param languageId The language the editor now shows.
     */
    protected onEditPanelLanguage(languageId: number) {
        this.#navigationService.panelLanguageChanged(languageId);
    }

    /**
     * The legacy panel closed, for any reason. Refresh the list quietly every time: a close after a
     * delete looks exactly like a cancel, and a move or a language switch sends no event at all, so
     * one extra list request is cheaper than missing a change (#37759, FR-014, FR-015).
     * Remove with the legacy editor.
     */
    protected onLegacyPanelClosed() {
        this.#navigationService.closeEditPanel();
        this.#store.reloadContentDrive({ quiet: true });
    }

    /**
     * Bring Back restored an older version in the legacy panel. The editor reloads with it and
     * sends no save, so refresh the list quietly here (#37759, US6). Remove with the legacy editor.
     */
    protected onLegacyPanelRestored() {
        this.#store.reloadContentDrive({ quiet: true });
    }

    /**
     * The first save of a new page in the legacy panel: close it, refresh the list, and open the
     * page in the page editor in the language the editor named, as Content Search does (#37759,
     * FR-008). Remove with the legacy editor.
     *
     * @param request The page to open and its language.
     */
    protected onLegacyPanelPageEditor({ url, languageId }: DotLegacyEditorPageRequest) {
        this.#navigationService.closeEditPanel();
        this.#store.reloadContentDrive({ quiet: true });
        this.#dotRouterService.goToEditPage({ url, language_id: languageId });
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
        // No target means no folder is selected, which is every scope that is not a folder: all
        // site content and System Host. `canAddChildrenTo` answers `true` for an absent target
        // because it has nothing to judge, so asking it there would wave the drop through on the
        // site's answer while the Upload button beside it is correctly disabled. The store's gate
        // already knows which scope is open and whose permission applies.
        const allowed = targetFolder
            ? canAddChildrenTo(targetFolder, this.#store.siteCanAddChildren())
            : this.#store.$canAddChildren();

        if (allowed) {
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

    /**
     * Drag-and-drop / sidebar flow: the files are already known. When the target folder pins a base
     * type, upload the files directly; otherwise open the type menu (anchored to the content area)
     * and carry the files into the payload to upload right after the user picks.
     */
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
            // Its own wording rather than the workflow sentence. Without this the run reads
            // "Applying Upload to demo.dotcms.com" — a phrasing for an action applied TO content,
            // which is not what putting files INTO a place is.
            //
            // The caller picks singular or plural because it is the only place that knows how
            // many files were chosen. The messages spell the noun out rather than hedging with
            // "file(s)", which is what the count is for.
            labelKey: uploadIndicatorKey(files.length),
            total: files.length,
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
                //
                // The last fallback is the *browsed* host, not the site in the switcher: with
                // System Host selected the switcher still shows a site, and that site is context
                // rather than the destination.
                ...(hostFolder?.id && hostFolder.path
                    ? { folderId: hostFolder.id }
                    : {
                          siteId: hostFolder?.id ?? this.#store.$newContentHostId() ?? ''
                      })
            })
            .subscribe({
                next: (event) => {
                    if (event.kind === 'progress') {
                        // Ignored, deliberately. The app runs on Angular's fetch backend, which
                        // never emits upload progress, and the XHR backend that would is
                        // deprecated — so this never arrives here. The indicator reports the upload
                        // as activity without a position, which is the honest rendering of a wait
                        // whose denominator the browser will not give us (FR-041a).
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
                    // The server's own count, which is the one to display: it equals the `total`
                    // the outcome later reports, so the indicator and the message that ends it
                    // describe the same batch. The author's count is the fallback for an instance
                    // older than the field, and is right whenever no parts were lost in transit.
                    const submitted = event.handle.submitted ?? files.length;

                    const backgroundRunId = this.#store.startExternalRun({
                        operation: `${UPLOAD_BATCH_OPERATION}:${event.handle.jobId}`,
                        labelKey: uploadIndicatorKey(submitted, { backgrounded: true }),
                        total: submitted,
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
                            hostFolder?.hostname
                                ? toFolderRef(
                                      hostFolder.hostname,
                                      // An empty path is the site root, which normalises to
                                      // `//hostname` — the ref the listing computes when browsing it.
                                      hostFolder.path || ROOT_PATH
                                  )
                                : // No folder chosen means the batch lands wherever the sidebar is
                                  // pointing, which is exactly what the browsed reference describes.
                                  // Rebuilding it from the switcher's site instead named the site
                                  // root while the files were going to System Host, so the run and
                                  // the listing disagreed about where they had landed and the grid
                                  // was never refreshed.
                                  browsedFolderRef(
                                      this.#store.currentSite()?.hostname,
                                      this.#store.path()
                                  )
                        ],
                        backgroundRunId,
                        // Carried to the outcome because a resubmission means opposite things by
                        // base type, and by the time the completion lands nothing else knows which
                        // one ran (FR-040b).
                        baseType
                    );

                    // No notification here any more. This used to raise one, because the
                    // indicator could say work was happening but not that the page guard had
                    // released the author. The status toast that replaced the indicator says
                    // "in the background" itself, and having both on screen meant a backgrounded
                    // upload announced itself twice, once wide and once compact.
                    //
                    // What is genuinely lost is the sentence spelling out that the author may
                    // leave the page. The wording carries the fact; it no longer argues for it.

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

        const { pathToMove, dragItems } = this.getMoveMetadata(event);

        const dragItemsInodes = dragItems.contentlets.map((item) => item.inode);
        const assetContentletsCount = dragItems.contentlets.length;

        // Reports on the toolbar indicator, not as a notification announcing a start (FR-007,
        // FR-008). The two "moving …" toasts this replaces said only that something had begun,
        // which the indicator says better and without stacking up over the outcome that follows.
        const runId = this.#store.startExternalRun({
            operation: MOVE_TO_FOLDER_WORKFLOW_ACTION_ID,
            total: assetContentletsCount,
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

    protected onSelectItems(items: DotContentDriveBrowseItem[]) {
        // Takes what the table emits, which includes menu links so the shared listing can serve the
        // Asset Picker, and narrows here. Content Drive never asks for links, but a handler typed to
        // the narrower union was asserting that rather than checking it — and the selection feeds
        // every action, none of which a link can be the subject of.
        this.#store.setSelectedItems(items.filter(isActionableBrowseItem));
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
