import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { sanitizeUrl } from '@dotcms/block-editor';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { squarePlus } from '../../../../shared/components/suggestions/suggestion-icons';

@Component({
    selector: 'dot-image-card-list',
    templateUrl: './dot-image-card-list.component.html',
    styleUrls: ['./dot-image-card-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageCardListComponent {
    @Input() done = false;
    @Input() contentlets: DotCMSContentlet[][] = [];
    @Input() loading = true;
    @Output() nextBatch: EventEmitter<number> = new EventEmitter();
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();

    public loadingItems = [null, null, null];
    public icon = sanitizeUrl(squarePlus);

    onScrollIndexChange(e: { first: number; last: number }, offset: number) {
        if (this.done) {
            return;
        }

        // -1 so as not to wait until the last element is reached.
        const end = e.last - 1;
        const total = this.contentlets.length - 1;

        if (end === total) {
            this.nextBatch.emit(offset);
        }
    }
}
