import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-seo-image-preview',
    templateUrl: './dot-seo-image-preview.component.html',
    styleUrls: ['./dot-seo-image-preview.component.scss'],
    imports: [DotMessagePipe, CommonModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSeoImagePreviewComponent {
    @Input() image: string;
    noImageAvailable = false;

    onImageError() {
        this.noImageAvailable = true;
    }
}
