import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';

import { DotErrorBoundaryComponent } from '../../components/dot-error-boundary/dot-error-boundary.component';
import {
    DotError,
    DotErrorCodes,
    DotErrorHandler
} from '../../components/dot-error-boundary/dot-error-handler.service';
import { DotPageAssetLayoutRow } from '../../models';
import { ColumnComponent } from '../column/column.component';

/**
 * This component is responsible to display a row with columns.
 *
 * @export
 * @class RowComponent
 */
@Component({
    selector: 'dotcms-row',
    standalone: true,
    imports: [ColumnComponent, DotErrorBoundaryComponent],
    template: `
        @for (column of row.columns; track $index) {
            <dot-error-boundary>
                <dotcms-column [column]="column" [rowIndex]="index" [colIndex]="$index" />
            </dot-error-boundary>
        }
    `,
    styleUrl: './row.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RowComponent implements OnInit {
    /**
     * The row object containing the columns.
     *
     * @type {DotPageAssetLayoutRow}
     * @memberof RowComponent
     */
    @Input({ required: true }) row!: DotPageAssetLayoutRow;
    @Input() index!: number;
    errorHandler = inject(DotErrorHandler);

    ngOnInit() {
        // RANDOMLY THROW AN ERROR BUT WE CAN MAKE INTEGRITY CHECKS OF THE ROWS
        try {
            if (Math.random() > 0.8)
                throw new DotError(DotErrorCodes.ROW001, {
                    row: this.index
                });
        } catch (error) {
            this.errorHandler.handleError(error);
        }
    }
}
