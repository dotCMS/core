import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService, DotCMSResponse } from '@dotcms/dotcms-js';

import { DotNotificationResponse } from '../../shared/models/notifications/notification.model';

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
            .requestView<DotNotificationResponse>({
                url: this.urls.getLastNotificationsUrl
            })
            .pipe(map((res) => res?.bodyJsonObject));
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .requestView<DotNotificationResponse>({
                url: this.urls.getNotificationsUrl
            })
            .pipe(map((res) => res?.bodyJsonObject));
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
            .pipe(map((res) => res?.bodyJsonObject));
    }

    markAllAsRead(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.coreWebService
            .request<DotCMSResponse<DotNotificationResponse>>({
                method: 'PUT',
                url: this.urls.markAsReadNotificationsUrl
            })
            .pipe(map((res) => res as DotCMSResponse<DotNotificationResponse>));
    }
}
