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
    selector: 'dot-action-button',
    styleUrls: ['./dot-action-button.component.scss'],
    templateUrl: 'dot-action-button.component.html'
})

export class DotActionButtonComponent {
    @Input() model?: MenuItem[];
    @Input() command?: ($event) => void;
    @Input() label: string;
    @Input() flat: boolean;
}
