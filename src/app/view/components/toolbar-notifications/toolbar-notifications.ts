import { Component, ViewEncapsulation, ElementRef, ViewChild } from '@angular/core';

import { BaseComponent } from '../_common/_base/base-component';
import { DotDropdownComponent } from '../_common/dropdown-component/dot-dropdown.component';
import { DotcmsEventsService, LoginService } from 'dotcms-js/dotcms-js';
import { INotification } from '../../../shared/models/notifications';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { NotificationsService } from '../../../api/services/notifications-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-toolbar-notifications',
    styleUrls: ['./toolbar-notifications.scss'],
    templateUrl: 'toolbar-notifications.html'
})
export class ToolbarNotifications extends BaseComponent {
    @ViewChild(DotDropdownComponent) dropdown: DotDropdownComponent;
    private elementRef;
    private isNotificationsMarkedAsRead = false;
    private notifications: Array<INotification> = [];
    private notificationsUnreadCount = 0;
    private showNotifications = false;
    private existsMoreToLoad = false;

    constructor(
        private dotcmsEventsService: DotcmsEventsService,
        private notificationService: NotificationsService,
        myElement: ElementRef,
        dotMessageService: DotMessageService,
        private loginService: LoginService,
        private iframeOverlayService: IframeOverlayService
    ) {
        super(
            ['notifications_dismissall', 'notifications_title', 'notifications_load_more'],
            dotMessageService
        );
        this.elementRef = myElement;
    }

    ngOnInit(): void {
        this.getNotifications();
        this.subscribeToNotifications();

        this.loginService.watchUser(this.getNotifications.bind(this));
    }

    private clearNotitications(): void {
        this.notifications = [];
        this.notificationsUnreadCount = 0;
        this.showNotifications = false;
        this.dropdown.closeIt();
    }

    // tslint:disable-next-line:no-unused-variable
    private dismissAllNotifications(): void {
        const items = this.notifications.map(item => item.id);
        this.notificationService.dismissNotifications({ items: items }).subscribe(res => {
            // TODO: I think we should get here res and err
            if (res.errors.length) {
                return;
            }

            this.clearNotitications();
        });
    }

    private getNotifications(): void {
        this.notificationService.getLastNotifications().subscribe(res => {
            this.notificationsUnreadCount = res.entity.totalUnreadNotifications;
            this.notifications = res.entity.notifications;
            this.existsMoreToLoad = res.entity.total > res.entity.notifications.length;
        });
    }

    // tslint:disable-next-line:no-unused-variable
    private loadMore(): void {
        this.notificationService.getAllNotifications().subscribe(res => {
            this.notificationsUnreadCount = res.entity.count;
            this.notifications = res.entity.notifications;
            this.existsMoreToLoad = false;
        });
    }

    private markAllAsRead(): void {
        this.notificationService.markAllAsRead().subscribe(res => {
            this.isNotificationsMarkedAsRead = true;
            this.notificationsUnreadCount = 0;
        });
    }

    // tslint:disable-next-line:no-unused-variable
    private onDismissNotification($event): void {
        const notificationId = $event.id;

        this.notificationService
            .dismissNotifications({ items: [notificationId] })
            .subscribe(res => {
                if (res.errors.length) {
                    return;
                }

                this.notifications = this.notifications.filter(item => {
                    return item.id !== notificationId;
                });

                if (this.notificationsUnreadCount) {
                    this.notificationsUnreadCount--;
                }

                if (!this.notifications.length && !this.notificationsUnreadCount) {
                    this.clearNotitications();
                }
            });
    }

    private subscribeToNotifications(): void {
        this.dotcmsEventsService.subscribeTo('NOTIFICATION').subscribe(res => {
            this.notifications.unshift(res.data);
            this.notificationsUnreadCount++;
            this.isNotificationsMarkedAsRead = false;
        });
    }

    // tslint:disable-next-line:no-unused-variable
    private toggleNotifications(): void {
        this.showNotifications = !this.showNotifications;

        if (this.showNotifications && !this.isNotificationsMarkedAsRead) {
            this.markAllAsRead();
        }
    }
}
