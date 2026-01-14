import { fromEvent as observableFromEvent, Subject } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, pluck, take, takeUntil } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotAppsService,
    DotMessageService,
    DotRouterService,
    PaginatorService
} from '@dotcms/data-access';
import { DotApp, DotAppsSite } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list/dot-apps-configuration-list.component';

import { DotAppsImportExportDialogStore } from '../../dot-apps-import-export-dialog/store/dot-apps-import-export-dialog.store';
import { DotAppsConfigurationHeaderComponent } from '../dot-apps-configuration-detail/components/dot-apps-configuration-header/dot-apps-configuration-header.component';

@Component({
    selector: 'dot-apps-configuration',
    templateUrl: './dot-apps-configuration.component.html',
    styleUrls: ['./dot-apps-configuration.component.scss'],
    imports: [
        InputTextModule,
        ButtonModule,
        DotAppsConfigurationHeaderComponent,
        DotAppsConfigurationListComponent,
        DotMessagePipe
    ]
})
export class DotAppsConfigurationComponent implements OnInit, OnDestroy {
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);
    readonly #dotAppsService = inject(DotAppsService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #route = inject(ActivatedRoute);
    readonly #dialogStore = inject(DotAppsImportExportDialogStore);

    paginationService = inject(PaginatorService);

    @ViewChild('searchInput', { static: true }) searchInput: ElementRef;

    apps: DotApp;
    hideLoadDataButton: boolean;
    paginationPerPage = 40;
    totalRecords: number;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        this.#route.data.pipe(pluck('data'), take(1)).subscribe((app: DotApp) => {
            this.apps = app;
            this.apps.sites = [];
        });

        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterConfigurations((keyboardEvent.target as HTMLInputElement).value);
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
     */
    loadData(event?: LazyLoadEvent): void {
        this.paginationService
            .getWithOffset((event && event.first) || 0)
            .pipe(take(1))
            .subscribe((apps: DotApp[]) => {
                const app = ([] as DotApp[]).concat(apps)[0];
                this.apps.sites = event ? this.apps.sites.concat(app.sites) : app.sites;
                this.apps.configurationsCount = app.configurationsCount;
                this.totalRecords = this.paginationService.totalRecords;
                this.hideLoadDataButton = !this.isThereMoreData(this.apps.sites.length);
            });
    }

    /**
     * Redirects to create/edit configuration site page
     */
    gotoConfiguration(site: DotAppsSite): void {
        this.#dotRouterService.goToUpdateAppsConfiguration(this.apps.key, site);
    }

    /**
     * Redirects to app configuration listing page
     */
    goToApps(key: string): void {
        this.#dotRouterService.gotoPortlet(`/apps/${key}`);
    }

    /**
     * Opens the export dialog
     */
    openExportDialog(site?: DotAppsSite): void {
        this.#dialogStore.openExport(this.apps, site);
    }

    /**
     * Delete a specific configuration
     */
    deleteConfiguration(site: DotAppsSite): void {
        this.#dotAppsService
            .deleteConfiguration(this.apps.key, site.id)
            .pipe(take(1))
            .subscribe(() => {
                this.apps.sites = [];
                this.loadData();
            });
    }

    /**
     * Display confirmation dialog to delete all configurations
     */
    deleteAllConfigurations(): void {
        this.#dotAlertConfirmService.confirm({
            accept: () => {
                this.#dotAppsService
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
            header: this.#dotMessageService.get('apps.confirmation.title'),
            message: this.#dotMessageService.get('apps.confirmation.delete.all.message'),
            footerLabel: {
                accept: this.#dotMessageService.get('apps.confirmation.accept')
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
