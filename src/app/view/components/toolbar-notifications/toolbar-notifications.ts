import {BaseComponent} from '../_common/_base/base-component';
import {Component, ViewEncapsulation, ElementRef, ViewChild } from '@angular/core';
import {DotcmsEventsService} from '../../../api/services/dotcms-events-service';
import {INotification, NotificationsService} from '../../../api/services/notifications-service';
import {MessageService} from '../../../api/services/messages-service';
import {LoginService} from '../../../api/services/login-service';
import {IframeOverlayService} from '../../../api/services/iframe-overlay-service';
import {DropdownComponent} from '../_common/dropdown-component/dropdown-component';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-toolbar-notifications',
    styles: [require('./toolbar-notifications.scss')],
    templateUrl: 'toolbar-notifications.html'
})
export class ToolbarNotifications extends BaseComponent {
    private static readonly MAX_NOTIFICATIONS_TO_SHOW = 25;

    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    private elementRef;
    private isNotificationsMarkedAsRead = false;
    private notifications: Array<INotification> = [];
    private notificationsUnreadCount = 0;
    private showNotifications = false;
    private existsMoreToLoad = false;

    constructor(private dotcmsEventsService: DotcmsEventsService, private notificationService: NotificationsService,
                myElement: ElementRef, messageService: MessageService, private loginService: LoginService,
                private iframeOverlayService: IframeOverlayService) {
        super(['notifications_dismissall', 'notifications_title', 'notifications_load_more'], messageService);
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

    private dismissAllNotifications(): void {
        let items = this.notifications.map(item => item.id);
        this.notificationService.dismissNotifications({'items': items}).subscribe(res => {
            // TODO: I think we should get here res and err
            if (res.errors.length) {
                return;
            }

            this.clearNotitications();

        });
    }

    private getNotifications(): void {
        this.notificationService.getLastNotifications().subscribe(res => {
            this.notificationsUnreadCount = res.entity.count;
            this.notifications = res.entity.notifications;
            this.existsMoreToLoad = true;
        });
    }

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

    private onDismissNotification($event): void {
        let notificationId = $event.id;

        this.notificationService.dismissNotifications({items: [notificationId]}).subscribe(res => {
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
        this.dotcmsEventsService.subscribeTo('NOTIFICATION').subscribe((res) => {
            this.notifications.unshift(res.data);
            this.notificationsUnreadCount++;
            this.isNotificationsMarkedAsRead = false;
        });
    }

    private toggleNotifications(): void {
        this.showNotifications = !this.showNotifications;

        if (this.showNotifications && !this.isNotificationsMarkedAsRead) {
            this.markAllAsRead();
        }
    }
}