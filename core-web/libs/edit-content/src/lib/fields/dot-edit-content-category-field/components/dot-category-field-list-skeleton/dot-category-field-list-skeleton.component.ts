import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-category-field-list-skeleton',
    imports: [SkeletonModule],
    template: `
        <ul class="m-0 p-1 list-none animate-fadein animate-duration-500">
            @for (_ of $rows(); track $index) {
                <li class="flex">
                    <p-skeleton size="1rem" styleClass="mr-2" />
                    <div style="flex: 1">
                        <p-skeleton width="100%" />
                    </div>
                </li>
            }
        </ul>
    `,
    styles: `
        li {
            min-height: 40px;
            align-content: center;
            flex-wrap: wrap;
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldListSkeletonComponent {
    /**
     * Represents the number of rows for a specific operation.
     */
    $numOfRows = input<number>(5, { alias: 'rows' });

    /**
     * The $rows variable represents a computed array of null values.
     */
    $rows = computed(() => {
        return Array(this.$numOfRows()).fill(null);
    });
}
