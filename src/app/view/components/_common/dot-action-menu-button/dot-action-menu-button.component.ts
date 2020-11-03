import { Component, Input, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotActionMenuItem } from '@shared/models/dot-action-menu/dot-action-menu-item.model';

/**
 * The DotActionMenuButtonComponent is a configurable button with
 * menu component as a pop up
 * @export
 * @class DotActionMenuButtonComponent
 */
@Component({
    selector: 'dot-action-menu-button',
    styleUrls: ['./dot-action-menu-button.component.scss'],
    templateUrl: 'dot-action-menu-button.component.html'
})
export class DotActionMenuButtonComponent implements OnInit {
    filteredActions: MenuItem[] = [];

    @Input() item: any;

    @Input() icon? = 'more_vert';

    @Input() actions?: DotActionMenuItem[];

    ngOnInit() {
        this.filteredActions = this.actions
            .filter((action: DotActionMenuItem) =>
                action.shouldShow ? action.shouldShow(this.item) : true
            )
            .map((action: DotActionMenuItem) => {
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
