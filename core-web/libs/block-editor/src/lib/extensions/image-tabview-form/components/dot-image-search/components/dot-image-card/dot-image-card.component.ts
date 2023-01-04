import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-image-card',
    templateUrl: './dot-image-card.component.html',
    styleUrls: ['./dot-image-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageCardComponent {
    @Input() contentlet: DotCMSContentlet;

    getImage(inode) {
        return `/dA/${inode}/500w/20q`;
    }
}
