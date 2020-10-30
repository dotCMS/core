import { Component, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { debounceTime, pluck, takeUntil } from 'rxjs/operators';
import { fromEvent as observableFromEvent, Subject } from 'rxjs';
import { DotApps, DotAppsListResolverData } from '@shared/models/dot-apps/dot-apps.model';
import * as _ from 'lodash';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsExportDialogComponent } from '../dot-apps-export-dialog/dot-apps-export-dialog.component';

@Component({
    selector: 'dot-apps-list',
    templateUrl: './dot-apps-list.component.html',
    styleUrls: ['./dot-apps-list.component.scss']
})
export class DotAppsListComponent implements OnInit, OnDestroy {
    @ViewChild('searchInput') searchInput: ElementRef;
    @ViewChild('exportDialog') exportDialog: DotAppsExportDialogComponent;
    apps: DotApps[];
    appsCopy: DotApps[];
    canAccessPortlet: boolean;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotRouterService: DotRouterService,
        private dotAppsService: DotAppsService
    ) {}

    ngOnInit() {
        this.route.data
            .pipe(pluck('dotAppsListResolverData'), takeUntil(this.destroy$))
            .subscribe((resolverData: DotAppsListResolverData) => {
                if (resolverData.isEnterpriseLicense) {
                    this.getApps(resolverData.apps);
                }
                this.canAccessPortlet = resolverData.isEnterpriseLicense;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Redirects to apps configuration listing page
     *
     * @param string key
     * @memberof DotAppsListComponent
     */
    goToApp(key: string): void {
        this.dotRouterService.goToAppsConfiguration(key);
    }

    /**
     * Opens the Export dialog for all configurations
     *
     * @memberof DotAppsConfigurationComponent
     */
    confirmExport(): void {
        this.exportDialog.showExportDialog = true;
    }

    /**
     * Checks if export button is disabled based on existing configurations
     *
     * @returns {boolean}
     * @memberof DotAppsListComponent
     */
    isExportButtonDisabled(): boolean {
        return this.apps.filter((app: DotApps) => app.configurationsCount).length > 0;
    }

    private getApps(apps: DotApps[]): void {
        this.apps = apps;
        this.appsCopy = _.cloneDeep(apps);
        setTimeout(() => {
            this.attachFilterEvents();
        }, 0);
    }

    private attachFilterEvents(): void {
        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterApps(keyboardEvent.target['value']);
            });

        this.searchInput.nativeElement.focus();
    }

    private filterApps(searchCriteria?: string): void {
        this.dotAppsService.get(searchCriteria).subscribe((apps: DotApps[]) => {
            this.appsCopy = apps;
        });
    }
}
