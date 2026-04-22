import { patchState, signalState } from '@ngrx/signals';
import { fromEvent as observableFromEvent } from 'rxjs';

import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    inject,
    viewChild
} from '@angular/core';
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
import { DotAppsImportExportDialogStore } from '../dot-apps-import-export-dialog/store/dot-apps-import-export-dialog.store';

interface DotAppsListState {
    allApps: DotApp[];
    displayedApps: DotApp[];
}

/**
 * App keys whose config is edited through a dedicated portlet rather than the
 * generic Apps UI. The cards are hidden from the grid; navigation to
 * `/apps/<key>` is separately redirected by {@link dotAppsSamlRedirectGuard}.
 */
const HIDDEN_APP_KEYS: ReadonlySet<string> = new Set(['dotsaml-config']);

const withoutHiddenApps = (apps: DotApp[]): DotApp[] =>
    apps.filter((app) => !HIDDEN_APP_KEYS.has(app.key));

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
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
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
        // For export all, we don't pass an app — the store handles null to mean "all apps".
        this.#dialogStore.openExport(null);
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
        const visible = withoutHiddenApps(apps);
        patchState(this.state, {
            allApps: visible,
            displayedApps: visible
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
        this.#dotAppsService
            .get(searchCriteria)
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((apps: DotApp[]) => {
                patchState(this.state, {
                    displayedApps: withoutHiddenApps(apps)
                });
            });
    }
}
