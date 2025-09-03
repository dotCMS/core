/* eslint-disable no-console */

import { JsonPipe, Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { LazyLoadEvent, SortEvent } from 'primeng/api';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotFolderListViewComponent, } from '@dotcms/portlets/content-drive/ui';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotContentDriveToolbarComponent } from '../components/dot-content-drive-toolbar/dot-content-drive-toolbar.component';
import { ContextMenuData, DotFolderListViewContextMenuComponent } from '../components/dot-folder-list-context-menu/dot-folder-list-context-menu.component';
import { SORT_ORDER } from '../shared/constants';
import { DotContentDriveSortOrder, DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';
import { encodeFilters } from '../utils/functions';

@Component({
    selector: 'dot-content-drive-shell',
    imports: [DotFolderListViewComponent, DotContentDriveToolbarComponent, DotFolderListViewContextMenuComponent, DotAddToBundleComponent, JsonPipe],
    providers: [DotContentDriveStore, DotWorkflowsActionsService],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveShellComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #workflowsActionsService = inject(DotWorkflowsActionsService);

    readonly $items = this.#store.items;
    readonly $totalItems = this.#store.totalItems;
    readonly $status = this.#store.status;
    readonly $treeExpanded = this.#store.isTreeExpanded;
    readonly $contextMenuData = this.#store.contextMenu;

    readonly DOT_CONTENT_DRIVE_STATUS = DotContentDriveStatus;


    readonly addToBundleEffect = effect(() => {
        const showAddToBundle = this.#store.contextMenu()?.showAddToBundle;
        console.log('addToBundleEffect', showAddToBundle);
    });

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

    onPaginate(event: LazyLoadEvent) {
        // Explicit check because it can potentially be 0
        if (event.rows === undefined || event.first === undefined) {
            return;
        }

        this.#store.setPagination({
            limit: event.rows,
            offset: event.first
        });
    }

    onSort(event: SortEvent) {
        // Explicit check because it can potentially be 0
        if (event.order === undefined || !event.field) {
            return;
        }

        this.#store.setSort({
            field: event.field,
            order: SORT_ORDER[event.order] ?? DotContentDriveSortOrder.ASC
        });
    }

    onContextMenu({event, contentlet}: ContextMenuData) {
        // console.log('called onContextMenu from dot-content-drive-shell. Setting the store', {event, contentlet});
        event.preventDefault();
        // this.$contextMenuData.set({event, contentlet});
        this.#store.patchContextMenu({ triggeredEvent: event,contentlet });

    }

    cancelAddToBundle() {
        this.#store.setShowAddToBundle(false);
    }
}
