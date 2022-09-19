import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { MenuItem } from 'primeng/api';
import { DotContainer } from '@models/container/dot-container.model';
import { ActionHeaderOptions } from '@models/action-header';
import { DataTableColumn } from '@models/data-table';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import {
    DotActionBulkResult,
    DotBulkFailItem
} from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DialogService } from 'primeng/dynamicdialog';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';

export interface DotContainerListState {
    containerBulkActions: MenuItem[];
    selectedContainers: DotContainer[];
    addToBundleIdentifier: string;
    actionHeaderOptions: ActionHeaderOptions;
    tableColumns: DataTableColumn[];
    isEnterprise: boolean;
    hasEnvironments: boolean;
    stateLabels: { [key: string]: string };
    listing: DotListingDataTableComponent;
}

@Injectable()
export class DotContainerListStore extends ComponentStore<DotContainerListState> {
    constructor(
        private route: ActivatedRoute,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotSiteBrowserService: DotSiteBrowserService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotContainersService: DotContainersService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dialogService: DialogService
    ) {
        super(null);

        this.route.data
            .pipe(pluck('dotContainerListResolverData'), take(1))
            .subscribe(([isEnterprise, hasEnvironments]: [boolean, boolean]) => {
                this.setState({
                    containerBulkActions: this.getContainerBulkActions(
                        hasEnvironments,
                        isEnterprise
                    ),
                    tableColumns: this.getContainerColumns(),
                    stateLabels: this.getStateLabels(),
                    isEnterprise: isEnterprise,
                    hasEnvironments: hasEnvironments,
                    addToBundleIdentifier: '',
                    selectedContainers: [],
                    actionHeaderOptions: this.getActionHeaderOptions(),
                    listing: {} as DotListingDataTableComponent
                });
            });
    }

    readonly vm$ = this.select(
        ({
            containerBulkActions,
            addToBundleIdentifier,
            actionHeaderOptions,
            tableColumns,
            stateLabels
        }: DotContainerListState) => {
            return {
                containerBulkActions,
                addToBundleIdentifier,
                actionHeaderOptions,
                tableColumns,
                stateLabels
            };
        }
    );

    readonly updateBundleIdentifier = this.updater<string>(
        (state: DotContainerListState, addToBundleIdentifier: string) => {
            return {
                ...state,
                addToBundleIdentifier
            };
        }
    );

    readonly updateSelectedContainers = this.updater<DotContainer[]>(
        (state: DotContainerListState, selectedContainers: DotContainer[]) => {
            return {
                ...state,
                selectedContainers
            };
        }
    );

    readonly updateListing = this.updater<DotListingDataTableComponent>(
        (state: DotContainerListState, listing: DotListingDataTableComponent) => {
            return {
                ...state,
                listing
            };
        }
    );

    private getContainerBulkActions(hasEnvironments = false, isEnterprise = false) {
        return [
            {
                label: this.dotMessageService.get('Publish')
            },
            ...this.getLicenseAndRemotePublishContainerBulkOptions(hasEnvironments, isEnterprise),
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

    private getContainerColumns(): DataTableColumn[] {
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

    private getActionHeaderOptions(): ActionHeaderOptions {
        return {
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
     * @memberof DotContainerListStore
     */
    private getStateLabels = () => {
        return {
            archived: this.dotMessageService.get('Archived'),
            published: this.dotMessageService.get('Published'),
            revision: this.dotMessageService.get('Revision'),
            draft: this.dotMessageService.get('Draft')
        };
    };

    private getLicenseAndRemotePublishContainerBulkOptions(
        hasEnvironments: boolean,
        isEnterprise: boolean
    ): MenuItem[] {
        const bulkOptions: MenuItem[] = [];
        if (hasEnvironments) {
            bulkOptions.push({
                label: this.dotMessageService.get('Remote-Publish'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: selectedContainers
                            .map((container) => container.identifier)
                            .toString(),
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    });
                }
            });
        }

        if (isEnterprise) {
            bulkOptions.push({
                label: this.dotMessageService.get('Add-To-Bundle'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.updateBundleIdentifier(
                        selectedContainers.map((container) => container.identifier).toString()
                    );
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
        const { hasEnvironments, isEnterprise } = this.get();
        if (hasEnvironments) {
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

        if (isEnterprise) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Add-To-Bundle'),
                    command: () => {
                        this.updateBundleIdentifier(container.identifier);
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
     * @param {DotContainer} {container}
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
        const { listing } = this.get();
        if (response.fails.length) {
            this.showErrorDialog({
                ...response,
                fails: this.getFailsInfo(response.fails),
                action: this.dotMessageService.get(messageKey)
            });
        } else {
            this.showToastNotification(this.dotMessageService.get(messageKey));
        }

        listing.clearSelection();
        listing.loadCurrentPage();
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
        const { listing } = this.get();

        return (listing.items as DotContainer[]).find((template: DotContainer) => {
            return template.identifier === identifier;
        }).name;
    }
}
