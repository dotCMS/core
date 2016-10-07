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
    private currentSite: Site;
    private sites: Site[];
    private message: string;

    constructor(private siteService: SiteService, messageService: MessageService) {
        super(['updated-current-site-message', 'archived-current-site-message', 'modes.Close'], messageService);
    }

    ngOnInit(): void {
        this.siteService.switchSite$.subscribe(site => this.currentSite = site);
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

        }, error => alert(error.errorsMessages));
    }
}
