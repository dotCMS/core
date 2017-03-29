import {Component, ViewEncapsulation, ViewChild} from '@angular/core';
import {DotcmsConfig} from '../../../api/services/system/dotcms-config';
import {Site} from '../../../api/services/site-service';
import {SiteService} from '../../../api/services/site-service';
import {MessageService} from '../../../api/services/messages-service';
import {BaseComponent} from '../_common/_base/base-component';
import {AutoComplete} from 'primeng/primeng';
import {IframeOverlayService} from '../../../api/services/iframe-overlay-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-site-selector-component',
    styleUrls: ['dot-site-selector-component.css'],
    templateUrl: 'dot-site-selector-component.html',
})
export class SiteSelectorComponent extends BaseComponent {
    private currentSite: any;
    private sites: Site[];
    private sitesCounter: number;
    private message: string;
    private filteredSitesResults: Array<any>;
    private paginationPage = 1;
    private paginationPerPage: number;
    private paginationQuery = '';

    @ViewChild(AutoComplete) private autoCompleteComponent: AutoComplete;

    constructor(private siteService: SiteService, messageService: MessageService, config: DotcmsConfig,
                private iframeOverlayService: IframeOverlayService) {
        super(['updated-current-site-message', 'archived-current-site-message', 'modes.Close'], messageService);

        config.getConfig().subscribe(configParams => this.paginationPerPage = configParams.defaultRestPageCount);
    }

    ngOnInit(): void {
        this.siteService.switchSite$.subscribe(site => this.currentSite = {
            label: site.hostname,
            value: site.identifier,
        });
        this.siteService.sites$.subscribe(sites => this.sites = sites);
        this.siteService.sitesCounter$.subscribe(counter => this.sitesCounter = counter);
    }

    /**
     * This method changes the current site for the new one
     * clicked on the site selector.
     *
     * @param option the selected Site identifier
     */
    switchSite(option: any): void {
        this.siteService.switchSite(option.value).subscribe(response => {

        });

        this.iframeOverlayService.hide();
    }

    /**
     * Filter the users displayed in the dropdown by comparing if
     * the user name characters set on the drowpdown search box matches
     * some on the user names set on the userlist variable loaded on the
     * ngOnInit method
     *
     * @param event - The event with the query parameter to filter the users
     */
    filterSites(event): void {
        this.filteredSitesResults = [];

        /**
         * only execute the search if there is at least 3 characters
         */
        if (event.query.length >= 3) {
            /**
             * If the query change then clean the paginationPage
             * and paginationQuery variables
             */
            if (this.paginationQuery !== event.query) {
                this.paginationPage = 1;
                this.paginationQuery = event.query;
            }

            this.showPaginateSites();
        }
    }

    /**
     * Display all the existing login as users availables loaded on the
     * userList variable initialized on the ngOnInit method
     *
     * @param event - The click event to display the dropdown options
     */
    handleSitesDropdownClick(event: {originalEvent: Event, query: string}): void {
        this.iframeOverlayService.toggle();

        // TODO: get rid of this lines when this is fixed: https://github.com/primefaces/primeng/issues/745
        event.originalEvent.preventDefault();
        event.originalEvent.stopPropagation();
        if (this.autoCompleteComponent.panelVisible) {
            this.autoCompleteComponent.onDropdownBlur();
            this.autoCompleteComponent.hide();
        } else {
            this.autoCompleteComponent.onDropdownFocus();
            this.autoCompleteComponent.show();
        }

        this.filteredSitesResults = [];
        this.paginationPage = 1;
        this.paginationQuery = 'all';

        this.showPaginateSites();
    }

    /**
     * Call the SiteService paginateSite method with the values set on the
     * paginationQuery, paginationPage and paginationPerPage variables
     */
    private showPaginateSites(): void {
        /**
         * Call the web resource to get the subset of site results
         */
        this.siteService.paginateSites(this.paginationQuery, false, this.paginationPage, this.paginationPerPage).subscribe(response => {
            let results = response.sites.results;

            if (results.length) {
                this.iframeOverlayService.show();
            } else {
                this.iframeOverlayService.hide();
            }

            /*
             Include the sites results for the current pagination page
             */
            results.forEach(site => {
                this.filteredSitesResults.push( {
                    label: site.hostname,
                    value: site.identifier,
                });
            });
        });
    }
}
