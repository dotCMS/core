import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { MenuItem } from 'primeng/api';
import { pluck, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActionHeaderOptions } from '@models/action-header';
import { DataTableColumn } from '@models/data-table';
import { DotContainer } from '@models/container/dot-container.model';
import { DotContentState } from '@dotcms/dotcms-models';
import {
    DotContainerListState,
    DotContainerListStore
} from '@portlets/dot-containers/container-list/store/dot-container-list.store';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    styleUrls: ['./container-list.component.scss'],
    providers: [DotContainerListStore]
})
export class ContainerListComponent implements OnInit, OnDestroy {
    @ViewChild('listing', { static: false })
    vm$ = this.store.vm$;

    listing: DotListingDataTableComponent;
    selectedContainers: DotContainer[] = [];

    containerBulkActions$: Observable<MenuItem[]>;
    tableColumns$: Observable<DataTableColumn[]>;
    actionHeaderOptions$: Observable<ActionHeaderOptions>;
    addToBundleIdentifier$: Observable<string>;

    updateBundleIdentifier = this.store.updateBundleIdentifier;

    private isEnterPrise: boolean;
    private hasEnvironments: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotContainerListStore,
        private dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit(): void {
        this.store.updateTableColumns(this.setContainerColumns());
        this.store.updateContainerBulkActions(this.setContainerBulkActions());

        this.containerBulkActions$ = this.vm$.pipe(
            takeUntil(this.destroy$),
            pluck('containerBulkActions')
        );

        this.tableColumns$ = this.vm$.pipe(takeUntil(this.destroy$), pluck('tableColumns'));

        this.actionHeaderOptions$ = this.vm$.pipe(
            takeUntil(this.destroy$),
            pluck('actionHeaderOptions')
        );

        this.addToBundleIdentifier$ = this.vm$.pipe(
            takeUntil(this.destroy$),
            pluck('addToBundleIdentifier')
        );

        this.vm$
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ selectedContainers }: DotContainerListState) => {
                this.selectedContainers = selectedContainers;
            });

        this.vm$
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ isEnterPrise, hasEnvironments }: DotContainerListState) => {
                this.isEnterPrise = isEnterPrise;
                this.hasEnvironments = hasEnvironments;
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
        this.store.updateActionHeaderOptions({
            primary: {
                command: () => {
                    this.dotRouterService.gotoPortlet(`/container-new/new`);
                }
            }
        });
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
     * get the attributes that define the state of a container.
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

    /**
     * Keep updated the selected containers in the grid
     * @param {DotContainer[]} containers
     *
     * @memberof DotContainerListComponent
     */
    updateSelectedContainers(containers: DotContainer[]): void {
        this.store.updateSelectedContainers(containers);
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
                    this.store.updateBundleIdentifier(
                        this.selectedContainers.map((container) => container.identifier).toString()
                    );
                }
            });
        }

        return bulkOptions;
    }
}
