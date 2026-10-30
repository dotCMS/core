import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotApp } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss'],
    standalone: false
})
export class DotAppsCardComponent {
    @Input() app: DotApp;
    @Output() actionFired = new EventEmitter<string>();
}
