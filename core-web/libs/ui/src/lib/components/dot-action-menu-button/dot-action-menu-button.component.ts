import { NgIf } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionMenuItem } from '@dotcms/dotcms-models';

import { DotMenuComponent } from '../dot-menu/dot-menu.component';

/**
 * The DotActionMenuButtonComponent is a configurable button with
 * menu component as a pop up
 * @export
 * @class DotActionMenuButtonComponent
 */
@Component({
    selector: 'dot-action-menu-button',
    styleUrls: ['./dot-action-menu-button.component.scss'],
    templateUrl: 'dot-action-menu-button.component.html',
    imports: [DotMenuComponent, ButtonModule, TooltipModule, NgIf],
    standalone: true
})
export class DotActionMenuButtonComponent implements OnInit {
    filteredActions: MenuItem[] = [];

    @Input() item: Record<string, unknown>;

    @Input() icon? = 'pi pi-ellipsis-v';

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
