import { patchState, signalState } from '@ngrx/signals';
import { fromEvent as observableFromEvent } from 'rxjs';

import { Component, DestroyRef, ElementRef, AfterViewInit, inject, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime, map, take } from 'rxjs/operators';

import { DotAppsService, DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAppsCardComponent } from './dot-apps-card/dot-apps-card.component';

import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';
import { DotAppsImportExportDialogComponent } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';

interface DotAppsListState {
    allApps: DotApp[];
    displayedApps: DotApp[];
    canAccessPortlet: boolean;
    importExportDialogAction: string;
    showDialog: boolean;
}

@Component({
    selector: 'dot-apps-list',
    templateUrl: './dot-apps-list.component.html',
    styleUrls: ['./dot-apps-list.component.scss'],
    imports: [
        InputTextModule,
        ButtonModule,
        DotAppsCardComponent,
        DotAppsImportExportDialogComponent,

        DotPortletBaseComponent,
        DotMessagePipe
    ]
})
export class DotAppsListComponent implements AfterViewInit {
    private route = inject(ActivatedRoute);
    private dotRouterService = inject(DotRouterService);
    private dotAppsService = inject(DotAppsService);
    private destroyRef = inject(DestroyRef);

    searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
    importExportDialog = viewChild<DotAppsImportExportDialogComponent>('importExportDialog');

    state = signalState<DotAppsListState>({
        allApps: [],
        displayedApps: [],
        canAccessPortlet: false,
        importExportDialogAction: '',
        showDialog: false
    });

    ngAfterViewInit(): void {
        this.route.data
            .pipe(
                map((data) => data['dotAppsListResolverData']),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((apps: DotApp[]) => {
                this.initAppsState(apps);
            });
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
        patchState(this.state, {
            importExportDialogAction: action,
            showDialog: true
        });
    }

    /**
     * Updates dialog show/hide state
     *
     * @memberof DotAppsConfigurationComponent
     */
    onClosedDialog(): void {
        patchState(this.state, {
            showDialog: false
        });
    }

    /**
     * Checks if export button is disabled based on existing configurations
     *
     * @returns {boolean}
     * @memberof DotAppsListComponent
     */
    isExportButtonDisabled(): boolean {
        return this.state.allApps().filter((app: DotApp) => app.configurationsCount).length > 0;
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
                this.initAppsState(apps);
            });
    }

    private initAppsState(apps: DotApp[]): void {
        patchState(this.state, {
            allApps: apps,
            displayedApps: apps
        });

        this.attachFilterEvents();
    }

    private attachFilterEvents(): void {
        observableFromEvent(this.searchInput().nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntilDestroyed(this.destroyRef))
            .subscribe((keyboardEvent: Event) => {
                this.filterApps(keyboardEvent.target['value']);
            });

        this.searchInput().nativeElement.focus();
    }

    private filterApps(searchCriteria?: string): void {
        this.dotAppsService.get(searchCriteria).subscribe((apps: DotApp[]) => {
            patchState(this.state, {
                displayedApps: apps
            });
        });
    }
}
