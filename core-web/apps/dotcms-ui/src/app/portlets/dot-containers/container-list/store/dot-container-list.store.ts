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

export interface DotContainerListState {
    containerBulkActions: MenuItem[];
    selectedContainers: DotContainer[];
    addToBundleIdentifier: string;
    actionHeaderOptions: ActionHeaderOptions;
    tableColumns: DataTableColumn[];
    isEnterprise: boolean;
    hasEnvironments: boolean;
    stateLabels: { [key: string]: string };
}

@Injectable()
export class DotContainerListStore extends ComponentStore<DotContainerListState> {
    constructor(
        private route: ActivatedRoute,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private dotPushPublishDialogService: DotPushPublishDialogService
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
                    actionHeaderOptions: this.getActionHeaderOptions()
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
}
