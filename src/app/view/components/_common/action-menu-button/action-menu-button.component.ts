import { Component, Input, ViewEncapsulation, ViewChild, ElementRef, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/primeng';
import { DotDataTableAction } from '../../../../shared/models/data-table/dot-data-table-action';

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
export class ActionMenuButtonComponent implements OnInit {
    filteredActions: MenuItem[];
    @Input() item: any;
    @Input() icon? = 'fa-ellipsis-v';
    @Input() actions?: DotDataTableAction[];

    /**
     * Set action command with content type item as param
     * @param action
     * @param item
     * @param
     */
    handleActionCommand(item, $event): any {
        $event.stopPropagation();
        return this.filteredActions[0].command(item);
    }

    ngOnInit() {
        this.filteredActions = this.actions
            .filter(
                dotDataTableAction => (dotDataTableAction.shouldShow ? dotDataTableAction.shouldShow(this.item) : true)
            )
            .map(dotDataTableAction => dotDataTableAction.menuItem);
    }
}
