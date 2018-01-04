import { BaseComponent } from '../_common/_base/base-component';
import { Component, ViewEncapsulation } from '@angular/core';
import { DotMessageService } from '../../../api/services/dot-messages-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-global-search',
    styleUrls: ['./global-search.scss'],
    templateUrl: 'global-search.html'
})
export class GlobalSearch extends BaseComponent {
    constructor(dotMessageService: DotMessageService) {
        super(['search'], dotMessageService);
    }
}
