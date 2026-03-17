import { LowerCasePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

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
     * Returns true if the notification has a valid LINK action with text and URL.
     *
     * @returns {boolean} True if link action should be displayed
     */
    $showLinkAction = computed(() => {
        const data = this.$data();
        const actions = data?.actions ? data.actions[0] : null;

        return (
            actions &&
            actions?.actionType === 'LINK' &&
            actions?.text !== '' &&
            actions?.action &&
            actions?.action !== ''
        );
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
