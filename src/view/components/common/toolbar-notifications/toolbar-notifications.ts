import {BaseComponent} from '../_base/base-component';
import {Component, ViewEncapsulation, ElementRef} from '@angular/core';
import {DotcmsEventsService} from '../../../../api/services/dotcms-events-service';
import {DropdownComponent} from '../dropdown-component/dropdown-component';
import {INotification, NotificationsService} from '../../../../api/services/notifications-service';
import {MD_CARD_DIRECTIVES} from '@angular2-material/card/card';
import {MdButton} from '@angular2-material/button/button';
import {MdIcon} from '@angular2-material/icon/icon';
import {MessageService} from '../../../../api/services/messages-service';
import {NotificationsList} from '../notifications/notifications';

@Component({
    directives: [MdIcon, MdButton, NotificationsList, MD_CARD_DIRECTIVES, DropdownComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [DotcmsEventsService, NotificationsService],
    selector: 'dot-toolbar-notifications',
    styleUrls: ['toolbar-notifications.css'],
    templateUrl: ['toolbar-notifications.html']
})
export class ToolbarNotifications extends BaseComponent{
    private dotcmsEventsService: DotcmsEventsService;
    private elementRef;
    private isNotificationsMarkedAsRead: boolean = false;
    private notifications: Array<INotification> = [];
    private notificationService: NotificationsService;
    private notificationsUnreadCount: number = 0;
    private showNotifications: boolean = false;


    constructor(_dotcmsEventsService: DotcmsEventsService, _notificationService: NotificationsService,
                myElement: ElementRef, private messageService: MessageService) {
        super(['notifications_dismissall', 'notifications_title'], messageService);
        this.dotcmsEventsService = _dotcmsEventsService;
        this.elementRef = myElement;
        this.notificationService = _notificationService;
    }

    ngOnInit() {
        this.getNotifications();
        this.subscribeToNotifications();
    }

    private clearNotitications() {
        this.notifications = [];
        this.notificationsUnreadCount = 0;
        this.showNotifications = false;
    }

    private dismissAllNotifications():void {
        let items = this.notifications.map(item => item.id);
        this.notificationService.dismissNotifications({'items': items}).subscribe(res => {
            // TODO: I think we should get here res and err
            if (res.errors.length) {
                return;
            }

            this.clearNotitications();
        });
    }

    private getNotifications() {
        this.notificationService.getNotifications().subscribe(res => {
            this.notificationsUnreadCount = res.entity.count;
            this.notifications = res.entity.notifications;
        });
    }

    private markAllAsRead():void {
        this.notificationService.markAllAsRead().subscribe(res => {
            this.isNotificationsMarkedAsRead = true;
            this.notificationsUnreadCount = 0;
        });

    }

    private onDismissNotification($event):void {
        let notificationId = $event.id;

        this.notificationService.dismissNotifications({"items": [notificationId]}).subscribe(res => {
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