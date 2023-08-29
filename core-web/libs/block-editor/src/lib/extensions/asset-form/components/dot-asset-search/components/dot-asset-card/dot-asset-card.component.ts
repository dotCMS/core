import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { DotCMSContentlet, EDITOR_MARKETING_KEYS } from '@dotcms/dotcms-models';

import { DotMarketingConfigService } from '../../../../../../shared';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardComponent implements OnInit {
    showVideoThumbnail = true;

    @Input() contentlet: DotCMSContentlet;

    constructor(private dotMarketingConfigService: DotMarketingConfigService) {}

    ngOnInit() {
        this.showVideoThumbnail = this.dotMarketingConfigService.getProperty(
            EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL
        );
    }

    /**
     * Return the contentlet Thumbanil based in the inode
     *
     * @param {string} inode
     * @return {*}  {string}
     * @memberof DotAssetCardComponent
     */
    getImage(inode: string): string {
        return `/dA/${inode}/500w/50q`;
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
