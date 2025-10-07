import { ChangeDetectionStrategy, Component } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-edit-content-sidebar-activities-skeleton',
    imports: [SkeletonModule],
    templateUrl: './dot-edit-content-sidebar-activities-skeleton.component.html',
    styleUrls: ['./dot-edit-content-sidebar-activities-skeleton.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarActivitiesSkeletonComponent {}
