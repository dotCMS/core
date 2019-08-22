import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';

import { INotification } from '@models/notifications';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    selector: 'dot-notifications-item',
    styleUrls: ['./dot-notifications-item.component.scss'],
    templateUrl: 'dot-notifications-item.component.html'
})
export class DotNotificationsItemComponent implements OnInit {
    @Input()
    data;

    @Output()
    clear = new EventEmitter<Object>();

    showLinkAction = false;
    showTitleLinked = false;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private notificationIcons: Object = {
        ERROR: 'exclamation-triangle',
        INFO: 'info-circle',
        WARNING: 'ban'
    };

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.dotMessageService.getMessages(['notifications_dismiss']).subscribe((res) => {
            this.i18nMessages = res;
        });

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
    @Output() dismissNotification = new EventEmitter<Object>();

    constructor() {}

    onClearNotification($event): void {
        this.dismissNotification.emit($event);
    }
}
