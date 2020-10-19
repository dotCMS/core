import { Component, Input, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotDataTableAction } from '@models/data-table/dot-data-table-action';

/**
 * The ActionMenuButtonComponent is a configurable button with
 * menu component as a pop up
 * @export
 * @class ActionMenuButtonComponent
 */
@Component({
    selector: 'dot-action-menu-button',
    styleUrls: ['./action-menu-button.component.scss'],
    templateUrl: 'action-menu-button.component.html'
})
export class ActionMenuButtonComponent implements OnInit {
    filteredActions: MenuItem[] = [];
    @Input()
    item: any;
    @Input()
    icon? = 'more_vert';
    @Input()
    actions?: DotDataTableAction[];

    ngOnInit() {
        this.filteredActions = this.actions
            .filter(
                (action: DotDataTableAction) =>
                    action.shouldShow ? action.shouldShow(this.item) : true
            )
            .map((action: DotDataTableAction) => {
                return {
                    ...action.menuItem,
                    command: ($event) => {
                        action.menuItem.command(this.item);

                        $event = $event.originalEvent || $event;
                        $event.stopPropagation();
                    }
                };
            });
    }
}
