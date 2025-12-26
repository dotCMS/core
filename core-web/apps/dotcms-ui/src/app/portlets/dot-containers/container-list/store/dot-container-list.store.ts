import { ComponentStore } from '@ngrx/component-store';

import { Injectable, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    PaginatorService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, SiteService } from '@dotcms/dotcms-js';
import {
    CONTAINER_SOURCE,
    DotActionBulkResult,
    DotActionMenuItem,
    DotBulkFailItem,
    DotContainer
} from '@dotcms/dotcms-models';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';
import { ActionHeaderOptions } from '../../../../shared/models/action-header/action-header-options.model';
import { DataTableColumn } from '../../../../shared/models/data-table/data-table-column';
import { DotListingDataTableComponent } from '../../../../view/components/dot-listing-data-table/dot-listing-data-table.component';

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
    containers: DotContainer[];
    maxPageLinks: number;
    totalRecords: number;
}

export interface DotNotifyMessages {
    payload: DotActionBulkResult | DotContainer;
    message: string;
    failsInfo?: DotBulkFailItem[];
}

const CONTAINERS_URL = 'v1/containers';
const DEFAULT_MAX_PAGE_LINKS = 5;

@Injectable()
export class DotContainerListStore extends ComponentStore<DotContainerListState> {
    private route = inject(ActivatedRoute);
    private dotMessageService = inject(DotMessageService);
    private dotRouterService = inject(DotRouterService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private dotSiteBrowserService = inject(DotSiteBrowserService);
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotContainersService = inject(DotContainersService);
    private paginatorService = inject(PaginatorService);
    private dotSiteService = inject(SiteService);

    constructor() {
        super(null);
        this.paginatorService.url = CONTAINERS_URL;
        this.paginatorService.paginationPerPage = 40;

        this.dotSiteService
            .getCurrentSite()
            .pipe(
                take(1),
                switchMap(({ identifier }) => {
                    this.paginatorService.resetExtraParams();

                    this.paginatorService.setExtraParams('host', identifier);

                    return this.route.data.pipe(
                        map((x: any) => x?.dotContainerListResolverData),
                        take(1)
                    );
                })
            )
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
                    } as DotNotifyMessages,
                    containers: [],
                    maxPageLinks: DEFAULT_MAX_PAGE_LINKS,
                    totalRecords: 0
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
            selectedContainers,
            containers,
            totalRecords,
            maxPageLinks
        }: DotContainerListState) => {
            return {
                containerBulkActions,
                addToBundleIdentifier,
                actionHeaderOptions,
                tableColumns,
                stateLabels,
                selectedContainers,
                containers,
                totalRecords,
                maxPageLinks
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

    readonly clearSelectedContainers = this.updater((state: DotContainerListState) => {
        return {
            ...state,
            selectedContainers: []
        };
    });

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

    readonly getContainersByHost = this.effect<string>((identifier$) => {
        return identifier$.pipe(
            switchMap((identifier) => {
                this.paginatorService.setExtraParams('host', identifier);

                return this.paginatorService.getFirstPage();
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

    readonly getContainersByContentType = this.effect<string | undefined>((contentType$) => {
        return contentType$.pipe(
            switchMap((contentType) => {
                contentType
                    ? this.paginatorService.setExtraParams('content_type', contentType)
                    : this.paginatorService.deleteExtraParams('content_type');

                return this.paginatorService.get();
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

    readonly getContainersByArchiveState = this.effect<boolean>((archive$) => {
        return archive$.pipe(
            switchMap((archive) => {
                archive
                    ? this.paginatorService.setExtraParams('archive', archive)
                    : this.paginatorService.deleteExtraParams('archive');

                return this.paginatorService.get();
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

    readonly getContainersByQuery = this.effect<string>((query$) => {
        return query$.pipe(
            switchMap((query) => {
                query.trim().length
                    ? this.paginatorService.setExtraParams('filter', query)
                    : this.paginatorService.deleteExtraParams('filter');

                return this.paginatorService.get();
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

    readonly getContainersWithOffset = this.effect<number>((offset$) => {
        return offset$.pipe(
            switchMap((offset) => {
                return this.paginatorService.getWithOffset(offset);
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

    readonly loadCurrentContainersPage = this.effect((origin$) => {
        return origin$.pipe(
            switchMap(() => {
                return this.paginatorService.getCurrentPage();
            }),
            tap((containers: DotContainer[]) => {
                this.patchContainers(containers);
            })
        );
    });

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
     * Identify if is a container as File based on the identifier pathname.
     * @param {DotContainer} identifier
     * @returns boolean
     * @memberof DotContainerListComponent
     */
    private isContainerAsFile({ pathName }: DotContainer): boolean {
        return !!pathName;
    }

    /**
     * Handle selected container.
     * @param {DotContainer} container
     * @memberof DotContainerListComponent
     */
    editContainer(container: DotContainer): void {
        this.isContainerAsFile(container)
            ? this.dotSiteBrowserService
                  .setSelectedFolder(container.pathName)
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
                            message: this.dotMessageService.get('message.containers.full_delete')
                        });
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Delete-Container'),
            message: this.dotMessageService.get('message.containers.confirm.delete.container')
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
                    message: this.dotMessageService.get('message.containers.unpublished')
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
                    message: this.dotMessageService.get('message.containers.undelete')
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
                    message: this.dotMessageService.get('message.containers.delete')
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

    /**
     * Return a list of containers with disableInteraction in system items.
     * @param {DotContainer[]} containers
     * @returns DotContainer[]
     * @memberof DotContainerListStore
     */
    private getContainersWithDisabledEntities(containers: DotContainer[]): DotContainer[] {
        return containers.map((container) => {
            const copyContainer = structuredClone(container);
            copyContainer.disableInteraction =
                copyContainer.identifier.includes('/') ||
                copyContainer.identifier === 'SYSTEM_CONTAINER' ||
                copyContainer.source === CONTAINER_SOURCE.FILE;

            if (copyContainer.path) {
                copyContainer.pathName = new URL(`http:${container.path}`).pathname;
            }

            return copyContainer;
        });
    }

    /**
     * Patch the state with the containers and pagination info.
     *
     * @private
     * @param {DotContainer[]} containers
     * @memberof DotContainerListStore
     */
    private patchContainers(containers: DotContainer[]): void {
        this.patchState({
            containers: this.getContainersWithDisabledEntities(containers),
            maxPageLinks: this.paginatorService.maxLinksPage,
            totalRecords: this.paginatorService.totalRecords
        });
    }
}
