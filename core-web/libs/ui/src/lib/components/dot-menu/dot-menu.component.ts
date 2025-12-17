import { Component, ViewChild, computed, input } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

/**
 * Custom Menu to display options as a pop-up.
 *
 * @export
 * @class DotMenuComponent
 */
@Component({
    selector: 'dot-menu',
    templateUrl: './dot-menu.component.html',
    imports: [ButtonModule, MenuModule]
})
export class DotMenuComponent {
    $icon = input<string>('pi pi-ellipsis-v', { alias: 'icon' });

    $model = input.required<MenuItem[]>({ alias: 'model' });

    $float = input<boolean>(false, { alias: 'float' });

    @ViewChild('menu', { static: true })
    menu: Menu;

    // computed style class based on the float input
    $styleClass = computed(() =>
        this.$float()
            ? 'p-button-sm p-button-rounded'
            : 'p-button-sm p-button-rounded p-button-text'
    );

    /**
     * Toggle the visibility of the menu options
     *
     * @param {MouseEvent} $event
     *
     * @memberof DotMenuComponent
     */
    toggle($event: MouseEvent): void {
        $event.stopPropagation();
        this.menu.toggle($event);
    }
}
