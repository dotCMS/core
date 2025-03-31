import { ChangeDetectionStrategy, Component, Input, signal } from '@angular/core';

import { combineClasses, getColumnPositionClasses } from '@dotcms/uve/internal';
import { DotPageAssetLayoutColumn } from '@dotcms/uve/types';

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
        <div data-dot="column" [class]="customClasses()">
            <div [class]="column.styleClass">
                @for (container of column.containers; track $index) {
                    <dotcms-container [container]="container" />
                }
            </div>
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

    customClasses = signal('');

    ngOnInit() {
        const positionClasses = getColumnPositionClasses(this.column);

        this.customClasses.set(
            combineClasses([positionClasses.startClass, positionClasses.endClass])
        );
    }

    /**
     * The custom column class that combines the styleClass from the column data with the base column class
     */
    protected get customColumnClass(): string {
        return `${this.column.styleClass || ''} column`;
    }
}
