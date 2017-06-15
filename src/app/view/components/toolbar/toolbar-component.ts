import { Component, ViewEncapsulation, ViewChild } from '@angular/core';
import { SiteService, Site } from '../../../api/services/site-service';
import { SiteSelectorComponent } from '../site-selector/dot-site-selector.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'toolbar-component',
    templateUrl: 'toolbar-component.html',
})
export class ToolbarComponent {

    @ViewChild('siteSelector') siteSelectorComponent: SiteSelectorComponent;

    constructor(private siteService: SiteService) {

    }

    siteChange(site: Site): void {
        this.siteService.switchSite(site);
    }
}