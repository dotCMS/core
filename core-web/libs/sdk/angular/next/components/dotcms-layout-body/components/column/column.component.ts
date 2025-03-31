import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';

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
        <div [class]="column.styleClass">
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

    @HostBinding('class') customClasses = '';

    ngOnInit() {
        const positionClasses = getColumnPositionClasses(this.column);

        this.customClasses = combineClasses([positionClasses.startClass, positionClasses.endClass]);
    }
}
