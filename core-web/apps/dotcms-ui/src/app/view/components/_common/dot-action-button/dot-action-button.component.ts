import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    output,
    viewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

/**
 * The ActionButtonComponent is a configurable button with
 * options to add to the primary actions in the portlets.
 * @export
 * @class ActionButtonComponent
 */
@Component({
    selector: 'dot-action-button',
    templateUrl: 'dot-action-button.component.html',
    imports: [ButtonModule, MenuModule, NgClass],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.action-button--no-label]': '$isNotLabeled()',
        '(click)': 'onHostClick($event)',
        class: 'inline-flex flex-col items-center'
    }
})
export class DotActionButtonComponent {
    $menu = viewChild<Menu>('menu');

    disabled = input<boolean>(false);
    icon = input<string>('pi pi-plus');
    label = input<string>('');
    model = input<MenuItem[]>([]);

    press = output<MouseEvent>();

    $isNotLabeled = computed(() => !this.label());
    $isHaveOptions = computed(() => !!(this.model() && this.model().length));

    /**
     * Handle the click to the main button
     *
     * @param {MouseEvent} $event
     * @memberof DotActionButtonComponent
     */
    buttonOnClick($event: MouseEvent): void {
        this.$isHaveOptions() ? this.$menu()?.toggle($event) : this.press.emit($event);
    }

    /**
     * Stop propagation for host click
     *
     * @param {MouseEvent} event
     * @memberof DotActionButtonComponent
     */
    onHostClick(event: MouseEvent): void {
        event.stopPropagation();
    }
}
