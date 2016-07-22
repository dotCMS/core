import {Component, ViewEncapsulation} from '@angular/core';
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
})
export class ToolbarNotifications {
    private showNotifications:boolean = false;
    private notifications:Array<INotification> = [];
    private dotcmsEventsService:DotcmsEventsService
    private notificationService:NotificationsService

    constructor(_dotcmsEventsService:DotcmsEventsService, _notificationService:NotificationsService) {
        this.dotcmsEventsService = _dotcmsEventsService;
        this.notificationService = _notificationService;
    }

    subscribeToNotifications():void {
        this.dotcmsEventsService.subscribeTo('NOTIFICATION').subscribe((res) => {
            this.notifications.push(res.data.notificationData);
        });
    }

    getNotifications() {
        this.notificationService.getNotifications().subscribe(res => {
            this.notifications = res.entity.map(data => data.notificationData)
        });
    }

    ngOnInit() {
        this.getNotifications()
        this.subscribeToNotifications();
    }

    toggleNotifications():void {
        this.showNotifications = !this.showNotifications;
    }
}