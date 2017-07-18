import {Component, Inject, NgModule} from '@angular/core';

import {SiteSelectorService} from './site-selector.service';
import {Site} from '../../core/treeable/shared/site.model';
import {SiteBrowserState} from '../../core/util/site-browser.state';
import {CommonModule} from '@angular/common';
import {AutoCompleteModule} from 'primeng/components/autocomplete/autocomplete';
import {InputTextModule} from 'primeng/components/inputtext/inputtext';
import {FormsModule} from '@angular/forms';

@Inject('updateService')
@Component({
    selector: 'site-selector',
    styles: [require('./../app.css')],
    templateUrl: 'site-selector.html'
})
export class SiteSelectorComponent {

    sites: Site[];
    filteredHosts: any[];
    host = '';

    constructor(
        private updateService: SiteBrowserState,
        private siteSelectorService: SiteSelectorService
    ) {
        this.host = updateService.getSelectedSite();
    }

    /**
     * Is called when a site is selected in the dropdown
     * @param event
     */
    siteSelected(event: any): void {
        this.updateService.changeSite(this.host);
    }

    /**
     * Is called as the user tyoes in the filtering select
     * @param event
     */
    filterHosts(event: any): void {
        this.siteSelectorService.filterForSites(event.query)
            .subscribe((sites: Site[]) => this.handleSiteResults(sites, event));
        setTimeout(() => {}, 100);
    }

    /**
     * Is called when the user clicks to view all sites in the dropdown
     */
    handleDropdownClick(): void {
        this.filteredHosts = [];
        this.siteSelectorService.getSites()
            .subscribe((sites: Site[]) => this.handleSiteResults(sites));
        setTimeout(() => {}, 100);
    }

    private handleSiteResults(hosts: Site[], event: any = 0): void {
        this.filteredHosts = [];
        this.sites = hosts;
        for (let i = 0; i < this.sites.length; i++) {
            let site = this.sites[i].hostname;
            if (event && site.toLowerCase().indexOf(event.query.toLowerCase()) === 0) {
                this.filteredHosts.push(site);
            }else {
                this.filteredHosts[i] = this.sites[i].hostname;
            }
        }
    }
}

@NgModule({
    declarations: [SiteSelectorComponent],
    exports: [SiteSelectorComponent],
    imports: [CommonModule, FormsModule, InputTextModule, AutoCompleteModule]
})
export class DotcmsSiteSelectorModule { }
