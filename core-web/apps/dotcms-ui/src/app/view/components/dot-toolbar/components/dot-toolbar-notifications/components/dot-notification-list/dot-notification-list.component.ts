import { Component, input, output } from '@angular/core';

import { INotification } from '@models/notifications';

import { DotNotificationItemComponent } from '../dot-notification-item/dot-notification-item.component';

@Component({
    selector: 'dot-notification-list',
    standalone: true,
    imports: [DotNotificationItemComponent],
    styleUrls: ['./dot-notification-list.component.scss'],
    templateUrl: 'dot-notification-list.component.html'
})
export class DotNotificationListComponent {
    $notifications = input<INotification[]>([], { alias: 'notifications' });
    dismissNotification = output<{ id: string }>();

    onClearNotification($event: { id: string }): void {
        this.dismissNotification.emit($event);
    }
}
