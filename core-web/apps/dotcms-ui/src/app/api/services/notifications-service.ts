import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotNotificationResponse } from '../../shared/models/notifications/notification.model';

interface DotNotificationServiceUrls {
    dismissNotificationsUrl: string;
    getLastNotificationsUrl: string;
    getNotificationsUrl: string;
    markAsReadNotificationsUrl: string;
}

// Response type for endpoints that return bodyJsonObject
interface DotBodyJsonResponse<T> {
    bodyJsonObject: T;
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
        return this.http
            .get<
                DotBodyJsonResponse<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.getLastNotificationsUrl)
            .pipe(map((response) => response.bodyJsonObject));
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .get<
                DotBodyJsonResponse<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.getNotificationsUrl)
            .pipe(map((response) => response.bodyJsonObject));
    }

    dismissNotifications(
        items: Record<string, unknown>
    ): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .put<
                DotBodyJsonResponse<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.dismissNotificationsUrl, items)
            .pipe(map((response) => response.bodyJsonObject));
    }

    markAllAsRead(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .put<
                DotBodyJsonResponse<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.markAsReadNotificationsUrl, {})
            .pipe(map((response) => response.bodyJsonObject));
    }
}
