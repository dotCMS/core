import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';

import { INotification, NotificationIcons } from '@models/notifications';
@Component({
    selector: 'dot-notifications-item',
    styleUrls: ['./dot-notifications-item.component.scss'],
    templateUrl: 'dot-notifications-item.component.html'
})
export class DotNotificationsItemComponent implements OnInit {
    @Input() data;

    @Output() clear = new EventEmitter<Record<string, unknown>>();

    showLinkAction = false;
    showTitleLinked = false;

    private notificationIcons: NotificationIcons = {
        ERROR: 'exclamation-triangle',
        INFO: 'info-circle',
        WARNING: 'ban'
    };

    ngOnInit(): void {
        // TODO: hand more than one action
        const actions = this.data.actions ? this.data.actions[0] : null;
        this.showLinkAction =
            actions &&
            actions.actionType === 'LINK' &&
            (actions.text || actions.text !== '') &&
            actions.action &&
            actions.action !== '';

        this.showTitleLinked =
            actions &&
            actions.actionType === 'LINK' &&
            (!actions.text || actions.text === '') &&
            actions.action &&
            actions.action !== '';
    }

    getIconName(val: string): string {
        return 'notification-item__icon pi pi-' + this.notificationIcons[val];
    }

    onClear(): void {
        this.clear.emit({
            id: this.data.id
        });
    }
}

@Component({
    selector: 'dot-notifications-list',
    styleUrls: ['./dot-notifications-list.component.scss'],
    templateUrl: 'dot-notifications-list.component.html'
})
export class DotNotificationsListComponent {
    @Input() notifications: INotification[];
    @Output() dismissNotification = new EventEmitter<Record<string, unknown>>();

    onClearNotification($event: Record<string, unknown>): void {
        this.dismissNotification.emit($event);
    }
}
