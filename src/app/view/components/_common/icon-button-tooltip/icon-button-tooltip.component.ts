import { Component, Input } from '@angular/core';

@Component({
    selector: 'icon-button-tooltip',
    styles: [':host { cursor: pointer; }'],
    template: `
        <span pTooltip="{{tooltipText}}" tooltipPosition="bottom">
            <i class="fa {{icon}}"></i>
        </span>
    `,
})
export class IconButtonTooltipComponent {
    @Input() icon: string;
    @Input() tooltipText: string;

    constructor() { }
}
