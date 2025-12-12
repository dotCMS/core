import { of } from 'rxjs';

import { Location } from '@angular/common';
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

import { LazyLoadEvent, MessageService, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessagesModule } from 'primeng/messages';
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
import { ContextMenuData, DotContentDriveItem } from '@dotcms/dotcms-models';
import {
    DotFolderListViewComponent,
    DotContentDriveUploadFiles,
    DotFolderTreeNodeData,
    DotContentDriveMoveItems
} from '@dotcms/portlets/content-drive/ui';
import { DotAddToBundleComponent, DotMessagePipe, DotSeverityIconComponent } from '@dotcms/ui';

import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
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
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
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
        MessagesModule,
        ButtonModule,
        DotMessagePipe,
        DotContentDriveDropzoneComponent,
        DotSeverityIconComponent
    ],
    providers: [DotContentDriveStore, DotWorkflowsActionsService, MessageService, DotFolderService],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
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
    readonly #localStorageService = inject(DotLocalstorageService);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;

    readonly $contextMenuData = this.#store.contextMenu;

    readonly $dialog = this.#store.dialog;

    readonly DIALOG_TYPE = DIALOG_TYPE;

    readonly $offset = computed(() => this.#store.pagination().offset, {
        equal: (a, b) => a === b
    });

    readonly $loading = computed(() => this.#store.status() === DotContentDriveStatus.LOADING);
    readonly $showMessage = signal(false);

    readonly $fileInput = viewChild<ElementRef>('fileInput');

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
            const currentPath = untracked(() => this.#store.path());

            if (selectedNode.data.path !== currentPath) {
                this.#store.setPath(selectedNode.data.path);
            }
        }
    });

    ngOnInit() {
        this.$showMessage.set(
            !this.#localStorageService.getItem(HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY)
        );
    }

    protected onPaginate(event: LazyLoadEvent) {
        // Explicit check because it can potentially be 0
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.#store.setPagination({
            limit: event.rows,
            offset: event.first
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
     * Handles dialog hide event to reset the dialog state
     */
    protected onHideDialog() {
        this.#store.closeDialog();
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

    protected onAddNewDotAsset() {
        this.$fileInput().nativeElement.click();
    }

    /**
     * Handles file change event
     * @param event The event that triggered the file change
     */
    protected onFileChange(event: Event) {
        const input = event.target as HTMLInputElement;

        const files = input.files;

        if (!files || files.length === 0) {
            return;
        }

        const targetFolder = this.#store.selectedNode()?.data;

        this.resolveFilesUpload({ files, targetFolder });
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
     * @param files The files to upload
     */
    protected resolveFilesUpload({ files, targetFolder }: DotContentDriveUploadFiles) {
        if (files.length > 1) {
            this.uploadFiles({ files, targetFolder });

            return;
        }

        this.uploadFile({ files, targetFolder });
    }

    /**
     * Shows a warning message when multiple files are uploaded
     *
     * @protected
     * @param {FileList} files
     * @memberof DotContentDriveShellComponent
     */
    protected uploadFiles({ files, targetFolder }: DotContentDriveUploadFiles) {
        this.#messageService.add({
            severity: 'warn',
            summary: this.#dotMessageService.get('content-drive.work-in-progress'),
            detail: this.#dotMessageService.get('content-drive.multiple-files-warning'),
            life: WARNING_MESSAGE_LIFE
        });

        this.uploadFile({ files, targetFolder });
    }

    /**
     * Uploads a file to the content drive
     * @param file The file to upload
     */
    protected uploadFile({ files, targetFolder }: DotContentDriveUploadFiles) {
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('content-drive.file-upload-in-progress'),
            detail: this.#dotMessageService.get('content-drive.file-upload-in-progress-detail')
        });

        this.uploadDotAsset(files[0], targetFolder);
    }

    /**
     * Uploads a file to the content drive
     *
     * @protected
     * @param {File} file
     * @param {string} hostFolder
     * @memberof DotContentDriveShellComponent
     */
    protected uploadDotAsset(file: File, hostFolder: DotFolderTreeNodeData) {
        this.#fileService
            .uploadDotAsset(file, {
                baseType: 'dotAsset',
                hostFolder: hostFolder?.id,
                indexPolicy: 'WAIT_FOR'
            })
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
}
