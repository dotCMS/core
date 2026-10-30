import { DecimalPipe, NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { DotDeviceListItem, SEO_TILES } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-select-seo-tool',
    imports: [NgClass, NgIf, DecimalPipe, DotMessagePipe],
    templateUrl: './dot-select-seo-tool.component.html',
    styleUrls: ['./dot-select-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectSeoToolComponent implements OnChanges {
    @Input() socialMedia: string;
    @Input() device: DotDeviceListItem;
    socialMediaIconClass: string;
    SOCIAL_MEDIA_TILES = SEO_TILES;

    ngOnChanges() {
        this.socialMediaIconClass = `pi pi-${this.socialMedia?.toLowerCase()}`;
    }
}
