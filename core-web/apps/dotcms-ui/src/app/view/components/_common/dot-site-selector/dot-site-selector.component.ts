import { Subject } from 'rxjs';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { PaginatorService } from '@services/paginator';
import { SearchableDropdownComponent } from '../searchable-dropdown/component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { delay, retryWhen, take, takeUntil, tap } from 'rxjs/operators';

/**
 * It is dropdown of sites, it handle pagination and global search
 *
 * @export
 * @class DotSiteSelectorComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 * @implements {OnDestroy}
 */
@Component({
    providers: [PaginatorService],
    selector: 'dot-site-selector',
    styleUrls: ['./dot-site-selector.component.scss'],
    templateUrl: 'dot-site-selector.component.html'
})
export class DotSiteSelectorComponent implements OnInit, OnChanges, OnDestroy {
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

    @ViewChild('searchableDropdown') searchableDropdown: SearchableDropdownComponent;

    currentSite: Site;

    sitesCurrentPage: Site[];
    moreThanOneSite = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private siteService: SiteService,
        public paginationService: PaginatorService,
        private dotEventsService: DotEventsService
    ) {}

    ngOnInit(): void {
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

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.id && changes.id.currentValue) {
            this.selectCurrentSite(changes.id.currentValue);
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
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

    /**
     * Call when the global serach changed
     * @param any filter
     * @memberof SiteSelectorComponent
     */
    handleFilterChange(filter): void {
        this.getSitesList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof SiteSelectorComponent
     */
    handlePageChange(event): void {
        this.getSitesList(event.filter, event.first);
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

    /**
     * Call when the selected site changed and the switch event is emmited
     * @param Site site
     * @memberof SiteSelectorComponent
     */
    siteChange(site: Site): void {
        this.switch.emit(site);
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

    private getSiteByIdFromCurrentPage(siteId: string): Site {
        return (
            this.sitesCurrentPage &&
            this.sitesCurrentPage.filter((site) => site.identifier === siteId)[0]
        );
    }

    private selectCurrentSite(siteId: string): void {
        const selectedInCurrentPage = this.getSiteByIdFromCurrentPage(siteId);

        if (selectedInCurrentPage) {
            this.updateCurrentSite(selectedInCurrentPage);
        } else {
            this.siteService
                .getSiteById(siteId)
                .pipe(take(1))
                .subscribe((site: Site) => {
                    this.updateCurrentSite(site);
                });
        }
    }

    private updateValues(items: Site[]): void {
        this.sitesCurrentPage = [...items];
        this.moreThanOneSite = items.length > 1;
        this.updateCurrentSite(this.siteService.currentSite);
    }
}
