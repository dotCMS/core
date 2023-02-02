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

    /**
     * Return the contentlet Thumbanil based in the inode
     *
     * @param {string} inode
     * @return {*}  {string}
     * @memberof DotAssetCardComponent
     */
    getImage(inode: string): string {
        return `/dA/${inode}/500w/20q`;
    }

    /**
     * Return the contentlet icon
     *
     * @return {*}  {string}
     * @memberof DotAssetCardComponent
     */
    getContentletIcon(): string {
        return this.contentlet?.baseType !== 'FILEASSET'
            ? this.contentlet?.contentTypeIcon
            : this.contentlet?.__icon__;
    }
}
