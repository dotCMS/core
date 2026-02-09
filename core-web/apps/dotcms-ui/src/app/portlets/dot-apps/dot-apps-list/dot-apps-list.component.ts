import { fromEvent as observableFromEvent, Subject } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, pluck, take, takeUntil } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { DotApp, DotAppsListResolverData } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe, DotNotLicenseComponent } from '@dotcms/ui';

import { DotAppsCardComponent } from './dot-apps-card/dot-apps-card.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
import { DotAppsImportExportDialogComponent } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';

@Component({
    selector: 'dot-apps-list',
    templateUrl: './dot-apps-list.component.html',
    styleUrls: ['./dot-apps-list.component.scss'],
    imports: [
        InputTextModule,
        ButtonModule,
        DotAppsCardComponent,
        DotAppsImportExportDialogComponent,
        DotNotLicenseComponent,
        DotIconComponent,
        DotPortletBaseComponent,
        DotMessagePipe
    ]
})
export class DotAppsListComponent implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);
    private dotAppsService = inject(DotAppsService);

    @ViewChild('searchInput') searchInput: ElementRef;
    @ViewChild('importExportDialog') importExportDialog: DotAppsImportExportDialogComponent;
    apps: DotApp[];
    appsCopy: DotApp[];
    canAccessPortlet: boolean;
    importExportDialogAction: string;
    showDialog = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
     * Opens the Import/Export dialog for all configurations
     *
     * @memberof DotAppsConfigurationComponent
     */
    confirmImportExport(action: string): void {
        this.showDialog = true;
        this.importExportDialogAction = action;
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
     * Checks if export button is disabled based on existing configurations
     *
     * @returns {boolean}
     * @memberof DotAppsListComponent
     */
    isExportButtonDisabled(): boolean {
        return this.apps.filter((app: DotApp) => app.configurationsCount).length > 0;
    }

    /**
     * Reloads data of all apps configuration listing to update the UI
     *
     * @memberof DotAppsListComponent
     */
    reloadAppsData(): void {
        this.dotAppsService
            .get()
            .pipe(take(1))
            .subscribe((apps: DotApp[]) => {
                this.getApps(apps);
            });
    }

    private getApps(apps: DotApp[]): void {
        this.apps = apps;
        this.appsCopy = structuredClone(apps);
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
        this.dotAppsService.get(searchCriteria).subscribe((apps: DotApp[]) => {
            this.appsCopy = apps;
        });
    }
}
