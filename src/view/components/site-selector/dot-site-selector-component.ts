import {Component, ViewEncapsulation} from '@angular/core';
import {Site} from '../../../api/services/site-service';
import {SiteService} from '../../../api/services/site-service';
import {MessageService} from '../../../api/services/messages-service';
import {BaseComponent} from '../common/_base/base-component';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-site-selector-component',
    styleUrls: ['dot-site-selector-component.css'],
    templateUrl: ['dot-site-selector-component.html'],
})
export class SiteSelectorComponent extends BaseComponent {
    private currentSite: any;
    private sites: Site[];
    private message: string;
    private filteredSitesResults: Array<any>;

    constructor(private siteService: SiteService, messageService: MessageService) {
        super(['updated-current-site-message', 'archived-current-site-message', 'modes.Close'], messageService);
    }

    ngOnInit(): void {
        this.siteService.switchSite$.subscribe(site => this.currentSite = {
            label: site.hostName,
            value: site.identifier,
        });
        this.siteService.sites$.subscribe(sites => this.sites = sites);
        this.siteService.archivedCurrentSite$.subscribe(site => {
            this.message = this.i18nMessages['archived-current-site-message'];
        });
        this.siteService.updatedCurrentSite$.subscribe(site => {
            this.message = this.i18nMessages['updated-current-site-message'];
        });
    }

    switchSite(option: any): void {
        this.siteService.switchSite(option.value).subscribe(response => {

        });
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
        for(let i = 0; i < this.sites.length; i++) {
            let site = this.sites[i];
            if(site.hostName.toLowerCase().indexOf(event.query.toLowerCase()) >= 0) {
                this.filteredSitesResults.push({
                    label: site.hostName,
                    value: site.identifier,
                });
            }
        }
    }

    /**
     * Display all the existing login as users availables loaded on the
     * userList variable initialized on the ngOnInit method
     *
     * @param event - The click event to display the dropdown options
     */
    handleSitesDropdownClick(event): void {
        this.filteredSitesResults = [];
        setTimeout(() => {
            for(let i = 0; i < this.sites.length; i++) {
                let site = this.sites[i];
                this.filteredSitesResults.push({
                    label: site.hostName,
                    value: site.identifier,
                });
            }
        }, 100);
    }
}
