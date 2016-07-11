import {Component, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';

// Angular Material
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-global-search',
    styleUrls: ['global-search.css'],
    templateUrl: ['global-search.html'],
    providers: [],
    directives: [MD_INPUT_DIRECTIVES, FORM_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated
})
export class GlobalSearch {
    constructor() {
    }
}