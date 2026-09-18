import { LowerCasePipe } from '@angular/common';
import { Component, computed, input, output, ChangeDetectionStrategy } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import {
    INotification,
    NotificationIcons
} from '../../../../../../../shared/models/notifications/notification.model';
import { CustomTimeComponent } from '../../../../../_common/dot-custom-time.component/dot-custom-time.component';

/** Map of notification types to their corresponding PrimeNG icon classes */
const notificationIcons: NotificationIcons = {
    ERROR: 'exclamation-triangle',
    INFO: 'info-circle',
    WARNING: 'ban'
};

/**
 * Component that displays individual notification items in the toolbar notifications panel.
 * Handles rendering notification content, icons, actions, and clear functionality.
 *
 * @example
 * ```html
 * <dot-notification-item
 *   [data]="notification"
 *   (clear)="onNotificationClear($event)">
 * </dot-notification-item>
 * ```
 */
@Component({
    selector: 'dot-notification-item',
    imports: [ButtonModule, CustomTimeComponent, LowerCasePipe, DotMessagePipe],
    styleUrls: ['./dot-notification-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    templateUrl: 'dot-notification-item.component.html'
})
export class DotNotificationItemComponent {
    /**
     * Required input containing the notification data to display.
     * Includes notification content, level, actions, and metadata.
     */
    $data = input.required<INotification>({ alias: 'data' });

    /**
     * Event emitted when the user clicks the clear/dismiss button.
     * Emits an object containing the notification ID to be cleared.
     */
    clear = output<{ id: string }>();

    /**
     * Computed property that determines whether to show the link action button.
     * The notification's LINK action, or null when there is none to show.
     *
     * Returns the action itself rather than a boolean so the template renders the very object
     * this validated. It previously guarded on `data.actions` while the template read
     * `data.notificationData.actions` — the same list by way of a derived getter on the server,
     * but through an untyped `Record<string, unknown>` bag.
     *
     * @returns {INotification['actions'][number] | null} The link action, or null
     */
    $linkAction = computed(() => {
        const action = this.$data()?.actions?.[0];

        if (!action || action.actionType !== 'LINK' || !action.text || !action.action) {
            return null;
        }

        return action;
    });

    /**
     * Computed property that returns the appropriate CSS class name for the notification icon.
     * Maps notification level (ERROR, INFO, WARNING) to corresponding PrimeNG icon classes.
     *
     * @returns {string} CSS class string for the notification icon, or empty string if no icon found
     */
    $getIconName = computed(() => {
        const data = this.$data();

        const iconName = notificationIcons[data.level];

        if (!iconName) {
            return '';
        }

        return 'notification-item__icon pi pi-' + iconName;
    });

    /**
     * Handles the clear/dismiss action when user clicks the clear button.
     * Emits the clear event with the notification ID to parent component.
     */
    onClear(): void {
        const data = this.$data();
        this.clear.emit({ id: data.id });
    }
}
