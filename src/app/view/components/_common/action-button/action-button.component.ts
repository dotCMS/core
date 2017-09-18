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
    styleUrls: ['./action-button.component.scss'],
    templateUrl: 'action-button.component.html'
})

export class ActionButtonComponent {
    @Input() model?: MenuItem[];
    @Input() command?: ($event) => void;
    @Input() label: string;
    @Input() flat: boolean;
}
