import { NgIf } from '@angular/common';
import { Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipeModule } from '@dotcms/ui';

@Component({
    selector: 'dot-link',
    standalone: true,
    imports: [ButtonModule, NgIf, DotMessagePipeModule],
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
