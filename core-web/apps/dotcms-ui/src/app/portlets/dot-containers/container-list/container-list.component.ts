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
        private dotRouterService: DotRouterService
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
                header: this.dotMessageService.get('containers.fieldName.name'),
                sortable: true
            },
            {
                fieldName: 'status',
                header: this.dotMessageService.get('containers.fieldName.status'),
                width: '8%'
            },
            {
                fieldName: 'friendlyName',
                header: this.dotMessageService.get('containers.fieldName.description')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('containers.fieldName.lastEdit'),
                sortable: true
            }
        ];
    }

    private setAddOptions(): void {
        this.actionHeaderOptions = {
            primary: {
                command: () => {
                    this.dotRouterService.gotoPortlet(`/container-new/new`);
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
}
