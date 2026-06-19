import { signalMethod } from '@ngrx/signals';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    OnInit,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { Router } from '@angular/router';

import { MessageService, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';

import { catchError } from 'rxjs/operators';

import {
    DotFolderService,
    DotUploadFileService,
    DotLocalstorageService,
    DotWorkflowsActionsService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    ContextMenuData,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotContentDrivePaginateEvent
} from '@dotcms/dotcms-models';
import {
    DotFolderListViewComponent,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeData,
    DotContentDriveMoveItems
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
    HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY,
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
        DotContentDriveDialogFolderComponent,
        DotContentDriveDialogContentTypeSelectorComponent,
        DotContentDriveDialogUploadSelectorComponent,
        MessageModule,
        ButtonModule,
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
export class DotContentDriveShellComponent implements OnInit {
    readonly #store = inject(DotContentDriveStore);

    readonly #router = inject(Router);

    readonly #location = inject(Location);
    readonly #navigationService = inject(DotContentDriveNavigationService);

    readonly #dotMessageService = inject(DotMessageService);
    readonly #messageService = inject(MessageService);
    readonly #fileService = inject(DotUploadFileService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #localStorageService = inject(DotLocalstorageService);

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

    /** Payload (target folder + optional dropped files) for the upload-type selector dialog. */
    readonly $uploadSelectorPayload = computed<DotContentDriveUploadSelectorPayload | undefined>(
        () => {
            const dialog = this.$activeDialog();

            return dialog?.type === DIALOG_TYPE.UPLOAD_SELECTOR
                ? (dialog.payload as DotContentDriveUploadSelectorPayload)
                : undefined;
        }
    );

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
                return 'w-[min(92vw,38rem)] px-0! pt-0 pb-4';
            case DIALOG_TYPE.UPLOAD_SELECTOR:
                return 'w-[min(92vw,31.25rem)] pt-0 p-4';
            default:
                return 'w-[43.75rem] pt-0 p-4';
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
    readonly $showMessage = signal(false);

    readonly $fileInput = viewChild<ElementRef>('fileInput');

    readonly $totalItems = computed(() => {
        const pagination = untracked(() => this.#store.pagination());
        const currentPage = pagination.page; // 1-indexed
        const limit = pagination.limit;
        const page = this.#store.pages().at(-1);

        const items = untracked(() => this.#store.items());

        // The API uses cursor-based pagination and does not return a total count.
        // When hasMoreContent is true, we return one page beyond current so PrimeNG enables the next-page button.
        // When hasMoreContent is false, we can calculate the exact total.
        return page.hasMoreContent
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
        const selectedNode = this.#store.selectedNode();

        if (selectedNode) {
            // Read current path without tracking it to avoid circular dependencies
            const currentPath = untracked(() => this.#store.path()) ?? '';

            if (selectedNode.data.path != currentPath) {
                this.#store.setPath(selectedNode.data.path);
            }
        }
    });

    ngOnInit() {
        this.$showMessage.set(
            !this.#localStorageService.getItem(HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY)
        );
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
     * Closes the message
     *
     * @protected
     * @memberof DotContentDriveShellComponent
     */
    protected onCloseMessage() {
        this.$showMessage.set(false);

        this.#localStorageService.setItem(HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY, true);
    }

    /**
     * Upload-button flow: prompt for the asset type first; the OS file picker opens later, once the
     * user confirms a type in {@link onUploadTypeSelected}.
     */
    protected onUpload() {
        this.openUploadSelector({ targetFolder: this.#store.selectedNode()?.data });
    }

    /**
     * Drag-and-drop / sidebar flow: the files are already known, so prompt for the asset type and
     * carry the files into the dialog payload to upload right after the user confirms.
     */
    protected onRequestUpload({ files, targetFolder }: DotContentDriveUploadFiles) {
        this.openUploadSelector({ targetFolder, files });
    }

    /**
     * Opens the upload-type selector dialog (Asset vs File).
     */
    protected openUploadSelector(payload: DotContentDriveUploadSelectorPayload) {
        this.#store.setDialog({
            type: DIALOG_TYPE.UPLOAD_SELECTOR,
            header: this.#dotMessageService.get('content-drive.dialog.upload-selector.header'),
            payload
        });
    }

    /**
     * Handles the asset-type choice emitted by the upload selector dialog.
     * - Drag-and-drop: the files are already in the selection, so upload immediately.
     * - Upload button: stash the selection and open the OS file picker; {@link onFileChange}
     *   completes the upload once files are chosen.
     */
    protected onUploadTypeSelected(selection: DotContentDriveUploadSelection) {
        this.#store.closeDialog();

        if (selection.files?.length) {
            this.resolveFilesUpload(selection);

            return;
        }

        this.$activeSelection.set(selection);
        this.$fileInput().nativeElement.click();
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

        // Always reset so a cancelled/re-opened picker can't reuse a stale selection.
        this.$activeSelection.set(undefined);
        input.value = '';

        if (!files || files.length === 0 || !selection) {
            return;
        }

        this.resolveFilesUpload({ ...selection, files });
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
        contentType
    }: DotContentDriveUploadSelection) {
        if (!files?.length) {
            return;
        }

        if (files.length > 1) {
            this.uploadFiles({ files, targetFolder, contentType });

            return;
        }

        this.uploadFile({ files, targetFolder, contentType });
    }

    /**
     * Shows a warning message when multiple files are uploaded
     *
     * @protected
     * @param {DotContentDriveUploadSelection} selection
     * @memberof DotContentDriveShellComponent
     */
    protected uploadFiles({ files, targetFolder, contentType }: DotContentDriveUploadSelection) {
        this.#messageService.add({
            severity: 'warn',
            summary: this.#dotMessageService.get('content-drive.work-in-progress'),
            detail: this.#dotMessageService.get('content-drive.multiple-files-warning'),
            life: WARNING_MESSAGE_LIFE
        });

        this.uploadFile({ files, targetFolder, contentType });
    }

    /**
     * Uploads a file to the content drive
     * @param selection The chosen content type, target folder and files to upload
     */
    protected uploadFile({ files, targetFolder, contentType }: DotContentDriveUploadSelection) {
        if (!files?.length) {
            return;
        }

        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('content-drive.file-upload-in-progress'),
            detail: this.#dotMessageService.get('content-drive.file-upload-in-progress-detail')
        });

        this.uploadDotAsset(files[0], targetFolder, contentType);
    }

    /**
     * Uploads a file to the content drive as the given content type (`dotAsset` or `FileAsset`).
     *
     * @protected
     * @param {File} file
     * @param {DotFolderTreeNodeData} [hostFolder]
     * @param {string} [contentType]
     * @memberof DotContentDriveShellComponent
     */
    protected uploadDotAsset(file: File, hostFolder?: DotFolderTreeNodeData, contentType?: string) {
        this.#fileService
            .uploadDotAsset(
                file,
                {
                    hostFolder: hostFolder?.id ?? '',
                    indexPolicy: 'WAIT_FOR'
                },
                contentType
            )
            .subscribe({
                next: ({ title, contentType }) => {
                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-success'),
                        detail: this.#dotMessageService.get(
                            'content-drive.add-dotasset-success-detail',
                            title,
                            contentType
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
