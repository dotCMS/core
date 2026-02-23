import { patchState, signalState } from '@ngrx/signals';
import { fromEvent as observableFromEvent } from 'rxjs';

import {
    AfterViewInit,
    Component,
    DestroyRef,
    ElementRef,
    OnInit,
    computed,
    inject,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, pluck, take } from 'rxjs/operators';

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

import { DotAppsImportExportDialogComponent } from '../../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';
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
        DotAppsImportExportDialogComponent,
        DotMessagePipe
    ]
})
export class DotAppsConfigurationComponent implements OnInit, AfterViewInit {
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);
    readonly #dotAppsService = inject(DotAppsService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #route = inject(ActivatedRoute);
    readonly #dialogStore = inject(DotAppsImportExportDialogStore);
    readonly #destroyRef = inject(DestroyRef);
    paginationService = inject(PaginatorService);

    $searchInputElement = viewChild<ElementRef<HTMLInputElement>>('searchInput');

    $state = signalState({
        app: null,
        paginationPerPage: 40,
        totalRecords: 0
    });

    readonly $app = computed(() => this.$state().app);
    readonly $paginationPerPage = computed(() => this.$state().paginationPerPage);
    readonly $totalRecords = computed(() => this.$state().totalRecords);

    readonly $showMoreData = computed(() => {
        const app = this.$app();
        if (!app?.sites?.length) {
            return false;
        }

        return this.$totalRecords() / app.sites.length > 1;
    });

    ngOnInit() {
        this.#route.data.pipe(pluck('data'), take(1)).subscribe((app: DotApp) => {
            patchState(this.$state, {
                app: {
                    ...app,
                    sites: []
                }
            });

            // Initialize pagination after app data is available
            this.paginationService.url = `v1/apps/${app.key}`;
            this.paginationService.paginationPerPage = this.$paginationPerPage();
            this.paginationService.sortField = 'name';
            this.paginationService.setExtraParams('filter', '');
            this.paginationService.sortOrder = 1;
            this.loadData();
        });
    }

    ngAfterViewInit() {
        const searchInput = this.$searchInputElement();
        if (searchInput) {
            observableFromEvent(searchInput.nativeElement, 'keyup')
                .pipe(debounceTime(500), takeUntilDestroyed(this.#destroyRef))
                .subscribe((keyboardEvent: Event) => {
                    this.filterConfigurations((keyboardEvent.target as HTMLInputElement).value);
                });

            searchInput.nativeElement.focus();
        }
    }

    /**
     * Loads data through pagination service
     */
    loadData(event?: LazyLoadEvent): void {
        this.paginationService
            .getWithOffset((event && event.first) || 0)
            .pipe(take(1))
            .subscribe((app: DotApp) => {
                patchState(this.$state, {
                    app: {
                        ...app,
                        sites: event ? this.$state().app.sites.concat(app.sites) : app.sites,
                        configurationsCount: app.configurationsCount
                    },
                    totalRecords: this.paginationService.totalRecords
                });
            });
    }

    /**
     * Redirects to create/edit configuration site page
     */
    gotoConfiguration(site: DotAppsSite): void {
        this.#dotRouterService.goToUpdateAppsConfiguration(this.$app().key, site);
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
        this.#dialogStore.openExport(this.$app(), site);
    }

    /**
     * Delete a specific configuration
     */
    deleteConfiguration(site: DotAppsSite): void {
        this.#dotAppsService
            .deleteConfiguration(this.$app().key, site.id)
            .pipe(take(1))
            .subscribe(() => {
                patchState(this.$state, {
                    app: {
                        ...this.$app(),
                        sites: []
                    }
                });
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
                    .deleteAllConfigurations(this.$app().key)
                    .pipe(take(1))
                    .subscribe(() => {
                        patchState(this.$state, {
                            app: {
                                ...this.$app(),
                                sites: []
                            }
                        });
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

    private filterConfigurations(searchCriteria?: string): void {
        this.paginationService.setExtraParams('filter', searchCriteria);
        this.loadData();
    }
}
