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
     * Indicates whether the current license is an enterprise license.
     *
     * This flag is used to determine if enterprise-only features should be enabled.
     * When false, items marked as needsEnterpriseLicense will be disabled in the navigation bar.
     *
     * @type {boolean}
     * @default false
     */
    @Input() isEnterpriseLicense = false;

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

    /**
     * Determines if a navigation bar item should be disabled.
     *
     * An item is considered disabled if:
     * 1. It is explicitly marked as disabled (item.isDisabled is true), or
     * 2. It needs an enterprise license (item.needsEnterpriseLicense is true) and the current license is not an enterprise license.
     *
     * @param {NavigationBarItem} item - The navigation bar item to check.
     * @returns {boolean} True if the item should be disabled, false otherwise.
     */
    isItemDisabled(item: NavigationBarItem): boolean {
        return item.isDisabled || (item.needsEnterpriseLicense && !this.isEnterpriseLicense);
    }
}
