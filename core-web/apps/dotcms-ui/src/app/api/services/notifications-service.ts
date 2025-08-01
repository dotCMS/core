import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService, DotCMSResponse } from '@dotcms/dotcms-js';
import { DotNotificationResponse } from '@models/notifications';

interface DotNotificationServiceUrls {
    dismissNotificationsUrl: string;
    getLastNotificationsUrl: string;
    getNotificationsUrl: string;
    markAsReadNotificationsUrl: string;
}

@Injectable()
export class NotificationsService {
    private coreWebService = inject(CoreWebService);

    private urls: DotNotificationServiceUrls;

    constructor() {
        this.urls = {
            dismissNotificationsUrl: 'v1/notification/delete',
            getLastNotificationsUrl: 'v1/notification/getNotifications/offset/0/limit/24',
            getNotificationsUrl: 'v1/notification/getNotifications/',
            markAsReadNotificationsUrl: 'v1/notification/markAsRead'
        };
    }

    getLastNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView({
                url: this.urls.getLastNotificationsUrl
            })
            .pipe(pluck('bodyJsonObject'));
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView({
                url: this.urls.getNotificationsUrl
            })
            .pipe(pluck('bodyJsonObject'));
    }

    dismissNotifications(
        items: Record<string, unknown>
    ): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView({
                body: items,
                method: 'PUT',
                url: this.urls.dismissNotificationsUrl
            })
            .pipe(pluck('bodyJsonObject'));
    }

    markAllAsRead(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .request({
                method: 'PUT',
                url: this.urls.markAsReadNotificationsUrl
            })
            .pipe(pluck('bodyJsonObject'));
    }
}
