import { ChangeDetectionStrategy, Component, HostBinding, Input, OnInit } from '@angular/core';

import { DotPageAssetLayoutColumn } from '../../models';
import { getPositionStyleClasses } from '../../utils';
import { ContainerComponent } from '../container/container.component';

/**
 * This component is responsible to display a column with containers.
 *
 * @export
 * @class ColumnComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dotcms-column',
    standalone: true,
    imports: [ContainerComponent],
    template: `
        @for (container of column.containers; track $index) {
            <dotcms-container [container]="container" />
        }
    `,
    styleUrl: './column.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ColumnComponent implements OnInit {
    /**
     * The column object containing the containers.
     *
     * @type {DotPageAssetLayoutColumn}
     * @memberof ColumnComponent
     */
    @Input() column!: DotPageAssetLayoutColumn;

    /**
     * The data-testid attribute used for identifying the component during testing.
     *
     * @memberof ColumnComponent
     */
    @HostBinding('class') containerClasses = '';

    /**
     * Lifecycle hook that is called after data-bound properties of a directive are initialized.
     * Initializes the container classes based on the column's left offset and width.
     *
     * @memberof ColumnComponent
     */
    ngOnInit() {
        const { startClass, endClass } = getPositionStyleClasses(
            this.column.leftOffset,
            this.column.width + this.column.leftOffset
        );
        this.containerClasses = `${startClass} ${endClass}`;
    }
}
