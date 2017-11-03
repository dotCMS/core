import {
    Component,
    ViewEncapsulation,
    ViewChild,
    forwardRef,
    Output,
    EventEmitter,
    Input,
    OnInit,
    AfterViewInit,
    AfterContentInit,
    SimpleChanges,
    OnChanges
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Site, SiteService } from 'dotcms-js/dotcms-js';
import { PaginatorService } from '../../../../api/services/paginator';
import { SearchableDropdownComponent } from '../searchable-dropdown/component';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

/**
 * It is dropdown of sites, it handle pagination and global search
 * @export
 * @class SiteSelectorComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [
        PaginatorService,
        SearchableDropdownComponent,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SiteSelectorComponent)
        }
    ],
    selector: 'dot-site-selector',
    styleUrls: ['./site-selector.component.scss'],
    templateUrl: 'site-selector.component.html'
})
export class SiteSelectorComponent implements OnInit, OnChanges {
    private static readonly MIN_CHARECTERS_TO_SERACH = 3;

    @Input() archive: boolean;
    @Input() id: string;
    @Input() live: boolean;
    @Input() system = true;
    @Output() change: EventEmitter<Site> = new EventEmitter();
    @Output() hide: EventEmitter<any> = new EventEmitter();
    @Output() show: EventEmitter<any> = new EventEmitter();
    @ViewChild('searchableDropdown') searchableDropdown: SearchableDropdownComponent;

    currentSite: Observable<Site>;
    totalRecords: number;
    sitesCurrentPage: Site[];

    constructor(private siteService: SiteService, public paginationService: PaginatorService) {}

    propagateChange = (_: any) => {};

    ngOnInit(): void {
        this.paginationService.url = 'v1/site';

        this.paginationService.addExtraParams('archive', this.archive);
        this.paginationService.addExtraParams('live', this.live);
        this.paginationService.addExtraParams('system', this.system);

        this.getSitesList();

        this.siteService.refreshSites$.subscribe(site => this.handleSitesRefresh());

        if (this.id) {
            this.selectCurrentSite(this.id);
        } else if (!this.currentSite) {
            this.setCurrentSiteAsDefault();
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes && changes.id && changes.id.currentValue) {
            this.selectCurrentSite(changes.id.currentValue);
        }
    }

    /**
     * Manage the sites refresh when a event happen
     * @memberof SiteSelectorComponent
     */
    handleSitesRefresh(): void {
        this.paginationService.getCurrentPage().subscribe(items => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.sitesCurrentPage = items.splice(0);
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
        this.paginationService.getWithOffset(offset).subscribe(items => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.sitesCurrentPage = items.splice(0);
            this.totalRecords = this.totalRecords || this.paginationService.totalRecords;
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
        return (
            this.sitesCurrentPage &&
            this.sitesCurrentPage.filter(site => site.identifier === siteId)[0]
        );
    }

    private selectCurrentSite(siteId: string): void {
        const selectedInCurrentPage = this.getSiteByIdFromCurrentPage(siteId);
        this.currentSite = selectedInCurrentPage
            ? Observable.of(selectedInCurrentPage)
            : this.siteService.getSiteById(siteId);
    }

    private setCurrentSiteAsDefault() {
        if (this.siteService.currentSite) {
            this.currentSite = Observable.of(this.siteService.currentSite);
            this.siteChange(this.siteService.currentSite);
        } else {
            this.siteService.switchSite$.first().subscribe((site: Site) => {
                this.currentSite = Observable.of(site);
                this.siteChange(site);
            });
        }
    }
}
