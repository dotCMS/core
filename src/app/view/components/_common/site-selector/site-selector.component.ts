import { Subscription } from 'rxjs/Subscription';
import {
    Component,
    ViewEncapsulation,
    ViewChild,
    Output,
    EventEmitter,
    Input,
    OnInit,
    SimpleChanges,
    OnChanges,
    OnDestroy
} from '@angular/core';
import { Site, SiteService } from 'dotcms-js/dotcms-js';
import { PaginatorService } from '../../../../api/services/paginator';
import { SearchableDropdownComponent } from '../searchable-dropdown/component';
import { Observable } from 'rxjs/Observable';

/**
 * It is dropdown of sites, it handle pagination and global search
 *
 * @export
 * @class SiteSelectorComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 * @implements {OnDestroy}
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [
        PaginatorService,
        SearchableDropdownComponent
    ],
    selector: 'dot-site-selector',
    styleUrls: ['./site-selector.component.scss'],
    templateUrl: 'site-selector.component.html'
})
export class SiteSelectorComponent implements OnInit, OnChanges, OnDestroy {
    @Input() archive: boolean;
    @Input() id: string;
    @Input() live: boolean;
    @Input() system: boolean;

    @Output() change: EventEmitter<Site> = new EventEmitter();
    @Output() hide: EventEmitter<any> = new EventEmitter();
    @Output() show: EventEmitter<any> = new EventEmitter();

    @ViewChild('searchableDropdown') searchableDropdown: SearchableDropdownComponent;

    currentSite: Observable<Site>;
    sitesCurrentPage: Site[];
    totalRecords: number;

    private refreshSitesSub: Subscription;

    constructor(private siteService: SiteService, public paginationService: PaginatorService) {}

    propagateChange = (_: any) => {};

    ngOnInit(): void {
        this.paginationService.url = 'v1/site';

        this.paginationService.addExtraParams('archive', this.archive);
        this.paginationService.addExtraParams('live', this.live);
        this.paginationService.addExtraParams('system', this.system);

        this.refreshSitesSub = this.siteService.refreshSites$.subscribe((_site: Site) => this.handleSitesRefresh());

        this.getSitesList();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.id && changes.id.currentValue) {
            this.selectCurrentSite(changes.id.currentValue);
        }
    }

    ngOnDestroy(): void {
        this.refreshSitesSub.unsubscribe();
    }

    /**
     * Manage the sites refresh when a event happen
     * @memberof SiteSelectorComponent
     */
    handleSitesRefresh(): void {
        this.paginationService.getCurrentPage().subscribe((items) => {
            this.sitesCurrentPage = [...items];
            this.totalRecords = this.paginationService.totalRecords;
            this.currentSite = Observable.of(this.siteService.currentSite);
        });
    }

    /**
     * Call when the global serach changed
     * @param {any} filter
     * @memberof SiteSelectorComponent
     */
    handleFilterChange(filter): void {
        this.getSitesList(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof SiteSelectorComponent
     */
    handlePageChange(event): void {
        this.getSitesList(event.filter, event.first);
    }

    /**
     * Call to load a new page.
     * @param {string} [filter='']
     * @param {number} [page=1]
     * @memberof SiteSelectorComponent
     */
    getSitesList(filter = '', offset = 0): void {
        // Set filter if undefined
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe((items) => {
            this.sitesCurrentPage = [...items];
            this.totalRecords = this.totalRecords || this.paginationService.totalRecords;

            if (!this.currentSite) {
                this.setCurrentSiteAsDefault();
            }
        });
    }

    /**
     * Call when the selected site changed and the change event is emmited
     * @param {Site} site
     * @memberof SiteSelectorComponent
     */
    siteChange(site: Site): void {
        this.change.emit(site);
    }

    private getSiteByIdFromCurrentPage(siteId: string): Site {
        return this.sitesCurrentPage && this.sitesCurrentPage.filter((site) => site.identifier === siteId)[0];
    }

    private selectCurrentSite(siteId: string): void {
        const selectedInCurrentPage = this.getSiteByIdFromCurrentPage(siteId);
        this.currentSite = selectedInCurrentPage ? Observable.of(selectedInCurrentPage) : this.siteService.getSiteById(siteId);
    }

    private setCurrentSiteAsDefault() {
        this.currentSite = Observable.of(this.siteService.currentSite);
    }
}
