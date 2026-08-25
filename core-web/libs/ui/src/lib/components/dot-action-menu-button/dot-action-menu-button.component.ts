import { Component, OnInit, signal, ChangeDetectionStrategy, input } from '@angular/core';

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
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotMenuComponent, ButtonModule, TooltipModule]
})
export class DotActionMenuButtonComponent implements OnInit {
    filteredActions: CustomMenuItem[] = [];

    readonly item = input.required<Record<string, unknown>>();

    // Always has a default, so it is never undefined — the `?` made it `string | undefined`
    // and broke every consumer that types `icon` as `string`.
    readonly icon = input('pi pi-ellipsis-v');

    readonly actions = input<DotActionMenuItem[]>();

    $hasIcon = signal(false);

    ngOnInit() {
        this.filteredActions = (this.actions() ?? [])
            .filter((action: DotActionMenuItem) =>
                action.shouldShow ? action.shouldShow(this.item()) : true
            )
            .map((action: DotActionMenuItem) => {
                return {
                    ...action.menuItem,
                    command: ($event: DotActionMenuClickEvent) => {
                        action.menuItem.command?.(this.item());

                        $event?.originalEvent?.stopPropagation();
                    }
                };
            });

        if (this.filteredActions.length === 1) {
            this.$hasIcon.set(this.filteredActions[0]['icon'] ? true : false);
        }
    }
}
