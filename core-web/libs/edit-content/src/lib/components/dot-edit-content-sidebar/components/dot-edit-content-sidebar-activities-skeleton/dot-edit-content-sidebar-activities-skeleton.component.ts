import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-edit-content-sidebar-activities-skeleton',
    imports: [SkeletonModule],
    templateUrl: './dot-edit-content-sidebar-activities-skeleton.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarActivitiesSkeletonComponent {
    /**
     * Number of skeleton items to display
     */
    items = input<number>(3);

    /**
     * Array of skeleton items for rendering
     */
    $skeletonItems = computed(() => Array.from({ length: this.items() }));
}
