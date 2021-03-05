import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-api-link',
    templateUrl: './dot-api-link.component.html',
    styleUrls: ['./dot-api-link.component.scss']
})
export class DotApiLinkComponent {
    link: string;

    constructor() {}

    @Input('href')
    set href(value: string) {
        this.link = this.getFixedLink(value);
    }

    private getFixedLink(link: string): string {
        return link.startsWith('/') ? link : `/${link}`;
    }
}
