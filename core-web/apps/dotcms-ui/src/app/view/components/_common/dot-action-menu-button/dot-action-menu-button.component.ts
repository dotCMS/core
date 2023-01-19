/* eslint-disable no-console */
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';

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
export class DotActionMenuButtonComponent implements OnInit, OnChanges {
    filteredActions: MenuItem[] = [];

    @Input() lazy? = false;

    @Input() item: Record<string, unknown>;

    @Input() icon? = 'more_vert';

    @Input() actions?: DotActionMenuItem[];

    @Output() actionMenuStatus: EventEmitter<Record<string, unknown>> = new EventEmitter();

    ngOnInit() {
        console.log('init');
        // if (this.actions) {
        //     this.filteredActions = this.actions
        //         .filter((action: DotActionMenuItem) =>
        //             action.shouldShow ? action.shouldShow(this.item) : true
        //         )
        //         .map((action: DotActionMenuItem) => {
        //             return {
        //                 ...action.menuItem,
        //                 command: ($event) => {
        //                     action.menuItem.command(this.item);

        //                     $event = $event.originalEvent || $event;
        //                     $event.stopPropagation();
        //                 }
        //             };
        //         });
        // }
    }

    ngOnChanges(changes: SimpleChanges): void {
        console.log('==DotActionMenuButtonComponent', changes);
        console.log('**changes.actions?.currentValue', changes.actions?.currentValue);
        if (changes.actions?.currentValue) {
            this.actions = changes.actions.currentValue;
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

    handleMenuStatus(menuVisible: boolean) {
        this.actionMenuStatus.emit({
            visible: menuVisible,
            ...this.item
        });
    }
}
