import { Component, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { DotContainer } from '@models/container/dot-container.model';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotContainerListStore } from '@portlets/dot-containers/container-list/store/dot-container-list.store';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import {
    DotActionBulkResult,
    DotBulkFailItem
} from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DialogService } from 'primeng/dynamicdialog';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    styleUrls: ['./container-list.component.scss'],
    providers: [DotContainerListStore]
})
export class ContainerListComponent {
    vm$ = this.store.vm$;
    notify$ = this.store.notify$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    constructor(
        private store: DotContainerListStore,
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dialogService: DialogService
    ) {
        this.notify$.subscribe(({ payload, messageKey }) => {
            this.notifyResult(payload, messageKey);
        });
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
     * Handle filter for hide / show archive containers
     * @param {boolean} checked
     *
     * @memberof ContainerListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        checked
            ? this.listing.paginatorService.setExtraParams('archive', checked)
            : this.listing.paginatorService.deleteExtraParams('archive');
        this.listing.loadFirstPage();
    }

    /**
     * Keep updated the selected containers in the grid
     * @param {DotContainer[]} containers
     *
     * @memberof ContainerListComponent
     */
    updateSelectedContainers(containers: DotContainer[]): void {
        this.store.updateSelectedContainers(containers);
    }

    /**
     * Reset bundle state to null
     *
     * @memberof ContainerListComponent
     */
    resetBundleIdentifier(): void {
        this.store.updateBundleIdentifier(null);
    }

    setContainerActions(container: DotContainer): DotActionMenuItem[] {
        return this.store.getContainerActions(container);
    }

    private notifyResult(response: DotActionBulkResult | DotContainer, messageKey: string): void {
        if ('fails' in response && response.fails.length) {
            this.showErrorDialog({
                ...response,
                fails: this.getFailsInfo(response.fails),
                action: this.dotMessageService.get(messageKey)
            });
        } else {
            this.showToastNotification(this.dotMessageService.get(messageKey));
        }

        this.listing.clearSelection();
        this.listing.loadCurrentPage();
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
            header: this.dotMessageService.get('Results'),
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: result
        });
    }

    private getFailsInfo(items: DotBulkFailItem[]): DotBulkFailItem[] {
        return items.map((item: DotBulkFailItem) => {
            return { ...item, description: this.getContainerName(item.element) };
        });
    }

    private getContainerName(identifier: string): string {
        return (this.listing.items as DotContainer[]).find((template: DotContainer) => {
            return template.identifier === identifier;
        }).name;
    }
}
