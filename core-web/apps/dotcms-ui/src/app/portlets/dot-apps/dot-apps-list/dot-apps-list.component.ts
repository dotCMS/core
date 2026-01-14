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
import { DotAppsImportExportDialogStore } from '../dot-apps-import-export-dialog/store/dot-apps-import-export-dialog.store';

interface DotAppsListState {
    allApps: DotApp[];
    displayedApps: DotApp[];
}

@Component({
    selector: 'dot-apps-list',
    templateUrl: './dot-apps-list.component.html',
    styleUrls: ['./dot-apps-list.component.scss'],
    imports: [
        InputTextModule,
        ButtonModule,
        DotAppsCardComponent,
        DotPortletBaseComponent,
        DotMessagePipe
    ]
})
export class DotAppsListComponent implements AfterViewInit {
    readonly #route = inject(ActivatedRoute);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotAppsService = inject(DotAppsService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dialogStore = inject(DotAppsImportExportDialogStore);

    readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');

    readonly state = signalState<DotAppsListState>({
        allApps: [],
        displayedApps: []
    });

    constructor() {
        // Subscribe to import success to reload apps data
        this.#dialogStore.importSuccess$
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => this.reloadAppsData());
    }

    ngAfterViewInit(): void {
        this.#route.data
            .pipe(
                map((data) => data['dotAppsListResolverData']),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((apps: DotApp[]) => {
                this.initAppsState(apps);
            });
    }

    /**
     * Redirects to apps configuration listing page
     */
    goToApp(key: string): void {
        this.#dotRouterService.goToAppsConfiguration(key);
    }

    /**
     * Opens the Import dialog
     */
    openImportDialog(): void {
        this.#dialogStore.openImport();
    }

    /**
     * Opens the Export dialog for all configurations
     */
    openExportDialog(): void {
        // For export all, we don't pass an app - the store handles this
        this.#dialogStore.openExport(null as unknown as DotApp);
    }

    /**
     * Checks if export button is disabled based on existing configurations
     */
    isExportButtonDisabled(): boolean {
        return this.state.allApps().filter((app: DotApp) => app.configurationsCount).length > 0;
    }

    /**
     * Reloads data of all apps configuration listing to update the UI
     */
    reloadAppsData(): void {
        this.#dotAppsService
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
        const searchInputEl = this.searchInput();
        if (!searchInputEl) return;

        observableFromEvent(searchInputEl.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntilDestroyed(this.#destroyRef))
            .subscribe((keyboardEvent: Event) => {
                this.filterApps((keyboardEvent.target as HTMLInputElement).value);
            });

        searchInputEl.nativeElement.focus();
    }

    private filterApps(searchCriteria?: string): void {
        this.#dotAppsService.get(searchCriteria).subscribe((apps: DotApp[]) => {
            patchState(this.state, {
                displayedApps: apps
            });
        });
    }
}
