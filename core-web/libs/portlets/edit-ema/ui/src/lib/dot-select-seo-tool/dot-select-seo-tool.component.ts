import { DecimalPipe, NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { DotDeviceListItem, SEO_MEDIA_TYPES, SEO_TILES } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-select-seo-tool',
    imports: [NgClass, DecimalPipe, DotMessagePipe],
    templateUrl: './dot-select-seo-tool.component.html',
    styleUrls: ['./dot-select-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectSeoToolComponent implements OnChanges {
    @Input() socialMedia = '';
    @Input() device!: DotDeviceListItem;
    socialMediaIconClass = '';

    /**
     * Tile for the current social media, or undefined when there is none.
     *
     * `socialMedia` arrives as a plain string (the store types it `string | null`) while
     * `SEO_TILES` is keyed by `SEO_MEDIA_TYPES`, so the lookup can miss. Keeping the cast here
     * means the template does not have to index an exhaustive Record with an arbitrary key —
     * the `?.` it already used said as much.
     */
    protected get tile(): (typeof SEO_TILES)[SEO_MEDIA_TYPES] | undefined {
        return SEO_TILES[this.socialMedia as SEO_MEDIA_TYPES];
    }

    ngOnChanges() {
        this.socialMediaIconClass = `pi pi-${this.socialMedia?.toLowerCase()}`;
    }
}
