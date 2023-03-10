import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotApps } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss']
})
export class DotAppsCardComponent {
    @Input() app: DotApps;
    @Output() actionFired = new EventEmitter<string>();
}
