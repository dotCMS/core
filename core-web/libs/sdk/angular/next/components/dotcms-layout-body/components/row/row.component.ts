import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotPageAssetLayoutRow } from '../../../../models';
import { ColumnComponent } from '../column/column.component';

/**
 * This component renders a row with all its content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @internal
 */
@Component({
    selector: 'dotcms-row',
    standalone: true,
    imports: [ColumnComponent],
    template: `
        <div class="dot-row-container">
            <div [class]="customRowClass" data-dot-object="row">
                @for (column of row.columns; track $index) {
                    <dotcms-column [column]="column" />
                }
            </div>
        </div>
    `,
    styleUrl: './row.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RowComponent {
    /**
     * The row data to be rendered
     */
    @Input({ required: true }) row!: DotPageAssetLayoutRow;

    /**
     * The custom row class that combines the styleClass from the row data with the base row class
     */
    protected get customRowClass(): string {
        return `${this.row.styleClass || ''} row`;
    }
}
