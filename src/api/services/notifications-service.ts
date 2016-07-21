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
    private notificationsUrl:string;

    constructor(_apiRoot: ApiRoot, _http: Http) {
        super(_apiRoot, _http);
        this.notificationsUrl = `${_apiRoot.baseUrl}api/v1/notification/getNotifications/allUsers/true`;
    }

    getNotifications() {
        return this.request({
            url: this.notificationsUrl,
            method: RequestMethod.Get
        });
    }

}