import { Component, Input, OnInit, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { CustomMenuItem, DotActionMenuItem } from '@dotcms/dotcms-models';

import { DotMenuComponent } from '../dot-menu/dot-menu.component';

interface DotActionMenuClickEvent {
    item: MenuItem;
    originalEvent: MouseEvent;
}

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
    imports: [DotMenuComponent, ButtonModule, TooltipModule],
    standalone: true
})
export class DotActionMenuButtonComponent implements OnInit {
    filteredActions: CustomMenuItem[] = [];

    @Input() item: Record<string, unknown>;

    @Input() icon? = 'pi pi-ellipsis-v';

    @Input() actions?: DotActionMenuItem[];

    $hasIcon = signal(false);

    ngOnInit() {
        this.filteredActions = this.actions
            .filter((action: DotActionMenuItem) =>
                action.shouldShow ? action.shouldShow(this.item) : true
            )
            .map((action: DotActionMenuItem) => {
                return {
                    ...action.menuItem,
                    command: ($event: DotActionMenuClickEvent) => {
                        action.menuItem.command(this.item);

                        $event.originalEvent.stopPropagation();
                    }
                };
            });

        if (this.filteredActions.length === 1) {
            this.$hasIcon.set(this.filteredActions[0].icon ? true : false);
        }
    }
}
