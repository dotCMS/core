import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-image-card',
    templateUrl: './image-card.component.html',
    styleUrls: ['./image-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
