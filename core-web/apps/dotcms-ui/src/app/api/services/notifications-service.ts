import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotNotificationResponse } from '../../shared/models/notifications/notification.model';

interface DotNotificationServiceUrls {
    dismissNotificationsUrl: string;
    getLastNotificationsUrl: string;
    getNotificationsUrl: string;
    markAsReadNotificationsUrl: string;
}

@Injectable()
export class NotificationsService {
    private http = inject(HttpClient);

    private urls: DotNotificationServiceUrls;

    constructor() {
        this.urls = {
            dismissNotificationsUrl: '/api/v1/notification/delete',
            getLastNotificationsUrl: '/api/v1/notification/getNotifications/offset/0/limit/24',
            getNotificationsUrl: '/api/v1/notification/getNotifications/',
            markAsReadNotificationsUrl: '/api/v1/notification/markAsRead'
        };
    }

    getLastNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http.get<DotCMSResponse<DotNotificationResponse>>(
            this.urls.getLastNotificationsUrl
        );
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http.get<DotCMSResponse<DotNotificationResponse>>(
            this.urls.getNotificationsUrl
        );
    }

    dismissNotifications(
        items: Record<string, unknown>
    ): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http.put<DotCMSResponse<DotNotificationResponse>>(
            this.urls.dismissNotificationsUrl,
            items
        );
    }

    markAllAsRead(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http.put<DotCMSResponse<DotNotificationResponse>>(
            this.urls.markAsReadNotificationsUrl,
            {}
        );
    }
}
