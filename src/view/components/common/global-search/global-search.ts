import {BaseComponent} from '../_base/base-component';
import {Component, ViewEncapsulation} from '@angular/core';
import {MessageService} from '../../../../api/services/messages-service';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.None,
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