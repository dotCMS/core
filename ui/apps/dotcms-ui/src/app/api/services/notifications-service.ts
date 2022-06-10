import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';
import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotNotificationResponse } from '@models/notifications';

interface DotNotificationServiceUrls {
    dismissNotificationsUrl: string;
    getLastNotificationsUrl: string;
    getNotificationsUrl: string;
    markAsReadNotificationsUrl: string;
}

@Injectable()
export class NotificationsService {
    private urls: DotNotificationServiceUrls;

    constructor(private coreWebService: CoreWebService) {
        this.urls = {
            dismissNotificationsUrl: 'v1/notification/delete',
            getLastNotificationsUrl: 'v1/notification/getNotifications/offset/0/limit/24',
            getNotificationsUrl: 'v1/notification/getNotifications/',
            markAsReadNotificationsUrl: 'v1/notification/markAsRead'
        };
    }

    getLastNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView<DotNotificationResponse>({
                url: this.urls.getLastNotificationsUrl
            })
            .pipe(pluck('body'));
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView<DotNotificationResponse>({
                url: this.urls.getNotificationsUrl
            })
            .pipe(pluck('body'));
    }

    dismissNotifications(
        items: Record<string, unknown>
    ): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView<DotNotificationResponse>({
                body: items,
                method: 'PUT',
                url: this.urls.dismissNotificationsUrl
            })
            .pipe(pluck('body'));
    }

    markAllAsRead() {
        return this.coreWebService
            .request({
                method: 'PUT',
                url: this.urls.markAsReadNotificationsUrl
            })
            .pipe(pluck('bodyJsonObject'));
    }
}
