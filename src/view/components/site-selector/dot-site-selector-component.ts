
import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {DropdownComponent} from '../common/dropdown-component/dropdown-component';
import {Site} from '../../../api/services/site-service';
import {DotSelect} from '../common/dot-select/dot-select';
import {DotOption} from '../common/dot-select/dot-select';
import {SiteService} from '../../../api/services/site-service';

@Component({
    directives: [DropdownComponent, DotSelect, DotOption],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-site-selector-component',
    styleUrls: ['dot-site-selector-component.css'],
    templateUrl: ['dot-site-selector-component.html'],
})
export class SiteSelectorComponent {
    private currentSite: Site;
    private sites: Site[];

    constructor(private siteService: SiteService) {

    }

    ngOnInit(): void {
        this.siteService.$switchSite.subscribe( site => this.currentSite = site);
        this.siteService.$sites.subscribe( sites => this.sites = sites);
    }

    switchSite(option: any): void {
        this.siteService.switchSite(option.value).subscribe( response => {

        }, error => alert( error.errorsMessages ));
    }
}
