import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;

    getImage(inode) {
        return `/dA/${inode}/500w/20q`;
    }

    contentletIcon() {
        return this.contentlet?.baseType !== 'FILEASSET'
            ? this.contentlet?.contentTypeIcon
            : this.contentlet?.__icon__;
    }
}
