import { ChangeDetectionStrategy, Component, Input, NO_ERRORS_SCHEMA } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    standalone: true,
    imports: [CardModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [NO_ERRORS_SCHEMA] // WebComponent
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
