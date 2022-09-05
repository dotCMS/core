import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { MenuItem } from 'primeng/api';
import { pluck, take } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActionHeaderOptions } from '@models/action-header';
import { DataTableColumn } from '@models/data-table';
import { DotContainer } from '@models/container/dot-container.model';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import {
    DotActionBulkResult,
    DotBulkFailItem
} from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DialogService } from 'primeng/dynamicdialog';
import { DotTemplate } from '@models/dot-edit-layout-designer';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    styleUrls: ['./container-list.component.scss']
})
export class ContainerListComponent implements OnInit, OnDestroy {
    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;
    containerBulkActions: MenuItem[];
    selectedContainers: DotContainer[] = [];
    addToBundleIdentifier: string;
    actionHeaderOptions: ActionHeaderOptions;
    tableColumns: DataTableColumn[];

    private isEnterPrise: boolean;
    private hasEnvironments: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotRouterService: DotRouterService,
        private dotSiteBrowserService: DotSiteBrowserService,
        private dotContainersService: DotContainersService,
        private dotMessageDisplayService: DotMessageDisplayService,
        public dialogService: DialogService
    ) {}

    ngOnInit(): void {
        this.route.data
            .pipe(pluck('dotContainerListResolverData'), take(1))
            .subscribe(([isEnterPrise, hasEnvironments]: [boolean, boolean]) => {
                this.isEnterPrise = isEnterPrise;
                this.hasEnvironments = hasEnvironments;
                this.tableColumns = this.setContainerColumns();
                this.containerBulkActions = this.setContainerBulkActions();
            });
        this.setAddOptions();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setContainerColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'title',
                header: this.dotMessageService.get('message.containers.fieldName.name'),
                sortable: true
            },
            {
                fieldName: 'status',
                header: this.dotMessageService.get('message.containers.fieldName.status'),
                width: '8%'
            },
            {
                fieldName: 'friendlyName',
                header: this.dotMessageService.get('message.containers.fieldName.description')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('message.containers.fieldName.lastEdit'),
                sortable: true
            }
        ];
    }

    private setAddOptions(): void {
        this.actionHeaderOptions = {
            primary: {
                command: () => {
                    this.dotRouterService.gotoPortlet(`/container-new/create`);
                }
            }
        };
    }

    /**
     * set the labels of dot-state-icon.
     * @returns { [key: string]: string }
     * @memberof DotContainerListComponent
     */
    setStateLabels(): { [key: string]: string } {
        return {
            archived: this.dotMessageService.get('Archived'),
            published: this.dotMessageService.get('Published'),
            revision: this.dotMessageService.get('Revision'),
            draft: this.dotMessageService.get('Draft')
        };
    }

    /**
     * get the attributes that define the state of a template.
     * @param {DotContainer} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof DotContainerListComponent
     */
    getContainerState({ live, working, deleted }: DotContainer): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    /**
     * Handle filter for hide / show archive containers
     * @param {boolean} checked
     *
     * @memberof DotContainerListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        checked
            ? this.listing.paginatorService.setExtraParams('archive', checked)
            : this.listing.paginatorService.deleteExtraParams('archive');
        this.listing.loadFirstPage();
    }

    private setContainerBulkActions(): MenuItem[] {
        return [
            {
                label: this.dotMessageService.get('Publish')
            },
            ...this.setLicenseAndRemotePublishContainerBulkOptions(),
            {
                label: this.dotMessageService.get('Unpublish')
            },
            {
                label: this.dotMessageService.get('Archive')
            },
            {
                label: this.dotMessageService.get('Unarchive')
            },
            {
                label: this.dotMessageService.get('Delete')
            }
        ];
    }

    private setLicenseAndRemotePublishContainerBulkOptions(): MenuItem[] {
        const bulkOptions: MenuItem[] = [];
        if (this.hasEnvironments) {
            bulkOptions.push({
                label: this.dotMessageService.get('Remote-Publish'),
                command: () => {
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: this.selectedContainers
                            .map((container) => container.identifier)
                            .toString(),
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    });
                }
            });
        }

        if (this.isEnterPrise) {
            bulkOptions.push({
                label: this.dotMessageService.get('Add-To-Bundle'),
                command: () => {
                    this.addToBundleIdentifier = this.selectedContainers
                        .map((container) => container.identifier)
                        .toString();
                }
            });
        }

        return bulkOptions;
    }

    /**
     * Set the actions of each template based o current state.
     * @param {DotContainer} container
     ** @returns DotActionMenuItem[]
     * @memberof DotContainerListComponent
     */
    setContainerActions(container: DotContainer): DotActionMenuItem[] {
        let options: DotActionMenuItem[];
        if (container.deleted) {
            options = this.setArchiveContainerActions(container);
        } else {
            options = this.setBaseContainerOptions(container);
            options = [...options, ...this.setCopyContainerOptions(container)];

            if (!container.live) {
                options = [
                    ...options,
                    ...this.setLicenseAndRemotePublishContainerOptions(container),
                    ...this.setUnPublishAndArchiveContainerOptions(container)
                ];
            }
        }

        return options;
    }

    private setUnPublishAndArchiveContainerOptions(template: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (template.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unpublish'),
                    command: () => {
                        this.unPublishContainer([template.identifier]);
                    }
                }
            });
        } else {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Archive'),
                    command: () => {
                        this.archiveContainers([template.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private setLicenseAndRemotePublishContainerOptions(
        container: DotContainer
    ): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (this.hasEnvironments) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Remote-Publish'),
                    command: () => {
                        this.dotPushPublishDialogService.open({
                            assetIdentifier: container.identifier,
                            title: this.dotMessageService.get('contenttypes.content.push_publish')
                        });
                    }
                }
            });
        }

        if (this.isEnterPrise) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Add-To-Bundle'),
                    command: () => {
                        this.addToBundleIdentifier = container.identifier;
                    }
                }
            });
        }

        return options;
    }

    private setCopyContainerOptions(container: DotContainer): DotActionMenuItem[] {
        return !container.locked
            ? [
                  {
                      menuItem: {
                          label: this.dotMessageService.get('Duplicate'),
                          command: () => {
                              //
                          }
                      }
                  }
              ]
            : [];
    }

    private setBaseContainerOptions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];

        if (!container.locked) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('edit'),
                    command: () => {
                        this.editContainer(container);
                    }
                }
            });
        }

        if (!container.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('publish'),
                    command: () => {
                        this.publishContainer(container.identifier);
                    }
                }
            });
        }

        return options;
    }

    private setArchiveContainerActions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (!container.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unarchive'),
                    command: () => {
                        this.unArchiveContainer([container.identifier]);
                    }
                }
            });
        }

        if (!container.locked) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Delete'),
                    command: () => {
                        this.deleteContainer([container.identifier]);
                    }
                }
            });
        }

        return options;
    }

    /**
     * Identify if is a container as File based on the identifier path.
     * @param {DotContainer} {identifier}
     * @returns boolean
     * @memberof DotContainerListComponent
     */
    isContainerAsFile({ identifier }: DotContainer): boolean {
        return identifier.includes('/');
    }

    /**
     * Handle selected container.
     *
     * @param {DotContainer} { container }
     * @memberof DotContainerListComponent
     */
    editContainer(container: DotContainer): void {
        this.isContainerAsFile(container)
            ? this.dotSiteBrowserService.setSelectedFolder(container.identifier).subscribe(() => {
                  this.dotRouterService.goToSiteBrowser();
              })
            : this.dotRouterService.goToEditContainer(container.identifier);
    }

    private deleteContainer(identifiers: string[]): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotContainersService
                    .delete(identifiers)
                    .pipe(take(1))
                    .subscribe((response: DotActionBulkResult) => {
                        this.notifyResult(response, 'message.template.full_delete');
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Delete-Container'),
            message: this.dotMessageService.get('message.container.confirm.delete.container')
        });
    }

    private publishContainer(identifier: string): void {
        this.dotContainersService
            .publish(identifier)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container_list.published');
            });
    }

    private unPublishContainer(identifiers: string[]): void {
        this.dotContainersService
            .unPublish(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.unpublished');
            });
    }

    private unArchiveContainer(identifiers: string[]): void {
        this.dotContainersService
            .unArchive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.undelete');
            });
    }

    private archiveContainers(identifiers: string[]): void {
        this.dotContainersService
            .archive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.delete');
            });
    }

    private notifyResult(response: DotActionBulkResult, messageKey: string): void {
        if (response.fails.length) {
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
            return { ...item, description: this.getTemplateName(item.element) };
        });
    }

    private getTemplateName(identifier: string): string {
        return (this.listing.items as DotTemplate[]).find((template: DotTemplate) => {
            return template.identifier === identifier;
        }).name;
    }
}
