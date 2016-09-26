import {Injectable} from '@angular/core';
import {CoreWebService} from '../services/core-web-service';
import {Http, RequestMethod} from '@angular/http';
import {ApiRoot} from '../persistence/ApiRoot';

export interface INotification {
    title:string,
    message:string
}

@Injectable()
export class NotificationsService extends CoreWebService {
    private urls: any;

    constructor(_apiRoot: ApiRoot, _http: Http) {
        super(_apiRoot, _http);
        this.urls = {
            getNotificationsUrl: 'v1/notification/getNotifications/offset/0/limit/25',
            dismissNotificationsUrl: 'v1/notification/delete',
            markAsReadNotificationsUrl: 'v1/notification/markAsRead'
        };
    }

    getNotifications() {
        return this.request({
            url: this.urls.getNotificationsUrl,
            method: RequestMethod.Get
        });
    }

    dismissNotifications(items:Object) {
        return this.request({
            url: this.urls.dismissNotificationsUrl,
            method: RequestMethod.Put,
            body: items
        });
    }

    markAllAsRead() {
        return this.request({
            url: this.urls.markAsReadNotificationsUrl,
            method: RequestMethod.Put
        });
    }

}