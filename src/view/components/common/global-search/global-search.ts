import {BaseComponent} from '../_base/base-component';
import {Component, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MessageService} from '../../../../api/services/messages-service';

@Component({
    directives: [MD_INPUT_DIRECTIVES, FORM_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-global-search',
    styleUrls: ['global-search.css'],
    templateUrl: ['global-search.html'],
})
export class GlobalSearch extends BaseComponent{
    constructor(private messageService: MessageService) {
        super(['search'], messageService);
    }
}