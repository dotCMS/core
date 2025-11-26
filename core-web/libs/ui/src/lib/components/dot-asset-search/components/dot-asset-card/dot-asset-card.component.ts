import { CUSTOM_ELEMENTS_SCHEMA, ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    imports: [CardModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
