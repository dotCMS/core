import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotPageAssetLayoutRow } from '../../models';
import { ColumnComponent } from '../column/column.component';

@Component({
    selector: 'dotcms-row',
    standalone: true,
    imports: [ColumnComponent],
    template: `@for (column of row.columns; track $index) {
        <dotcms-column [column]="column" />
    }`,
    styleUrl: './row.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class RowComponent {
    @Input({ required: true }) row!: DotPageAssetLayoutRow;
}
