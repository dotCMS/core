import { Component, Input } from '@angular/core';

import { DotLinkComponent } from '../dot-link/dot-link.component';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    styleUrls: ['./dot-api-link.component.scss'],
    imports: [DotLinkComponent]
})
export class DotApiLinkComponent {
    @Input()
    href: string;
}
