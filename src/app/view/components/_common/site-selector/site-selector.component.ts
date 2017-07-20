import {
    Component,
    ViewEncapsulation,
    ViewChild,
    forwardRef,
    Output,
    EventEmitter,
    Input,
    SimpleChanges
} from '@angular/core';
import { AutoComplete } from 'primeng/primeng';
import { BaseComponent } from '../_base/base-component';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DotcmsConfig } from '../../../../api/services/system/dotcms-config';
import { IframeOverlayService } from '../../../../api/services/iframe-overlay-service';
import { MessageService } from '../../../api/services/messages-service';
import { PaginatorService } from '../../../../api/services/paginator';
import { SearchableDropdownComponent } from '../searchable-dropdown/component';
import { Site } from '../../../../api/services/site-service';
import { SiteService } from '../../../../api/services/site-service';
import { Observable } from 'rxjs/Observable';

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
    selector: 'site-selector-component',
    styles: [require('./site-selector.component.scss')],
    templateUrl: 'site-selector.component.html',
})
export class SiteSelectorComponent implements ControlValueAccessor {
    private static readonly MIN_CHARECTERS_TO_SERACH = 3;

    @ViewChild('searchableDropdown') searchableDropdown: SearchableDropdownComponent;
    @Output() change: EventEmitter<Site> = new EventEmitter();

    private currentSite: Observable<Site>;
    private sitesCurrentPage: Site[];
    private filteredSitesResults: Array<any>;
    private paginationPage = 1;
    private paginationPerPage: number;
    private paginatorLinks: number;
    private paginationQuery = '';
    private totalRecords: number;

    propagateChange = (_: any) => {};

    constructor(
        private config: DotcmsConfig,
        private iframeOverlayService: IframeOverlayService,
        private paginationService: PaginatorService,
        private siteService: SiteService
    ) {}

    ngOnInit(): void {
        this.paginationService.url = 'v1/site';

        this.getSitesList();

        if (this.siteService.currentSite) {
            this.currentSite = Observable.of(this.siteService.currentSite);
            this.propagateChange(this.siteService.currentSite);
        } else {
            this.siteService.switchSite$.subscribe(site => {
                this.currentSite = Observable.of(site);
                this.propagateChange(site.identifier);
            });
        }

        this.siteService.refreshSites$.subscribe(site => this.handleSitesRefresh());
    }

    /**
     * Manage the sites refresh when a event happen
     * @memberof SiteSelectorComponent
     */
    handleSitesRefresh(): void {
        this.paginationService.getCurrentPage().subscribe((items) => {
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
    getSitesList(filter = '',  offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe( items => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.sitesCurrentPage = items.splice(0);
            this.totalRecords = this.totalRecords | this.paginationService.totalRecords;
        });
    }

    /**
     * Call when the selected site changed and the change event is emmited
     * @param {Site} site
     * @memberof SiteSelectorComponent
     */
    siteChange(site: Site): void {
        let value = site.identifier;
        this.change.emit(site);
        this.propagateChange(value);
    }

    /**
     * Write a new value to the element
     * @param {*} value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: string): void {
        if (value) {
            this.selectCurrentSite(value);
        }
    }

    /**
     * Set the function to be called when the control receives a change event.
     * @param {any} fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    private getSiteByIdFromCurrentPage(siteId: string): Site {
        return this.sitesCurrentPage && this.sitesCurrentPage.filter(site => site.identifier === siteId)[0];
    }

    private selectCurrentSite(siteId: string): void {
        let selectedInCurrentPage = this.getSiteByIdFromCurrentPage(siteId);
        this.currentSite = selectedInCurrentPage ? Observable.of(selectedInCurrentPage) : this.siteService.getSiteById(siteId);
    }
}
