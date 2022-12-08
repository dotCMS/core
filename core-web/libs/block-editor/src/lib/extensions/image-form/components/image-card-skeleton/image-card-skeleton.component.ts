import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-image-card-skeleton',
    templateUrl: './image-card-skeleton.component.html',
    styleUrls: ['./image-card-skeleton.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageCardSkeletonComponent {}
