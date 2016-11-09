import {Injectable} from '@angular/core';
import {CoreWebService} from '../services/core-web-service';
import {Http, RequestMethod} from '@angular/http';
import {ApiRoot} from '../persistence/ApiRoot';
import {Observable} from 'rxjs/Rx';

export interface INotification {
    title: string;
    message: string;
}

@Injectable()
export class NotificationsService {
    private urls: any;

    constructor(private coreWebService: CoreWebService) {
        this.urls = {
            dismissNotificationsUrl: 'v1/notification/delete',
            getNotificationsUrl: 'v1/notification/getNotifications/offset/0/limit/25',
            markAsReadNotificationsUrl: 'v1/notification/markAsRead'
        };
    }

    getNotifications(): Observable<any> {
        return this.coreWebService.request({
            method: RequestMethod.Get,
            url: this.urls.getNotificationsUrl
        });
    }

    dismissNotifications(items: Object): Observable<any>  {
        return this.coreWebService.request({
            body: items,
            method: RequestMethod.Put,
            url: this.urls.dismissNotificationsUrl,
        });
    }

    markAllAsRead(): Observable<any>  {
        return this.coreWebService.request({
            method: RequestMethod.Put,
            url: this.urls.markAsReadNotificationsUrl,
        });
    }

}