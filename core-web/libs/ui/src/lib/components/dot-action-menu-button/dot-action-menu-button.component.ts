import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { MenuItem, MenuItemCommandEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

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
    imports: [DotMenuComponent, ButtonModule, TooltipModule],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotActionMenuButtonComponent {
    $item = input<Record<string, unknown>>({});
    $icon = input('pi pi-ellipsis-v', {
        alias: 'icon'
    });
    $actions = input<MenuItem[]>([], {
        alias: 'actions'
    });

    $filteredActions = computed(() => {
        const actions = this.$actions();
        const item = this.$item();

        return (
            actions
                /*.filter((action: MenuItem) =>
            action.shouldShow ? action.shouldShow(item) : true
        )*/
                .map((action: MenuItem) => ({
                    ...action,
                    command: (event: MenuItemCommandEvent) => {
                        console.log('command', event);
                        const { item, originalEvent } = event;
                        action.command({ originalEvent, item });
                    }
                }))
        );
    });
}
