import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotSpinnerModule } from '@dotcms/ui';

@Component({
    selector: 'dot-video-placeholder',
    templateUrl: './video-placeholder.component.html',
    styleUrls: ['./video-placeholder.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [DotSpinnerModule, ButtonModule]
})
export class VideoPlaceholderComponent {
    @Output() cancel = new EventEmitter<boolean>();
}
