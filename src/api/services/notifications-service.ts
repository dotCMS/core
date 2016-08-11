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
    private getNotificationsUrl:string;
    private dismissNotificationsUrl:string;
    private markAsReadNotificationsUrl:string;

    constructor(_apiRoot: ApiRoot, _http: Http) {
        super(_apiRoot, _http);
        this.getNotificationsUrl = `${_apiRoot.baseUrl}api/v1/notification/getNotifications/offset/0/limit/25`;
        this.dismissNotificationsUrl = `${_apiRoot.baseUrl}api/v1/notification/delete`;
        this.markAsReadNotificationsUrl = `${_apiRoot.baseUrl}api/v1/notification/markAsRead`;
    }

    getNotifications() {
        return this.request({
            url: this.getNotificationsUrl,
            method: RequestMethod.Get
        });
    }

    dismissNotifications(items:Object) {
        return this.request({
            url: this.dismissNotificationsUrl,
            method: RequestMethod.Put,
            body: items
        });
    }

    markAllAsRead() {
        return this.request({
            url: this.markAsReadNotificationsUrl,
            method: RequestMethod.Put
        });
    }

}