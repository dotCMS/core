import { Component, Input } from '@angular/core';

@Component({
    selector: 'icon-button-tooltip',
    template: `<button pButton [icon]="icon" pTooltip="{{tooltipText}}" tooltipPosition="bottom"></button>`
})
export class IconButtonTooltipComponent {
    @Input() icon: string;
    @Input() tooltipText: string;

    constructor() { }
}
