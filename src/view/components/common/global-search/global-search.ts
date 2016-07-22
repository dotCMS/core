import {Component, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';

// Angular Material
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';

@Component({
    directives: [MD_INPUT_DIRECTIVES, FORM_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-global-search',
    styleUrls: ['global-search.css'],
    templateUrl: ['global-search.html'],
})
export class GlobalSearch {
    constructor() {
    }
}