import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-icon-button-tooltip',
    template: `<dot-icon-button
        [icon]="icon"
        [pTooltip]="tooltipText"
        tooltipPosition="bottom"
    ></dot-icon-button>`
})
export class DotIconButtonTooltipComponent {
    @Input()
    icon: string;

    @Input()
    tooltipText: string;

    constructor() {}
}
