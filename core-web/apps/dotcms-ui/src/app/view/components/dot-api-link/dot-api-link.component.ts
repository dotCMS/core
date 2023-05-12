import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    styleUrls: ['./dot-api-link.component.scss']
})
export class DotApiLinkComponent {
    @Input()
    href: string;
}
