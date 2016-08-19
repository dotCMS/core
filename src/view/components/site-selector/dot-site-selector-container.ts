
import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {SiteSelectorComponent} from "./dot-site-selector-component";
import {SiteService} from "../../../api/services/site-service";
import {Site} from "../../../api/services/site-service";

@Component({
    directives: [SiteSelectorComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-site-selector-container',
    styleUrls: [],
    template: `
        <dot-site-selector-component [currentSite] ="currentSite"
                                     [sites] = "sites"
                                     (change) = "switchSite($event)">
        </dot-site-selector-component>
    `,
})
export class SiteSelectorContainer{

    private sites:Site[];
    private currentSite:Site;

    constructor(private siteService:SiteService){

    }

    ngOnInit(){
        this.siteService.getAllSites().subscribe( response => {
             this.currentSite = response.currentSite;
            console.log('RESPONSE', response);
            this.sites = response.sites;
        }, error => alert( error.errorsMessages ));
    }

    switchSite(siteId:string){
        this.siteService.switchSite(siteId).subscribe( response => {

        }, error => alert( error.errorsMessages ));
    }
}
