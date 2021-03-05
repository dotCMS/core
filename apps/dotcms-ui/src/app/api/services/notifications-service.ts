import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';

@Injectable()
export class NotificationsService {
    private urls: any;

    constructor(private coreWebService: CoreWebService) {
        this.urls = {
            dismissNotificationsUrl: 'v1/notification/delete',
            getLastNotificationsUrl: 'v1/notification/getNotifications/offset/0/limit/24',
            getNotificationsUrl: 'v1/notification/getNotifications/',
            markAsReadNotificationsUrl: 'v1/notification/markAsRead'
        };
    }

    getLastNotifications(): Observable<any> {
        return this.coreWebService.request({
            url: this.urls.getLastNotificationsUrl
        });
    }

    getAllNotifications(): Observable<any> {
        return this.coreWebService.request({
            url: this.urls.getNotificationsUrl
        });
    }

    dismissNotifications(items: Object): Observable<any> {
        return this.coreWebService.request({
            body: items,
            method: 'PUT',
            url: this.urls.dismissNotificationsUrl
        });
    }

    markAllAsRead(): Observable<any> {
        return this.coreWebService.request({
            method: 'PUT',
            url: this.urls.markAsReadNotificationsUrl
        });
    }
}
