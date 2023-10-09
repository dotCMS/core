import { DecimalPipe, NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { DotDeviceListItem } from '@dotcms/dotcms-models';

import { socialMediaTiles } from '../../../content/services/dot-edit-content-html/models/meta-tags-model';

@Component({
    selector: 'dot-select-seo-tool',
    standalone: true,
    imports: [NgClass, NgIf, DecimalPipe],
    templateUrl: './dot-select-seo-tool.component.html',
    styleUrls: ['./dot-select-seo-tool.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectSeoToolComponent implements OnChanges {
    @Input() socialMedia: string;
    @Input() device: DotDeviceListItem;
    socialMediaIconClass: string;
    socialMediaTiles = socialMediaTiles;

    ngOnChanges() {
        this.socialMediaIconClass = `pi pi-${this.socialMedia?.toLowerCase()}`;
    }
}
