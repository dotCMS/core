import { Subject } from 'rxjs';

import {
    ChangeDetectorRef,
    Directive,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Output,
    Self
} from '@angular/core';

import { Dropdown } from 'primeng/dropdown';

import { debounceTime, delay, retryWhen, take, takeUntil, tap } from 'rxjs/operators';

import { DotEventsService, PaginatorService, DotSiteService } from '@dotcms/data-access';
import { Site, SiteService } from '@dotcms/dotcms-js';

@Directive({
    selector: '[dotSiteSelector]',
    providers: [PaginatorService],
    standalone: true
})
export class DotSiteSelectorDirective implements OnInit, OnDestroy {
    @Input() archive = false;
    @Input() live = true;
    @Input() system = true;
    @Input() pageSize = 10;

    @Output() switchSite: EventEmitter<Site> = new EventEmitter();

    protected currentSite: Site;
    private readonly destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly dotEvents = ['login-as', 'logout-as'];
    private readonly control: Dropdown;

    constructor(
        @Optional() @Self() private readonly primeDropdown: Dropdown,
        private readonly dotEventsService: DotEventsService,
        private readonly dotSiteService: DotSiteService,
        private readonly cd: ChangeDetectorRef,
        private readonly siteService: SiteService
    ) {
        this.control = this.primeDropdown;

        if (this.control) {
            this.control.onFilter.pipe(debounceTime(225)).subscribe((event) => {
                this.getSitesList(event.filter);
            });
        } else {
            console.warn('ContainerOptionsDirective is for use with PrimeNg Dropdown');
        }
    }

    ngOnInit() {
        this.getSitesList();

        this.siteService.refreshSites$
            .pipe(takeUntil(this.destroy$))
            .subscribe((site: Site) => this.handleSitesRefresh(site));

        this.dotEvents.forEach((event: string) => {
            this.dotEventsService
                .listen(event)
                .pipe(takeUntil(this.destroy$))
                .subscribe(() => this.getSitesList());
        });

        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.updateCurrentSite(this.siteService.currentSite);
        });
    }

    /**
     * Manage the sites refresh when a event happen
     * @memberof SiteSelectorComponent
     */
    handleSitesRefresh(site: Site): void {
        this.dotSiteService
            .getSites()
            .pipe(
                take(1),
                tap((items: Site[]) => {
                    const siteIndex = items.findIndex(
                        (item: Site) => site.identifier === item.identifier
                    );

                    const shouldRetry = site.archived ? siteIndex >= 0 : siteIndex === -1;

                    if (shouldRetry) {
                        throw new Error('Indexing... site still present');
                    }
                }),
                retryWhen((error) => error.pipe(delay(1000), take(10)))
            )
            .subscribe((items: Site[]) => this.updateOptions(items));
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Set the options of the dropdown
     * @param options
     */
    private setOptions(options: Array<Site>): void {
        this.primeDropdown.options = [...options];
        this.cd.detectChanges();
    }

    /**
     * Updates the current site
     *
     * @param {Site} site
     * @memberof DotSiteSelectorComponent
     */
    private updateCurrentSite(site: Site): void {
        this.currentSite = site;
        this.switchSite.emit(site);
        this.cd.detectChanges();
    }

    /**
     * Updates the options and the current site
     *
     * @private
     * @param {Site[]} items
     * @memberof DotSiteSelectorDirective
     */
    private updateOptions(items: Site[]): void {
        this.setOptions(items);
        this.updateCurrentSite(this.siteService.currentSite);
    }

    /**
     * Call to load a new page.
     * @param string [filter='']
     * @memberof SiteSelectorComponent
     */
    private getSitesList(filter = ''): void {
        this.dotSiteService
            .getSites(filter, this.pageSize)
            .pipe(take(1))
            .subscribe((items: Site[]) => this.setOptions(items));
    }
}
