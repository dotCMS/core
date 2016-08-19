
import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {DropdownComponent} from "../common/dropdown-component/dropdown-component";
import {Site} from "../../../api/services/site-service";
import {DotSelect} from "../common/dot-select/dot-select";
import {DotOption} from "../common/dot-select/dot-select";

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
    @Input() currentSite:string;
    @Input() sites:Site[];

    @Output() change = new EventEmitter<string>();

    switchSite(option){
        this.change.emit( option.value );
    }
}

