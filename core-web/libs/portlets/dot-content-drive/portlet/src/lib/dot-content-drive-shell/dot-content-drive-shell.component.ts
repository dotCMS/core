import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { LazyLoadEvent, MessageService, SortEvent } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';

import { DotFolderService, DotWorkflowsActionsService } from '@dotcms/data-access';
import { ContextMenuData, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotContentDriveDialogFolderComponent } from '../components/dialogs/dot-content-drive-dialog-folder/dot-content-drive-dialog-folder.component';
import { DotContentDriveSidebarComponent } from '../components/dot-content-drive-sidebar/dot-content-drive-sidebar.component';
import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import { DIALOG_TYPE, SORT_ORDER } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveNavigationService } from '../shared/services';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { encodeFilters } from '../utils/functions';

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
    DotContentDriveDialogFolderComponent
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

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;
    readonly $contextMenuData = this.#store.contextMenu;

    readonly $dialog = this.#store.dialog;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;
    readonly DIALOG_TYPE = DIALOG_TYPE;

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
        this.#store.resetDialog();
    }
}
