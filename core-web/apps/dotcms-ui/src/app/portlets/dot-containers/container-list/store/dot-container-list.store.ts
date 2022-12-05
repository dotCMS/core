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
    notifyMessages: DotNotifyMessages;
}

export interface DotNotifyMessages {
    payload: DotActionBulkResult | DotContainer;
    message: string;
    failsInfo?: DotBulkFailItem[];
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
        private dotContainersService: DotContainersService
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
                    listing: {} as DotListingDataTableComponent,
                    notifyMessages: {
                        payload: {},
                        message: null,
                        failsInfo: []
                    } as DotNotifyMessages
                });
            });
    }

    readonly vm$ = this.select(
        ({
            containerBulkActions,
            addToBundleIdentifier,
            actionHeaderOptions,
            tableColumns,
            stateLabels,
            selectedContainers
        }: DotContainerListState) => {
            return {
                containerBulkActions,
                addToBundleIdentifier,
                actionHeaderOptions,
                tableColumns,
                stateLabels,
                selectedContainers
            };
        }
    );

    readonly notify$ = this.select(({ notifyMessages }: DotContainerListState) => {
        return notifyMessages;
    });

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

    readonly updateNotifyMessages = this.updater<DotNotifyMessages>(
        (state: DotContainerListState, notifyMessages: DotNotifyMessages) => {
            const { payload } = notifyMessages;

            if ('fails' in payload && payload.fails.length) {
                notifyMessages.failsInfo = this.getFailsInfo(payload.fails);
            }

            return {
                ...state,
                notifyMessages
            };
        }
    );

    /**
     * Set the actions of each container based o current state.
     * @param {DotContainer} container
     ** @returns DotActionMenuItem[]
     * @memberof DotContainerListComponent
     */
    getContainerActions(container: DotContainer): DotActionMenuItem[] {
        let options: DotActionMenuItem[];
        if (container.deleted) {
            options = this.setArchiveContainerActions(container);
        } else {
            options = this.setBaseContainerOptions(container);
            options = [
                ...options,
                ...this.getLicenseAndRemotePublishContainerOptions(container),
                ...this.getUnPublishAndArchiveContainerOptions(container)
            ];

            options = [...options, ...this.setCopyContainerOptions(container)];
        }

        return options;
    }

    private getContainerBulkActions(hasEnvironments = false, isEnterprise = false) {
        return [
            {
                label: this.dotMessageService.get('Publish'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.publishContainer(
                        selectedContainers.map((container) => container.identifier)
                    );
                }
            },
            ...this.getLicenseAndRemotePublishContainerBulkOptions(hasEnvironments, isEnterprise),
            {
                label: this.dotMessageService.get('Unpublish'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.unPublishContainer(
                        selectedContainers.map((container) => container.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Archive'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.archiveContainers(
                        selectedContainers.map((container) => container.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Unarchive'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.unArchiveContainer(
                        selectedContainers.map((container) => container.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Delete'),
                command: () => {
                    const { selectedContainers } = this.get();
                    this.deleteContainer(
                        selectedContainers.map((container) => container.identifier)
                    );
                }
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
                    this.dotRouterService.gotoPortlet(`/containers/create`);
                }
            }
        };
    }

    /**
     * set the labels of dot-state-icon.
     * @returns { [key: string]: string }
     * @memberof DotContainerListStore
     */
    private getStateLabels(): { [key: string]: string } {
        return {
            archived: this.dotMessageService.get('Archived'),
            published: this.dotMessageService.get('Published'),
            revision: this.dotMessageService.get('Revision'),
            draft: this.dotMessageService.get('Draft')
        };
    }

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

    private getUnPublishAndArchiveContainerOptions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (container.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unpublish'),
                    command: () => {
                        this.unPublishContainer([container.identifier]);
                    }
                }
            });
        } else {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Archive'),
                    command: () => {
                        this.archiveContainers([container.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private getLicenseAndRemotePublishContainerOptions(
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
        return [
            {
                menuItem: {
                    label: this.dotMessageService.get('Duplicate'),
                    command: () => {
                        this.copyContainer(container.identifier);
                    }
                }
            }
        ];
    }

    private setBaseContainerOptions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];

        options.push({
            menuItem: {
                label: this.dotMessageService.get('edit'),
                command: () => {
                    this.editContainer(container);
                }
            }
        });

        options.push({
            menuItem: {
                label: this.dotMessageService.get('publish'),
                command: () => {
                    this.publishContainer([container.identifier]);
                }
            }
        });

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
     * @param {DotContainer} identifier
     * @returns boolean
     * @memberof DotContainerListComponent
     */
    private isContainerAsFile({ identifier }: DotContainer): boolean {
        return identifier.includes('/');
    }

    /**
     * Handle selected container.
     * @param {DotContainer} container
     * @memberof DotContainerListComponent
     */
    editContainer(container: DotContainer): void {
        this.isContainerAsFile(container)
            ? this.dotSiteBrowserService
                  .setSelectedFolder(container.identifier)
                  .pipe(take(1))
                  .subscribe(() => {
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
                    .subscribe((payload: DotActionBulkResult) => {
                        this.updateNotifyMessages({
                            payload,
                            message: this.dotMessageService.get('message.container.full_delete')
                        });
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Delete-Container'),
            message: this.dotMessageService.get('message.container.confirm.delete.container')
        });
    }

    private publishContainer(identifiers: string[]): void {
        this.dotContainersService
            .publish(identifiers)
            .pipe(take(1))
            .subscribe((payload: DotActionBulkResult) => {
                this.updateNotifyMessages({
                    payload,
                    message: this.dotMessageService.get('message.container_list.published')
                });
            });
    }

    private copyContainer(identifier: string): void {
        this.dotContainersService
            .copy(identifier)
            .pipe(take(1))
            .subscribe((payload: DotContainer) => {
                this.updateNotifyMessages({
                    payload,
                    message: this.dotMessageService.get('message.container_list.published')
                });
            });
    }

    private unPublishContainer(identifiers: string[]): void {
        this.dotContainersService
            .unPublish(identifiers)
            .pipe(take(1))
            .subscribe((payload: DotActionBulkResult) => {
                this.updateNotifyMessages({
                    payload,
                    message: this.dotMessageService.get('message.container.unpublished')
                });
            });
    }

    private unArchiveContainer(identifiers: string[]): void {
        this.dotContainersService
            .unArchive(identifiers)
            .pipe(take(1))
            .subscribe((payload: DotActionBulkResult) => {
                this.updateNotifyMessages({
                    payload,
                    message: this.dotMessageService.get('message.container.undelete')
                });
            });
    }

    private archiveContainers(identifiers: string[]): void {
        this.dotContainersService
            .archive(identifiers)
            .pipe(take(1))
            .subscribe((payload: DotActionBulkResult) => {
                this.updateNotifyMessages({
                    payload,
                    message: this.dotMessageService.get('message.container.delete')
                });
            });
    }

    private getFailsInfo(items: DotBulkFailItem[]): DotBulkFailItem[] {
        return items.map((item: DotBulkFailItem) => {
            return { ...item, description: this.getContainerName(item.element) };
        });
    }

    private getContainerName(identifier: string): string {
        const { selectedContainers } = this.get();

        return selectedContainers.find((container: DotContainer) => {
            return container.identifier === identifier;
        }).name;
    }
}
