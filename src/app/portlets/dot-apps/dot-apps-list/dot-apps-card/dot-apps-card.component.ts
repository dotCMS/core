import { Component, Input, Output, EventEmitter } from '@angular/core';

import { DotApps } from '@shared/models/dot-apps/dot-apps.model';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss']
})
export class DotAppsCardComponent {
    @Input() app: DotApps;
    @Output() actionFired = new EventEmitter<string>();

    constructor() {}
}
