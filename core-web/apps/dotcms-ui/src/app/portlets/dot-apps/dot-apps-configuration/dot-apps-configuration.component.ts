import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { dialogAction, DotApps, DotAppsSites } from '@shared/models/dot-apps/dot-apps.model';
import { ActivatedRoute } from '@angular/router';
import { pluck, take, debounceTime, takeUntil } from 'rxjs/operators';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { LazyLoadEvent } from 'primeng/api';
import { PaginatorService } from '@services/paginator';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotAppsImportExportDialogComponent } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';

@Component({
    selector: 'dot-apps-configuration',
    templateUrl: './dot-apps-configuration.component.html',
    styleUrls: ['./dot-apps-configuration.component.scss']
})
export class DotAppsConfigurationComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;
    @ViewChild('importExportDialog') importExportDialog: DotAppsImportExportDialogComponent;
    apps: DotApps;
    siteSelected: DotAppsSites;
    importExportDialogAction = dialogAction.EXPORT;
    showDialog = false;

    hideLoadDataButton: boolean;
    paginationPerPage = 40;
    totalRecords: number;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotAppsService: DotAppsService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        public paginationService: PaginatorService
    ) {}

    ngOnInit() {
        this.route.data.pipe(pluck('data'), take(1)).subscribe((app: DotApps) => {
            this.apps = app;
            this.apps.sites = [];
        });

        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterConfigurations(keyboardEvent.target['value']);
            });

        this.paginationService.url = `v1/apps/${this.apps.key}`;
        this.paginationService.paginationPerPage = this.paginationPerPage;
        this.paginationService.sortField = 'name';
        this.paginationService.setExtraParams('filter', '');
        this.paginationService.sortOrder = 1;
        this.loadData();

        this.searchInput.nativeElement.focus();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Loads data through pagination service
     *
     * @param LazyLoadEvent event
     * @memberof DotAppsConfigurationComponent
     */
    loadData(event?: LazyLoadEvent): void {
        this.paginationService
            .getWithOffset((event && event.first) || 0)
            .pipe(take(1))
            .subscribe((apps: DotApps[]) => {
                const app = [].concat(apps)[0];
                this.apps.sites = event ? this.apps.sites.concat(app.sites) : app.sites;
                this.apps.configurationsCount = app.configurationsCount;
                this.totalRecords = this.paginationService.totalRecords;
                this.hideLoadDataButton = !this.isThereMoreData(this.apps.sites.length);
            });
    }

    /**
     * Redirects to create/edit configuration site page
     *
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationComponent
     */
    gotoConfiguration(site: DotAppsSites): void {
        this.dotRouterService.goToUpdateAppsConfiguration(this.apps.key, site);
    }

    /**
     * Updates dialog show/hide state
     *
     * @memberof DotAppsConfigurationComponent
     */
    onClosedDialog(): void {
        this.showDialog = false;
    }

    /**
     * Redirects to app configuration listing page
     *
     * @param string key
     * @memberof DotAppsConfigurationComponent
     */
    goToApps(key: string): void {
        this.dotRouterService.gotoPortlet(`/apps/${key}`);
    }

    /**
     * Opens the dialog and set Export actions based on a single/all sites
     *
     * @param DotAppsSites [site]
     * @memberof DotAppsConfigurationComponent
     */
    confirmExport(site?: DotAppsSites): void {
        this.importExportDialog.show = true;
        this.siteSelected = site;
    }

    /**
     * Display confirmation dialog to delete a specific configuration
     *
     * @param DotAppsSites site
     * @memberof DotAppsConfigurationComponent
     */
    deleteConfiguration(site: DotAppsSites): void {
        this.dotAppsService
            .deleteConfiguration(this.apps.key, site.id)
            .pipe(take(1))
            .subscribe(() => {
                this.apps.sites = [];
                this.loadData();
            });
    }

    /**
     * Display confirmation dialog to delete all configurations
     *
     * @memberof DotAppsConfigurationComponent
     */
    deleteAllConfigurations(): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotAppsService
                    .deleteAllConfigurations(this.apps.key)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.apps.sites = [];
                        this.loadData();
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('apps.confirmation.title'),
            message: this.dotMessageService.get('apps.confirmation.delete.all.message'),
            footerLabel: {
                accept: this.dotMessageService.get('apps.confirmation.accept')
            }
        });
    }

    private isThereMoreData(index: number): boolean {
        return this.totalRecords / index > 1;
    }

    private filterConfigurations(searchCriteria?: string): void {
        this.paginationService.setExtraParams('filter', searchCriteria);
        this.loadData();
    }
}
