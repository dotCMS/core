import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

/**
 * @class DotTableSkeletonComponent
 *
 * Represents a table component with skeleton loading feature with dynamic columns and rows
 */
@Component({
    selector: 'dot-table-skeleton',
    standalone: true,
    imports: [SkeletonModule, TableModule],
    templateUrl: './dot-table-skeleton.component.html',
    styleUrl: './dot-table-skeleton.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTableSkeletonComponent {
    /**
     * Represents the name of the header of the table
     *
     */
    columns = input.required<string[]>();
    /**
     * Represents the number of rows needed in the table with skeleton.
     *
     * @param {number} rows - The number of rows for the input.
     */
    rows = input<number>(5);

    $data = computed<string[]>(() => {
        return Array.from({ length: this.rows() }).map((_, i) => `${i}`);
    });
}
