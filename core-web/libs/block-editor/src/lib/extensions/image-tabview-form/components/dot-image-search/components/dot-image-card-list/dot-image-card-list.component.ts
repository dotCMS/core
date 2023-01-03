import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { sanitizeUrl, squarePlus } from '@dotcms/block-editor';

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

        const end = e.last;
        const total = this.contentlets.length;

        if (end === total) {
            this.nextBatch.emit(offset);
        }
    }
}
