import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentThumbnailComponent } from '../../../dot-content-thumbnail/dot-content-thumbnail.component';
import { DotContentletStatusBadgeComponent } from '../../../dot-contentlet-status-badge/dot-contentlet-status-badge.component';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    imports: [Card, Tag, DotContentletStatusBadgeComponent, DotContentThumbnailComponent],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardComponent {
    readonly contentlet = input.required<DotCMSContentlet>();

    /**
     * `language` on a contentlet is either a plain code or a full `DotLanguage`. The template
     * needs a string, and passing the object rendered as "[object Object]".
     */
    get languageLabel(): string {
        const language = this.contentlet()?.language;

        if (!language) {
            return '';
        }

        return typeof language === 'string' ? language : (language.isoCode ?? '');
    }
}
