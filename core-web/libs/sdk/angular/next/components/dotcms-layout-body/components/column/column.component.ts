import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotPageAssetLayoutColumn } from '../../../../models';
import { ContainerComponent } from '../container/container.component';

/**
 * This component renders a column with all its content using the layout provided by dotCMS Page API.
 *
 * @see {@link https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas}
 * @category Components
 * @internal
 */
@Component({
    selector: 'dotcms-column',
    standalone: true,
    imports: [ContainerComponent],
    template: `
        <div [class]="customColumnClass" [style.width.%]="column.widthPercent">
            @for (container of column.containers; track $index) {
                <dotcms-container [container]="container" />
            }
        </div>
    `,
    styleUrl: './column.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ColumnComponent {
    /**
     * The column data to be rendered
     */
    @Input({ required: true }) column!: DotPageAssetLayoutColumn;

    /**
     * The custom column class that combines the styleClass from the column data with the base column class
     */
    protected get customColumnClass(): string {
        return `${this.column.styleClass || ''} column`;
    }
}
