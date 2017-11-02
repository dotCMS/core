import { Component, Input, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { MenuItem } from 'primeng/primeng';

/**
 * The ActionMenuButtonComponent is a configurable button with
 * menu component as a pop up
 * @export
 * @class ActionMenuButtonComponent
 */
@Component({
    selector: 'action-menu-button',
    styleUrls: ['./action-menu-button.component.scss'],
    templateUrl: 'action-menu-button.component.html'
})

export class ActionMenuButtonComponent {
    @Input() actions?: MenuItem[];
    @Input() item: any;
    @Input() icon? = 'fa-ellipsis-v';

    /**
     * Set action command with content type item as param
     * @param action
     * @param item
     * @param
     */
    handleActionCommand(action, item, $event): any {
        $event.stopPropagation();
        return action.command(item);
    }
}
