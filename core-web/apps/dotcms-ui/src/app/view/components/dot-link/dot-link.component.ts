import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-link',
    standalone: true,
    templateUrl: './dot-link.component.html',
    styleUrls: ['./dot-link.component.scss']
})
export class DotLinkComponent {
    @Input()
    label: string;

    classNames: string;

    link: string;

    @Input()
    set href(value: string) {
        this.link = this.getFixedLink(value);
    }

    @Input()
    set icon(value: string) {
        this.classNames = `pi ${value}`;
    }

    private getFixedLink(link: string): string {
        return link.startsWith('/') ? link : `/${link}`;
    }
}
