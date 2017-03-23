import {Component, ViewEncapsulation, ViewChild} from '@angular/core';
import {SelectItem, AutoComplete} from "primeng/primeng";

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'pattern-library',
    styleUrls: ['pattern-library.css'],
    templateUrl: ['pattern-library.html']
})

export class PatternLibrary {
    private checkBoxValues: string[] = ['val3'];
    private radioBoxValues: string[] = ['val3'];
    private radioBoxDisabledValues: string[] = ['val'];
    private cities: SelectItem[];
    private selectedCity: string;
    private autocompleteResults: Array<string> = [];
    private displayDialog: boolean = false;

    @ViewChild(AutoComplete) private autoCompleteComponent: AutoComplete;

    constructor() {
        this.cities = [];
        this.cities.push({label:'Select City', value:null});
        this.cities.push({label:'New York', value:{id:1, name: 'New York', code: 'NY'}});
        this.cities.push({label:'Rome', value:{id:2, name: 'Rome', code: 'RM'}});
        this.cities.push({label:'London', value:{id:3, name: 'London', code: 'LDN'}});
        this.cities.push({label:'Istanbul', value:{id:4, name: 'Istanbul', code: 'IST'}});
        this.cities.push({label:'Paris', value:{id:5, name: 'Paris', code: 'PRS'}});
    }

    autocompleteComplete($event) {
        this.autocompleteResults = [];
        this.autocompleteResults = ['Hello', 'World'];
    }

    autocompleteCompleteDropdownClick(event: {originalEvent: Event, query: string}) {
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

        this.autocompleteResults = ['Hello', 'World'];

    }

    showDialog() {
        this.displayDialog = true;
    }
}