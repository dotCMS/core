import { ChangeDetectionStrategy, Component, Input, OnChanges, signal } from '@angular/core';

import { DotPageAssetLayoutRow } from '@dotcms/types';
import { combineClasses, DOT_SECTION_ID_PREFIX } from '@dotcms/uve/internal';

import { ColumnComponent } from '../column/column.component';

/**
 * @description This component renders a row with all its content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @internal
 * @class RowComponent
 */
@Component({
    selector: 'dotcms-row',
    imports: [ColumnComponent],
    template: `
        <div [id]="sectionId()" [class]="customClasses()">
            <div class="dot-row" data-dot-object="row" data-testid="dotcms-row">
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
    @Input({ required: true }) sectionIndex!: number;

    customClasses = signal('');
    sectionId = signal('');

    ngOnChanges() {
        this.customClasses.set(combineClasses(['dot-row-container', this.row.styleClass ?? '']));
        this.sectionId.set(`${DOT_SECTION_ID_PREFIX}${this.sectionIndex}`);
    }
}
