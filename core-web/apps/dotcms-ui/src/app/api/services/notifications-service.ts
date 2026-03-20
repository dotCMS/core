import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse, DotCMSResponseJsonObject } from '@dotcms/dotcms-models';

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

    /*
     * The notification API returns a two-level envelope:
     *   { bodyJsonObject: { entity: DotNotificationResponse, errors: [], ... } }
     *
     * DotCMSResponseJsonObject models the outer `bodyJsonObject` wrapper,
     * DotCMSResponse models the inner standard payload (`entity`, `errors`, etc.).
     * The `map` below unwraps the outer layer so callers receive a flat DotCMSResponse.
     */

    getLastNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http.get<DotCMSResponse<DotNotificationResponse>>(
            this.urls.getLastNotificationsUrl
        );
    }

    getAllNotifications(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .get<
                DotCMSResponseJsonObject<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.getNotificationsUrl)
            .pipe(map((response) => response.bodyJsonObject));
    }

    dismissNotifications(
        items: Record<string, unknown>
    ): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .put<
                DotCMSResponseJsonObject<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.dismissNotificationsUrl, items)
            .pipe(map((response) => response.bodyJsonObject));
    }

    markAllAsRead(): Observable<DotCMSResponse<DotNotificationResponse>> {
        return this.http
            .put<
                DotCMSResponseJsonObject<DotCMSResponse<DotNotificationResponse>>
            >(this.urls.markAsReadNotificationsUrl, {})
            .pipe(map((response) => response.bodyJsonObject));
    }
}
