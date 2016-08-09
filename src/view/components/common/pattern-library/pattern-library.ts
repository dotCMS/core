import {Component, ViewEncapsulation} from '@angular/core';

import {MdButton} from '@angular2-material/button';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import { MdUniqueSelectionDispatcher } from '@angular2-material/core';
import { MD_RADIO_DIRECTIVES } from '@angular2-material/radio';


@Component({
    directives: [MdButton, MD_INPUT_DIRECTIVES, MdCheckbox, MD_RADIO_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [MdUniqueSelectionDispatcher],
    selector: 'pattern-library',
    styleUrls: ['pattern-library.css'],
    templateUrl: ['pattern-library.html'],
})

export class PatternLibrary {
    constructor() {
        console.log('PL')
    }
}