import { Component, Input, ViewEncapsulation } from '@angular/core';
import { MenuItem } from 'primeng/primeng';

/**
 * The ActionButtonComponent is a configurable button with
 * options to add to the primary actions in the portlets.
 * @export
 * @class ActionButtonComponent
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'action-button',
    styles: [require('./action-button.component.scss')],
    templateUrl: 'action-button.component.html'
})

export class ActionButtonComponent {
    @Input() options: MenuItem[];
    @Input() primaryAction: Function;
}