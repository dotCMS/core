import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { sanitizeUrl, squarePlus } from '@dotcms/block-editor';

@Component({
    selector: 'dot-asset-card-list',
    templateUrl: './dot-asset-card-list.component.html',
    styleUrls: ['./dot-asset-card-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardListComponent {
    @Output() nextBatch: EventEmitter<number> = new EventEmitter();
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();

    @Input() done = false;
    @Input() loading = true;
    @Input() set contentlets(value: DotCMSContentlet[]) {
        this._offset = value?.length || 0;
        this._itemRows = this.createRowItem(value);
    }

    public loadingItems = [null, null, null];
    public icon = sanitizeUrl(squarePlus);
    private _itemRows: DotCMSContentlet[][] = [];
    private _offset = 0;

    get rows() {
        // Force Scroll to Update by breaking the object Reference
        return [...this._itemRows];
    }

    onScrollIndexChange(e: { first: number; last: number }) {
        if (this.done) {
            return;
        }

        const end = e.last;
        const total = this.rows.length;

        if (end === total) {
            // We multiply by 2 because of the way we should de images.
            // Two images per row.
            this.nextBatch.emit(this._offset);
        }
    }

    /**
     *
     * Create an array of type: [[DotCMSContentlet, DotCMSContentlet], ...]
     * Due PrimeNg virtual scroll allows only displaying one element at a time [https://primefaces.org/primeng/virtualscroller],
     * and figma's layout requires displaying two columns of contentlets [https://github.com/dotCMS/core/issues/23235]
     *
     * @private
     * @param {DotCMSContentlet[][]} prev
     * @param {DotCMSContentlet[]} contentlets
     * @return {*}
     * @memberof DotAssetSearchStore
     */
    private createRowItem(contentlets: DotCMSContentlet[] = []) {
        const rows = [];
        contentlets.forEach((contentlet) => {
            const i = rows.length - 1;
            rows[i]?.length < 2 ? rows[i].push(contentlet) : rows.push([contentlet]);
        });

        return rows;
    }
}
