import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

import { DotLinkComponent } from '../dot-link/dot-link.component';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotLinkComponent]
})
export class DotApiLinkComponent {
    @Input()
    href: string;
}
