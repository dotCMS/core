import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-icon-button-tooltip',
    template: ``
})
export class UiDotIconButtonTooltipComponent {
    @Input()
    icon: string;

    @Input()
    tooltipText: string;

    @Input()
    tooltipPosition = 'bottom';
}
