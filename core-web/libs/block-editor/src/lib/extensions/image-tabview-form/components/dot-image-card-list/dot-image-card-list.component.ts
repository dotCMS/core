import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { sanitizeUrl } from '@dotcms/block-editor';
import { squarePlus } from '../../../../shared/components/suggestions/suggestion-icons';

@Component({
    selector: 'dot-dot-image-card-list',
    templateUrl: './dot-image-card-list.component.html',
    styleUrls: ['./dot-image-card-list.component.scss']
})
export class DotImageCardListComponent {
    @Input() contentlets: DotCMSContentlet[][] = [];
    @Input() resultsSize: number;
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();
    @Output() loadItems: EventEmitter<number> = new EventEmitter();

    public form: FormGroup;
    public icon = sanitizeUrl(squarePlus);

    loadContentlets(event) {
        const offset = this.contentlets.length * 2;
        if (event.first === 0 || offset >= this.resultsSize) {
            return;
        }

        this.loadItems.emit(offset);
    }
}
