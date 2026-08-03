import { signalMethod } from '@ngrx/signals';
import { of } from 'rxjs';

import { Location, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { Router } from '@angular/router';

import { MessageService, SortEvent } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { Popover, PopoverModule } from 'primeng/popover';
import { ToastModule } from 'primeng/toast';

import { catchError } from 'rxjs/operators';

import {
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
    DotContentDriveFolder,
    DotContentDriveItem,
    DotContentDrivePaginateEvent
} from '@dotcms/dotcms-models';
import {
    DotFolderListViewComponent,
    DOT_FOLDER_LIST_VIEW_COLUMN_TYPE,
    DotContentDriveUploadFiles,
    DotFolderListViewColumn,
    DotFolderTreeNodeData,
    DotFolderTreeNodeContentData,
    DotContentDriveMoveItems,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { DotAddToBundleComponent, DotMessagePipe, DotSeverityIconComponent } from '@dotcms/ui';

import { DotContentDriveDialogContentTypeSelectorComponent } from '../components/dialogs/dot-content-drive-dialog-content-type-selector/dot-content-drive-dialog-content-type-selector.component';
import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
import { DotContentDriveDialogUploadSelectorComponent } from '../components/dialogs/dot-content-drive-dialog-upload-selector/dot-content-drive-dialog-upload-selector.component';
import { DotContentDriveDropzoneComponent } from '../components/dot-content-drive-dropzone/dot-content-drive-dropzone.component';
import { DotContentDriveSidebarComponent } from '../components/dot-content-drive-sidebar/dot-content-drive-sidebar.component';
import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import {
    DIALOG_TYPE,
    SORT_ORDER,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE,
    ERROR_MESSAGE_LIFE,
    MOVE_TO_FOLDER_WORKFLOW_ACTION_ID
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
import { encodeFilters, isFolder } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [
        DotFolderListViewComponent,
        DotContentDriveToolbarComponent,
        DotFolderListViewContextMenuComponent,
        DotAddToBundleComponent,
        DotContentDriveSidebarComponent,
        ToastModule,
        DialogModule,
        PopoverModule,
        NgTemplateOutlet,
        DotContentDriveDialogFolderComponent,
        DotContentDriveDialogContentTypeSelectorComponent,
        DotContentDriveDialogUploadSelectorComponent,
        MessageModule,
        DotMessagePipe,
        DotContentDriveDropzoneComponent,
        DotSeverityIconComponent
    ],
    providers: [DotContentDriveStore, DotWorkflowsActionsService, MessageService, DotFolderService],
    templateUrl: './dot-content-drive-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'grid relative h-full grid-cols-[min-content_1fr_min-content] grid-rows-[min-content_min-content_1fr]'
    }
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #router = inject(Router);

    readonly #location = inject(Location);
    readonly #navigationService = inject(DotContentDriveNavigationService);

    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #fileService = inject(DotUploadFileService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    readonly $items = this.#store.items;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;

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

    /** Folder payload for the folder dialog (narrowed from the dialog payload union by type). */
    readonly $folderPayload = computed(() => {
        const dialog = this.$activeDialog();

        return dialog?.type === DIALOG_TYPE.FOLDER
            ? (dialog.payload as DotContentDriveFolder)
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
            default:
                return 'w-175 pt-0 p-4';
        }
    });

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

    readonly updateQueryParamsEffect = effect(() => {
        const isTreeExpanded = this.#store.isTreeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        const queryParams: Record<string, string> = {};

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

        const urlTree = this.#router.createUrlTree([], {
            queryParams,
            queryParamsHandling: 'merge'
        });
        this.#location.go(urlTree.toString());
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
        if (sidebarLoading || !selectedNode) {
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

    /**
     * Upload-button flow. When the current folder pins a base type (`defaultBaseType`), skip the
     * menu and open the OS file picker straight away; otherwise open the type menu anchored to the
     * button and defer the picker until the user picks a type in {@link onUploadTypeSelected}.
     */
    protected onUpload(event: MouseEvent) {
        const targetFolder = this.#store.selectedNode()?.data;
        const contentData = (targetFolder && targetFolder.type !== LOAD_MORE_NODE_TYPE)
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
    protected onRequestUpload({ files, targetFolder }: DotContentDriveUploadFiles) {
        const contentData = (targetFolder.type !== LOAD_MORE_NODE_TYPE)
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

        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('content-drive.file-upload-in-progress'),
            detail: this.#dotMessageService.get('content-drive.file-upload-in-progress-detail')
        });

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
        const { folderName, pathToMove, dragItems } = this.getMoveMetadata(event);

        const dragItemsInodes = dragItems.contentlets.map((item) => item.inode);
        const assetContentletsCount = dragItems.contentlets.length;

        if (dragItems.folders.length > 0) {
            this.#messageService.add({
                severity: 'info',
                summary: this.#dotMessageService.get(
                    'content-drive.move-to-folder-in-progress-with-folders'
                ),
                detail: this.#dotMessageService.get(
                    'content-drive.move-to-folder-in-progress-detail-with-folders',
                    assetContentletsCount.toString(),
                    `${assetContentletsCount > 1 ? 's ' : ' '}`
                )
            });
        } else {
            this.#messageService.add({
                severity: 'info',
                summary: this.#dotMessageService.get(
                    'content-drive.move-to-folder-in-progress',
                    folderName
                ),
                detail: this.#dotMessageService.get(
                    'content-drive.move-to-folder-in-progress-detail',
                    assetContentletsCount.toString(),
                    `${assetContentletsCount > 1 ? 's ' : ' '}`
                )
            });
        }
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

                    const title = item?.title ?? inode;

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

        const folderName = cleanPath?.length > 0 ? cleanPath : pathToMove;

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
}
