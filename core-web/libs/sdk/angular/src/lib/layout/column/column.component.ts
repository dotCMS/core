import {
    ChangeDetectionStrategy,
    Component,
    HostBinding,
    inject,
    Input,
    OnInit
} from '@angular/core';

import { DotErrorBoundaryComponent } from '../../components/dot-error-boundary/dot-error-boundary.component';
import {
    DotError,
    DotErrorCodes,
    DotErrorHandler
} from '../../components/dot-error-boundary/dot-error-handler.service';
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
    imports: [ContainerComponent, DotErrorBoundaryComponent],
    template: `
        @for (container of column.containers; track $index) {
            <dot-error-boundary>
                <dotcms-container [container]="container" [col]="colIndex" [row]="rowIndex" />
            </dot-error-boundary>
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
    @Input() rowIndex!: number;
    @Input() colIndex!: number;

    /**
     * The data-testid attribute used for identifying the component during testing.
     *
     * @memberof ColumnComponent
     */
    @HostBinding('class') containerClasses = '';

    errorHandler = inject(DotErrorHandler);

    ngOnInit() {
        const { startClass, endClass } = getPositionStyleClasses(
            this.column.leftOffset,
            this.column.width + this.column.leftOffset
        );
        this.containerClasses = `${startClass} ${endClass}`;

        try {
            // RANDOMLY THROW AN ERROR BUT WE CAN MAKE INTEGRITY CHECKS OF THE COLUMNS

            if (Math.random() > 0.7)
                throw new DotError(DotErrorCodes.COL001, {
                    row: this.rowIndex,
                    column: this.colIndex
                });
        } catch (error) {
            this.errorHandler.handleError(error);
        }
    }
}
