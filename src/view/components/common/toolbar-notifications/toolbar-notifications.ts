import {Component, ViewEncapsulation, ElementRef, Inject} from '@angular/core';


import {MdIcon} from '@angular2-material/icon/icon';
import {MdButton} from '@angular2-material/button/button';
import {MD_CARD_DIRECTIVES} from '@angular2-material/card/card';
import {NotificationsList} from '../notifications/notifications';
import {DotcmsEventsService} from '../../../../api/services/dotcms-events-service';
import {INotification, NotificationsService} from '../../../../api/services/notifications-service';

@Component({
    directives: [MdIcon, MdButton, NotificationsList, MD_CARD_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [DotcmsEventsService, NotificationsService],
    selector: 'dot-toolbar-notifications',
    styleUrls: ['toolbar-notifications.css'],
    templateUrl: ['toolbar-notifications.html'],
    host: {
        '(document:click)': 'handleClick($event)',
    }
})
export class ToolbarNotifications {
    private dotcmsEventsService:DotcmsEventsService;
    private elementRef;
    private isNotificationsMarkedAsRead:boolean = false;
    private notifications:Array<INotification> = [];
    private notificationsUnreadCount:number = 0;
    private notificationService:NotificationsService;
    private showNotifications:boolean = false;
    private i18nMessagesMap:string;

    constructor(@Inject('dotcmsConfig') private dotcmsConfig, _dotcmsEventsService:DotcmsEventsService, _notificationService:NotificationsService, myElement: ElementRef) {
        this.i18nMessagesMap = dotcmsConfig.configParams.config.i18nMessagesMap;
        this.dotcmsEventsService = _dotcmsEventsService;
        this.notificationService = _notificationService;
        this.elementRef = myElement;
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

    private handleClick($event) {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.elementRef.nativeElement) {
                inside = true;
            }
            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.showNotifications = false;
        }
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

    private subscribeToNotifications():void {
        this.dotcmsEventsService.subscribeTo('NOTIFICATION').subscribe((res) => {
            this.notifications.unshift(res.data);
            this.notificationsUnreadCount++;
            this.isNotificationsMarkedAsRead = false;
        });
    }

    private toggleNotifications():void {
        this.showNotifications = !this.showNotifications;

        if (this.showNotifications && !this.isNotificationsMarkedAsRead) {
            this.markAllAsRead();
        }
    }
}