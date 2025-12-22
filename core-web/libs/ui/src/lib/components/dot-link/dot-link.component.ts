import { Component, computed, input } from '@angular/core';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-link',
    imports: [DotMessagePipe],
    templateUrl: './dot-link.component.html',
    styleUrls: ['./dot-link.component.css']
})
export class DotLinkComponent {
    label = input<string>();
    href = input<string>();
    icon = input<string>();

    link = computed(() => {
        const hrefValue = this.href();
        return hrefValue ? this.getFixedLink(hrefValue) : '';
    });

    classNames = computed(() => {
        const iconValue = this.icon();
        return iconValue ? `pi ${iconValue}` : '';
    });

    private getFixedLink(link: string): string {
        return link.startsWith('/') ? link : `/${link}`;
    }
}
