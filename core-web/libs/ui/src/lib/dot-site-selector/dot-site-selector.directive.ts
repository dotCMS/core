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

import { delay, retryWhen, take, takeUntil, tap } from 'rxjs/operators';

import { DotEventsService, PaginatorService } from '@dotcms/data-access';
import { Site, SiteService } from '@dotcms/dotcms-js';

@Directive({
    selector: '[dotSiteSelector]',
    providers: [PaginatorService],
    standalone: true
})
export class DotSiteSelectorDirective implements OnInit, OnDestroy {
    @Input() archive: boolean;
    @Input() id: string;
    @Input() live: boolean;
    @Input() system: boolean;
    @Input() cssClass: string;
    @Input() width: string;
    @Input() pageSize = 15;
    @Input() asField = false;

    @Output() switch: EventEmitter<Site> = new EventEmitter();
    @Output() hide: EventEmitter<unknown> = new EventEmitter();
    @Output() display: EventEmitter<unknown> = new EventEmitter();

    currentSite: Site;

    sitesCurrentPage: Site[];
    moreThanOneSite = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private siteService: SiteService,
        public paginationService: PaginatorService,
        private dotEventsService: DotEventsService,
        @Optional() @Self() private readonly primeDropdown: Dropdown,
        private readonly changeDetectorRef: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this.paginationService.url = 'v1/site';
        this.paginationService.setExtraParams('archive', this.archive);
        this.paginationService.setExtraParams('live', this.live);
        this.paginationService.setExtraParams('system', this.system);
        this.paginationService.paginationPerPage = this.pageSize;

        this.siteService.refreshSites$
            .pipe(takeUntil(this.destroy$))
            .subscribe((_site: Site) => this.handleSitesRefresh(_site));
        this.getSitesList();
        ['login-as', 'logout-as'].forEach((event: string) => {
            this.dotEventsService
                .listen(event)
                .pipe(takeUntil(this.destroy$))
                .subscribe(() => {
                    this.getSitesList();
                });
        });

        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            setTimeout(() => {
                this.updateCurrentSite(this.siteService.currentSite);
            }, 200);
        });
    }

    /**
     * Manage the sites refresh when a event happen
     * @memberof SiteSelectorComponent
     */
    handleSitesRefresh(site: Site): void {
        this.paginationService
            .getCurrentPage()
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
            .subscribe((items: Site[]) => {
                this.updateValues(items);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setOptions(options: Array<unknown>) {
        this.primeDropdown.options = [...options];
        this.changeDetectorRef.detectChanges();
    }

    /**
     * Updates the current site
     *
     * @param {Site} site
     * @memberof DotSiteSelectorComponent
     */
    updateCurrentSite(site: Site): void {
        this.currentSite = site;
    }

    private updateValues(items: Site[]): void {
        this.sitesCurrentPage = [...items];
        this.moreThanOneSite = items.length > 1;
        this.updateCurrentSite(this.siteService.currentSite);
    }

    /**
     * Call to load a new page.
     * @param string [filter='']
     * @param number [page=1]
     * @memberof SiteSelectorComponent
     */
    getSitesList(filter = '', offset = 0): void {
        this.paginationService.filter = `*${filter}`;
        this.paginationService
            .getWithOffset(offset)
            .pipe(take(1))
            .subscribe((items: Site[]) => {
                this.sitesCurrentPage = [...items];
                this.moreThanOneSite = this.moreThanOneSite || items.length > 1;
            });
    }
}
