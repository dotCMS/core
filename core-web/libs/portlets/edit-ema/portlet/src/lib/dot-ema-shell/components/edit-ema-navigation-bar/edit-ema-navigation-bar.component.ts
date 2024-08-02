import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { NavigationBarItem } from '../../../shared/models';
@Component({
    selector: 'dot-edit-ema-navigation-bar',
    standalone: true,
    templateUrl: './edit-ema-navigation-bar.component.html',
    styleUrls: ['./edit-ema-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonModule, RouterModule, DotMessagePipe, TooltipModule]
})
export class EditEmaNavigationBarComponent {
    /**
     * List of items to display on the navigation bar
     *
     * @type {NavigationBarItem[]}
     * @memberof EditEmaNavigationBarComponent
     */
    @Input() items: NavigationBarItem[];

    /**
     * Emits the id of the clicked item
     *
     * @type {EventEmitter<string>}
     * @memberof EditEmaNavigationBarComponent
     */
    @Output() action: EventEmitter<string> = new EventEmitter();

    /**
     * Handle the click event on the item
     *
     * @param {NavigationBarItem} item
     * @memberof EditEmaNavigationBarComponent
     */
    itemAction(item: NavigationBarItem): void {
        this.action.emit(item.id);
    }
}
