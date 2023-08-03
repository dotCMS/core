import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-preview-link',
    templateUrl: './dot-preview-link.component.html',
    styleUrls: ['./dot-preview-link.component.scss']
})
export class DotPreviewLinkComponent {
    link: string;

    @Input()
    set href(value: string) {
        this.link = this.getFixedLink(value);
    }

    private getFixedLink(link: string): string {
        return link.startsWith('/') ? link : `/${link}`;
    }
}
