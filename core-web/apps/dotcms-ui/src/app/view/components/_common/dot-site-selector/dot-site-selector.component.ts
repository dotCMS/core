import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { toSignal } from '@angular/core/rxjs-interop';
import { delay, map, retryWhen, scan, take, tap } from 'rxjs/operators';

import { DotEventsService, PaginatorService } from '@dotcms/data-access';
import { Site, SiteService, LoggerService } from '@dotcms/dotcms-js';
import { SiteEntity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { Select, SelectModule } from 'primeng/select';

/**
 * It is dropdown of sites, it handles global search using p-select native filtering
 *
 * @export
 * @class DotSiteSelectorComponent
 */
@Component({
    providers: [PaginatorService],
    selector: 'dot-site-selector',
    styleUrls: ['./dot-site-selector.component.scss'],
    templateUrl: 'dot-site-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, SelectModule]
})
export class DotSiteSelectorComponent {
    #globalStore = inject(GlobalStore);
    #siteService = inject(SiteService);
    #paginationService = inject(PaginatorService);
    #dotEventsService = inject(DotEventsService);
    #loggerService = inject(LoggerService);

    // Signal inputs
    archive = input<boolean>();
    id = input<string>();
    live = input<boolean>();
    system = input<boolean>();
    cssClass = input<string>();
    width = input<string>();
    asField = input<boolean>(false);

    // Signal outputs
    switch = output<Site>();
    hide = output<void>();
    display = output<void>();

    @ViewChild('select') select: Select;

    // Internal state signals
    $sitesList = signal<Site[]>([]);
    #userSelectedSite = signal<Site | null>(null);

    // Computed signals
    $moreThanOneSite = computed(() => this.$sitesList().length > 1);

    // Computed: Determine which site should be selected based on priority:
    // 1. User selected site
    // 2. ID input
    // 3. Service site
    $targetSite = computed(() => {
        const sitesList = this.$sitesList();
        const userSelected = this.#userSelectedSite();
        const siteId = this.id();
        const serviceSite = this.$currentSiteFromService();

        // Priority 1: User selection
        if (userSelected) {
            return this.#findSiteInList(userSelected.identifier, sitesList) || userSelected;
        }

        // Priority 2: ID input
        if (siteId && sitesList.length > 0) {
            return this.#findSiteInList(siteId, sitesList);
        }

        // Priority 3: Service site (only if list is loaded)
        if (serviceSite && sitesList.length > 0) {
            return this.#findSiteInList(serviceSite.identifier, sitesList);
        }

        return null;
    });

    // Current site signal - synced with target site
    $currentSite = signal<Site | null>(null);

    // Virtual scroll options: show max 15 items at a time
    $virtualScrollOptions = signal({
        scrollHeight: '300px',
        autoSize: true
    });

    // Convert observables to signals
    $currentSiteFromService = toSignal(this.#siteService.currentSite$, {
        initialValue: null
    });

    constructor() {
        // Initialize pagination service
        this.#paginationService.url = 'v1/site';
        this.#paginationService.paginationPerPage = 1000;

        // Effect: Update pagination params when inputs change
        effect(() => {
            const archive = this.archive();
            const live = this.live();
            const system = this.system();

            if (archive !== undefined) {
                this.#paginationService.setExtraParams('archive', archive);
            }
            if (live !== undefined) {
                this.#paginationService.setExtraParams('live', live);
            }
            if (system !== undefined) {
                this.#paginationService.setExtraParams('system', system);
            }
        });

        // Effect: Sync current site with target site (computed)
        effect(() => {
            const targetSite = this.$targetSite();
            const currentSite = this.$currentSite();

            if (targetSite && currentSite?.identifier !== targetSite.identifier) {
                this.$currentSite.set(targetSite);
            }

            // Clear user selection flag if it matches service site
            const userSelected = this.#userSelectedSite();
            const serviceSite = this.$currentSiteFromService();
            if (userSelected?.identifier === serviceSite?.identifier) {
                this.#userSelectedSite.set(null);
            }
        });

        // Load sites on init
        this.loadAllSites();

        // Handle site refresh events
        this.#siteService.refreshSites$.subscribe(() => {
            this.loadAllSites();
        });

        // Handle login-as and logout-as events
        ['login-as', 'logout-as'].forEach((event: string) => {
            this.#dotEventsService.listen(event).subscribe(() => {
                this.loadAllSites();
            });
        });
    }

    /**
     * Call when the select panel is shown
     * Resets the virtual scroller state to ensure options are displayed correctly
     * @memberof DotSiteSelectorComponent
     */
    onSelectShow(): void {
        this.display.emit();
        // Reset virtual scroller state to fix initial display issue
        requestAnimationFrame(() => {
            if (this.select?.scroller) {
                this.select.scroller.setInitialState();
            }
        });
    }

    /**
     * Call when a site is selected
     * @param event change event from p-select
     * @memberof DotSiteSelectorComponent
     */
    onSiteChange(event: { value: Site }): void {
        if (event.value) {
            this.siteChange(event.value);
        }
    }

    /**
     * Load all sites without pagination. p-select will handle client-side filtering.
     * @memberof DotSiteSelectorComponent
     */
    loadAllSites(): void {
        this.#paginationService.filter = '*';
        this.#paginationService
            .getWithOffset(0)
            .pipe(take(1))
            .subscribe((items: Site[]) => {
                this.$sitesList.set(items);
                // Handle ID input if site not found in list
                this.#handleMissingSiteId();
            });
    }

    /**
     * Call when the selected site changed and the switch event is emitted
     * @param {Site} site
     * @memberof DotSiteSelectorComponent
     */
    siteChange(site: Site): void {
        // Mark as user-selected (computed will use this)
        this.#userSelectedSite.set(site);
        // Update global store
        this.#globalStore.setCurrentSite(site as unknown as SiteEntity);
        // Emit event
        this.switch.emit(site);
    }

    /**
     * Updates the current site (called from template ngModelChange)
     * @param {Site} site
     * @memberof DotSiteSelectorComponent
     */
    updateCurrentSite(site: Site): void {
        // This is called from template, treat as user selection
        this.siteChange(site);
    }

    /**
     * Find site in list by identifier
     * @private
     */
    #findSiteInList(siteId: string, sitesList: Site[]): Site | undefined {
        return sitesList.find((site) => site.identifier === siteId);
    }

    /**
     * Handle ID input when site is not found in loaded list
     * Site may be new or refreshed and not yet indexed
     * Retries loading the list with delay until site appears
     * @private
     */
    #handleMissingSiteId(): void {
        const siteId = this.id();
        const sitesList = this.$sitesList();

        if (!siteId || this.#findSiteInList(siteId, sitesList)) {
            return;
        }

        this.#loggerService.warn(
            `Site with ID ${siteId} not found in list. It may be indexing. Retrying...`
        );

        // Retry loading sites list until the site appears (max 10 attempts, 1 second delay)
        this.#paginationService
            .getWithOffset(0)
            .pipe(
                take(1),
                map((items: Site[]) => {
                    const siteFound = items.some((site) => site.identifier === siteId);
                    if (!siteFound) {
                        throw new Error('Site still not found in list');
                    }
                    return items;
                }),
                retryWhen((errors) =>
                    errors.pipe(
                        scan((count) => count + 1, 0),
                        delay(1000),
                        tap((count) => {
                            if (count === 10) {
                                this.#loggerService.warn(
                                    `Site ${siteId} not found after 10 retries. It may not be indexed yet.`
                                );
                            }
                        }),
                        take(10)
                    )
                )
            )
            .subscribe((items: Site[]) => {
                this.$sitesList.set(items);
            });
    }
}
