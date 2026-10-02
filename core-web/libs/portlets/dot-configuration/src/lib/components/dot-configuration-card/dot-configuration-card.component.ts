import { Component, input } from '@angular/core';

import { PanelModule } from 'primeng/panel';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Section card of the Configuration page: a `p-panel` whose header carries an icon, a title and
 * a subtitle. Content placed with the `card-header-end` attribute renders at the right of the
 * header; everything else is the card body.
 */
@Component({
    selector: 'dot-configuration-card',
    imports: [PanelModule, DotMessagePipe],
    templateUrl: './dot-configuration-card.component.html',
    styleUrls: ['./dot-configuration-card.component.scss']
})
export class DotConfigurationCardComponent {
    /** Material Symbols name, e.g. `palette`. */
    readonly icon = input.required<string>();
    readonly titleKey = input.required<string>();
    readonly subtitleKey = input.required<string>();
}
