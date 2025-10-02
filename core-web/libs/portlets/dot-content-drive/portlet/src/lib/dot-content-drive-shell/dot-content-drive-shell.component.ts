import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    ElementRef,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { Router } from '@angular/router';

import { LazyLoadEvent, MessageService, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import {
    DotFolderService,
    DotUploadFileService,
    DotWorkflowActionsFireService,
    DotLocalstorageService,
    DotWorkflowsActionsService,
    DotMessageService
} from '@dotcms/data-access';
import { ContextMenuData, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { DotAddToBundleComponent, DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
import { DotContentDriveDropzoneComponent } from '../components/dot-content-drive-dropzone/dot-content-drive-dropzone.component';
import { DotContentDriveSidebarComponent } from '../components/dot-content-drive-sidebar/dot-content-drive-sidebar.component';
import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import { DIALOG_TYPE, HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY, SORT_ORDER } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { encodeFilters } from '../utils/functions';
import { ALL_FOLDER } from '../utils/tree-folder.utils';

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
        DotContentDriveDropzoneComponent
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
    readonly #workflowActionsFireService = inject(DotWorkflowActionsFireService);

    readonly #localStorageService = inject(DotLocalstorageService);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;
    readonly $contextMenuData = this.#store.contextMenu;

    readonly $dialog = this.#store.dialog;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;
    readonly DIALOG_TYPE = DIALOG_TYPE;

    // Default to false to avoid showing the message banner on init
    readonly $showMessage = signal<boolean>(false);

    readonly $fileInput = viewChild<ElementRef>('fileInput');

    readonly updateQueryParamsEffect = effect(() => {
        const isTreeExpanded = this.#store.isTreeExpanded();
        const path = this.#store.path();
        const filters = this.#store.filters();

        const queryParams: Record<string, string> = {};

        queryParams['isTreeExpanded'] = isTreeExpanded.toString();

        if (path && path.length) {
            queryParams['path'] = path;
        }

        if (filters && Object.keys(filters).length) {
            queryParams['filters'] = encodeFilters(filters);
        } else {
            delete queryParams['filters'];
        }

        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
    });

    ngOnInit() {
        this.$showMessage.set(
            !this.#localStorageService.getItem(HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY) // The existence of the key means the message banner has been hidden
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

    protected onFileChange(event: Event) {
        const input = event.target as HTMLInputElement;

        if (!input.files || input.files.length === 0) {
            return;
        }

        const file = input.files[0];

        this.#store.setStatus(DotContentDriveStatus.LOADING);

        const hostFolder =
            this.#store.selectedNode() === ALL_FOLDER
                ? this.#store.currentSite()?.identifier
                : this.#store.selectedNode()?.data.id;

        this.#fileService
            .uploadDotAsset(file, {
                baseType: 'dotAsset',
                hostFolder,
                indexPolicy: 'WAIT_FOR'
            })
            .subscribe({
                next: () => {
                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-success')
                    });
                    this.#store.loadItems();
                },
                error: (error) => {
                    console.error('error => ', error);
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.add-dotasset-error'),
                        detail: this.#dotMessageService.get(
                            'content-drive.add-dotasset-error-detail'
                        )
                    });
                    this.#store.setStatus(DotContentDriveStatus.LOADED);
                }
            });
    }
}
