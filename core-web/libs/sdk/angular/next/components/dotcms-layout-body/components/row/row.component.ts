import { ChangeDetectionStrategy, Component, Input, OnChanges, signal } from '@angular/core';

import { combineClasses } from '@dotcms/uve/internal';
import { DotPageAssetLayoutRow } from '@dotcms/uve/types';

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
            <div [class]="customClasses()" data-dot-object="row" data-testid="dotcms-row">
                @for (column of row.columns; track $index) {
                    <dotcms-column [column]="column" />
                }
            </div>
        </div>
    `,
    styleUrl: './row.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RowComponent implements OnChanges {
    /**
     * The row data to be rendered
     */
    @Input({ required: true }) row!: DotPageAssetLayoutRow;

    customClasses = signal('');

    ngOnChanges() {
        this.customClasses.set(combineClasses([this.row.styleClass || 'dot-row']));
    }
}
