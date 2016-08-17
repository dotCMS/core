import {Component, ViewEncapsulation} from '@angular/core';

import {DotSelect, DotOption} from '../dot-select/dot-select';
import {DropdownComponent} from "../dropdown-component/dropdown-component";
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MD_RADIO_DIRECTIVES} from '@angular2-material/radio';
import {MdButton} from '@angular2-material/button';
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import {MdUniqueSelectionDispatcher} from '@angular2-material/core';

@Component({
    directives: [MdButton, MD_INPUT_DIRECTIVES, MdCheckbox, MD_RADIO_DIRECTIVES, DropdownComponent, DotSelect, DotOption],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [MdUniqueSelectionDispatcher],
    selector: 'pattern-library',
    styleUrls: ['pattern-library.css'],
    templateUrl: ['pattern-library.html'],
})

export class PatternLibrary {

}