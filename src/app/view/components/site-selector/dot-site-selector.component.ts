import { Component, ViewEncapsulation, ViewChild, forwardRef, Output, EventEmitter, Input, SimpleChanges } from '@angular/core';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { Site } from '../../../api/services/site-service';
import { SiteService } from '../../../api/services/site-service';
import { MessageService } from '../../../api/services/messages-service';
import { BaseComponent } from '../_common/_base/base-component';
import { AutoComplete } from 'primeng/primeng';
import { IframeOverlayService } from '../../../api/services/iframe-overlay-service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SearchableDropdownComponent } from '../_common/searchable-dropdown/component';

/**
 * It is dropdown of sites, it handle pagination and global search
 * @export
 * @class SiteSelectorComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [
        SearchableDropdownComponent,
        {
        multi: true,
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => SiteSelectorComponent)
        }
    ],
    selector: 'dot-site-selector-component',
    styles: [require('./dot-site-selector.component.scss')],
    templateUrl: 'dot-site-selector.component.html',
})
export class SiteSelectorComponent implements ControlValueAccessor {

    private static readonly MIN_CHARECTERS_TO_SERACH = 3;

    @ViewChild('searchableDropdown') searchableDropdown: SearchableDropdownComponent;
    @Output() change: EventEmitter<Site> = new EventEmitter();

    public value: string;
    private currentSite: Site;
    private sitesCurrentPage: Site[];
    private sitesCounter: number;
    private filteredSitesResults: Array<any>;
    private paginationPage = 1;
    private paginationPerPage: number;
    private paginatorLinks: number;
    private paginationQuery = '';

    propagateChange = (_: any) => {};

    constructor(private siteService: SiteService, private config: DotcmsConfig, private iframeOverlayService: IframeOverlayService) {

    }

    ngOnInit(): void {
        this.config.getConfig().subscribe(configParams => {
            this.paginationPerPage = configParams.paginatorRows;
            this.paginatorLinks = configParams.paginatorLinks;

            this.paginateSites();
        });

        this.siteService.sitesCounter$.subscribe(counter => {
            this.sitesCounter = counter;
        });
        this.currentSite = this.siteService.currentSite;
        this.siteService.switchSite$.subscribe(site => {
            this.writeValue(site.identifier);
        });
    }

    /**
     * Call when the global serach changed
     * @param {any} filter
     * @memberof SiteSelectorComponent
     */
    handleFilterChange(filter): void {
        this.paginateSites(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof SiteSelectorComponent
     */
    handlePageChange(event): void {
        this.paginateSites(event.filter, event.page + 1);
    }

    /**
     * Call to load a new page.
     * @param {string} [filter='']
     * @param {number} [page=1]
     * @memberof SiteSelectorComponent
     */
    paginateSites(filter = '',  page = 1): void {
        this.siteService.paginateSites(filter, false, page, this.paginationPerPage).subscribe( sites => {
            this.sitesCurrentPage = sites;
            this.selectCurrentSite();
        });
    }

    /**
     * Call when the selected site changed and the change event is emmited
     * @param {Site} site
     * @memberof SiteSelectorComponent
     */
    siteChange(site: Site): void {
        let value =  site.identifier;
        this.change.emit(site);
        this.propagateChange(value);
    }

    /**
     * Write a new value to the element
     * @param {*} value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: string): void {
        this.value = value;
        this.selectCurrentSite();
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

    private selectCurrentSite(): void {
        if (this.sitesCurrentPage) {
            let selected = this.sitesCurrentPage.filter( site => site.identifier === this.value);
            this.currentSite = selected.length > 0 ? selected[0] : this.currentSite;
        }
    }
}
