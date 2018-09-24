import { BaseComponent } from '../_common/_base/base-component';
import { Component, ViewEncapsulation } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-global-search',
    styleUrls: ['./global-search.scss'],
    templateUrl: 'global-search.html'
})
export class GlobalSearchComponent extends BaseComponent {
    constructor(dotMessageService: DotMessageService) {
        super(['search'], dotMessageService);
    }
}
