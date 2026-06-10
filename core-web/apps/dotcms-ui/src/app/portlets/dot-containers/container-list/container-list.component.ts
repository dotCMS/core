import { Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, ElementRef, inject, OnDestroy, viewChild, viewChildren } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';

import { takeUntil } from 'rxjs/operators';

import {
    DotMessageDisplayService,
    DotMessageService,
    DotSiteBrowserService
} from '@dotcms/data-access';
import {
    CONTAINER_SOURCE,
    DotActionBulkResult,
    DotActionMenuItem,
    DotBulkFailItem,
    DotContainer,
    DotContentState,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import {
    DotAddToBundleComponent,
    DotContentletStatusChipComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { DotContainerListResolver } from './dot-container-list-resolver.service';
import { DotContainerListStore } from './store/dot-container-list.store';

import { DotContainersService } from '../../../api/services/dot-containers/dot-containers.service';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotContentTypeSelectorComponent } from '../../../view/components/dot-content-type-selector/dot-content-type-selector.component';
import { ActionHeaderComponent } from '../../../view/components/dot-listing-data-table/action-header/action-header.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    imports: [
        DotPortletBaseComponent,
        TableModule,
        DotContentTypeSelectorComponent,
        DotMessagePipe,
        ButtonModule,
        CheckboxModule,
        ContextMenuModule,
        MenuModule,
        DotAddToBundleComponent,
        DotRelativeDatePipe,
        ActionHeaderComponent,
        InputTextModule,
        DotContentletStatusChipComponent,
        AsyncPipe
    ],
    providers: [
        DotContainerListStore,
        DotContainerListResolver,
        DotSiteBrowserService,
        DotContainersService,
        DialogService
    ]
})
export class ContainerListComponent implements OnDestroy {
    private dotMessageService = inject(DotMessageService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dialogService = inject(DialogService);
    readonly #globalStore = inject(GlobalStore);

    actionsMenu = viewChild<Menu>('actionsMenu');
    rowContextMenu = viewChild<ContextMenu>('rowContextMenu');
    tableRows = viewChildren<ElementRef<HTMLTableRowElement>>('tableRow');

    readonly #store = inject(DotContainerListStore);

    vm$ = this.#store.vm$;
    notify$ = this.#store.notify$;

    selectedContainers: DotContainer[] = [];
    contextMenuItems: MenuItem[] = [];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        this.notify$.pipe(takeUntil(this.destroy$)).subscribe(({ payload, message, failsInfo }) => {
            this.notifyResult(payload, failsInfo, message);
            this.selectedContainers = [];
        });

        this.#globalStore
            .switchSiteEvent$()
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ identifier }) => this.#store.getContainersByHost(identifier));
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Change content type to the selected one
     * @param {string} value
     * @memberof ContainerListComponent
     */
    changeContentTypeSelector(value: string) {
        this.#store.getContainersByContentType(value);
    }

    /**
     * Get the attributes that define the state of a container.
     * @param {DotContainer} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof ContainerListComponent
     */
    getContainerState({ live, working, deleted }: DotContainer): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    /**
     * Get the clicked container row and redirect to edit container page.
     * @param {DotContainer} container
     * @memberof ContainerListComponent
     */
    handleRowClick(container: DotContainer) {
        this.#store.editContainer(container);
    }

    /**
     * Handle filter for hide / show archive containers
     * @param {boolean} checked
     *
     * @memberof ContainerListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        this.#store.getContainersByArchiveState(checked);
    }

    /**
     * Handle query filter
     *
     * @param {string} query
     * @memberof ContainerListComponent
     */
    handleQueryFilter(query: string): void {
        this.#store.getContainersByQuery(query);
    }

    /**
     * Call when click on any pagination link
     * @param {LazyLoadEvent} event
     *
     * @memberof DotContainerListComponent
     */
    loadDataPaginationEvent({ first }: { first: number }): void {
        this.#store.getContainersWithOffset(first);
    }

    /**
     * Reset bundle state to null
     *
     * @memberof ContainerListComponent
     */
    resetBundleIdentifier(): void {
        this.#store.updateBundleIdentifier(null);
    }

    setContainerActions(container: DotContainer): DotActionMenuItem[] {
        return this.#store.getContainerActions(container);
    }

    // Invoked by both right-click and the 3-dot action button.
    setContextMenu(event: Event, container: DotContainer): void {
        if (container.disableInteraction) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        this.contextMenuItems = this.setContainerActions(container).map(
            ({ menuItem }: DotActionMenuItem) => menuItem
        );
        this.rowContextMenu()?.show(event);
    }

    /**
     * Handle action menu click
     *
     * @param {MouseEvent} event
     * @memberof ContainerListComponent
     */
    handleActionMenuOpen(event: MouseEvent): void {
        this.updateSelectedContainers();
        this.actionsMenu()?.toggle(event);
    }

    handleSelectionChange(): void {
        this.updateSelectedContainers();
    }

    /**
     * Focus first row if key arrow down on input
     *
     * @memberof ContainerListComponent
     */
    focusFirstRow(): void {
        const { nativeElement: firstActiveRow } = this.tableRows().find(
            (row) => row.nativeElement.getAttribute('data-disabled') === 'false'
        ) || { nativeElement: null }; // To not break on destructuring

        firstActiveRow?.focus();
    }

    /**
     * Keep updated the selected containers in the store
     *
     * @memberof ContainerListComponent
     */
    private updateSelectedContainers(): void {
        const filterContainers = this.selectedContainers.filter(
            (container: DotContainer) =>
                container.identifier !== 'SYSTEM_CONTAINER' &&
                container.source !== CONTAINER_SOURCE.FILE
        );
        this.#store.updateSelectedContainers(filterContainers);
    }

    private notifyResult(
        response: DotActionBulkResult | DotContainer,
        failsInfo: DotBulkFailItem[],
        message: string
    ): void {
        if ('fails' in response && failsInfo?.length) {
            this.showErrorDialog({
                ...response,
                fails: failsInfo,
                action: message
            });
        } else if (message) {
            this.showToastNotification(message);
        }

        this.#store.clearSelectedContainers();
        this.#store.loadCurrentContainersPage();
    }

    private showToastNotification(message: string): void {
        this.dotMessageDisplayService.push({
            life: 3000,
            message: message,
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }

    private showErrorDialog(result: DotActionBulkResult): void {
        this.dialogService.open(DotBulkInformationComponent, {
            closable: true,
            header: this.dotMessageService.get('Results'),
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: result
        });
    }
}
