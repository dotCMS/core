
import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {DropdownComponent} from "../common/dropdown-component/dropdown-component";
import {Site} from "../../../api/services/site-service";
import {DotSelect} from "../common/dot-select/dot-select";
import {DotOption} from "../common/dot-select/dot-select";
import {SiteService} from "../../../api/services/site-service";

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
export class SiteSelectorComponent{
    private currentSite:string;
    private sites:Site[];

    constructor(private siteService:SiteService){

    }

    ngOnInit() {
        this.siteService.getAllSites().subscribe( response => {
            this.currentSite = response.currentSite;
            this.sites = response.sites;
        }, error => alert( error.errorsMessages ));
    }

    switchSite(option:any){
        this.siteService.switchSite( option.value ).subscribe( response => {

        }, error => alert( error.errorsMessages ));
    }
}

