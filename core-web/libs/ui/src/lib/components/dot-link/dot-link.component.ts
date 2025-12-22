import { Component, Input } from '@angular/core';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-link',
    imports: [DotMessagePipe],
    templateUrl: './dot-link.component.html',
    styleUrls: ['./dot-link.component.css']
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
