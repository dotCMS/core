import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    inject
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { ScrollerModule } from 'primeng/scroller';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotAssetCardComponent } from '../dot-asset-card/dot-asset-card.component';
import { DotAssetCardSkeletonComponent } from '../dot-asset-card-skeleton/dot-asset-card-skeleton.component';

const squarePlus =
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDEiIHZpZXdCb3g9IjAgMCA0MCA0MSIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuMDc2MTcgOC41NTcxMkgwLjA5NTIxNDhWMzYuNDIzOEMwLjA5NTIxNDggMzguNjEzMyAxLjg4NjY0IDQwLjQwNDcgNC4wNzYxNyA0MC40MDQ3SDMxLjk0MjhWMzYuNDIzOEg0LjA3NjE3VjguNTU3MTJaTTM1LjkyMzggMC41OTUyMTVIMTIuMDM4MUM5Ljg0ODU1IDAuNTk1MjE1IDguMDU3MTIgMi4zODY2NCA4LjA1NzEyIDQuNTc2MTdWMjguNDYxOUM4LjA1NzEyIDMwLjY1MTQgOS44NDg1NSAzMi40NDI4IDEyLjAzODEgMzIuNDQyOEgzNS45MjM4QzM4LjExMzMgMzIuNDQyOCAzOS45MDQ3IDMwLjY1MTQgMzkuOTA0NyAyOC40NjE5VjQuNTc2MTdDMzkuOTA0NyAyLjM4NjY0IDM4LjExMzMgMC41OTUyMTUgMzUuOTIzOCAwLjU5NTIxNVpNMzUuOTIzOCAyOC40NjE5SDEyLjAzODFWNC41NzYxN0gzNS45MjM4VjI4LjQ2MTlaTTIxLjk5MDUgMjQuNDgwOUgyNS45NzE0VjE4LjUwOTVIMzEuOTQyOFYxNC41Mjg1SDI1Ljk3MTRWOC41NTcxMkgyMS45OTA1VjE0LjUyODVIMTYuMDE5VjE4LjUwOTVIMjEuOTkwNVYyNC40ODA5WiIgZmlsbD0iIzU3NkJFOCIvPgo8L3N2Zz4K';

@Component({
    selector: 'dot-asset-card-list',
    templateUrl: './dot-asset-card-list.component.html',
    styleUrls: ['./dot-asset-card-list.component.scss'],
    standalone: true,
    imports: [ScrollerModule, DotAssetCardComponent, DotAssetCardSkeletonComponent],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardListComponent implements OnChanges {
    @Output() nextBatch: EventEmitter<number> = new EventEmitter();
    @Output() selectedItem: EventEmitter<DotCMSContentlet> = new EventEmitter();

    @Input() done = false;
    @Input() loading = true;
    @Input() contentlets: DotCMSContentlet[] = [];

    private domSanitizer: DomSanitizer = inject(DomSanitizer);
    public loadingItems = [null, null, null];
    public icon = this.domSanitizer.bypassSecurityTrustResourceUrl(squarePlus);
    private _itemRows: DotCMSContentlet[][] = [];
    private _offset = 0;

    get rows() {
        // Force Scroll to Update by breaking the object Reference
        return [...this._itemRows];
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.contentlets) {
            this._offset = this.contentlets?.length || 0;
            this._itemRows = this.createRowItem(this.contentlets);
        }
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
