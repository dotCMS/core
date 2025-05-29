import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';
import { of, Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable, signal } from '@angular/core';

import { DotDropdownComponent } from '@components/_common/dot-dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { AnnouncementsStore } from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotMessageService } from '@dotcms/data-access';
import { DotcmsEventsService, LoginService, SiteService, SiteServiceMock } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNotificationsListComponent } from './components/dot-notifications/dot-notifications.component';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

@Injectable()
class MockDotcmsEventsService {
    private events: Subject<unknown> = new Subject();

    subscribeTo(clientEventType: string) {
        if (clientEventType) {
            return this.events.asObservable();
        } else {
            return null;
        }
    }
}

@Injectable()
class MockLoginService {
    public watchUser(_func: (params?: unknown) => void): void {
        return;
    }
}

describe('DotToolbarNotificationsComponent', () => {
    let spectator: Spectator<DotToolbarNotificationsComponent>;
    let notificationService: SpyObject<NotificationsService>;
    let iframeOverlayService: SpyObject<IframeOverlayService>;

    const messageServiceMock = new MockDotMessageService({
        notifications_dismissall: 'Dismiss all',
        notifications_title: 'Notifications',
        notifications_load_more: 'More',
        notifications_no_notifications_title: 'No More Notifications Here',
        notifications_no_notifications: 'There are no notifications to show right now.'
    });
    const siteServiceMock = new SiteServiceMock();

    const createComponent = createComponentFactory({
        component: DotToolbarNotificationsComponent,
        declarations: [
            MockComponent(DotNotificationsListComponent),
            MockComponent(DotDropdownComponent)
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            AnnouncementsStore,
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: IframeOverlayService, useClass: IframeOverlayService },
            { provide: DotcmsEventsService, useClass: MockDotcmsEventsService },
            { provide: LoginService, useClass: MockLoginService },
            mockProvider(NotificationsService),
            {
                provide: SiteService,
                useValue: siteServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        notificationService = spectator.inject(NotificationsService);
        iframeOverlayService = spectator.inject(IframeOverlayService);
    });

    it(`should has a badge`, () => {
        notificationService.getLastNotifications.and.returnValue(
            of({
                entity: {
                    totalUnreadNotifications: 1,
                    notifications: [
                        {
                            id: '1',
                            title: 'Notification Title',
                            message: 'Notification message'
                        }
                    ],
                    total: 1
                }
            })
        );
        spectator.component.showUnreadAnnouncement = signal(true);
        spectator.detectChanges();
        const badge = spectator.query('#dot-toolbar-notifications-badge');
        expect(badge).not.toBeNull();
    });

    it('should display the dropdown even if there are no notifications', () => {
        notificationService.getLastNotifications.and.returnValue(
            of({
                entity: {
                    totalUnreadNotifications: 0,
                    notifications: [],
                    total: 0
                }
            })
        );

        iframeOverlayService.show();
        spectator.detectChanges();

        const notificationsComponent = spectator.query(byTestId('dot-toolbar-notifications'));
        expect(notificationsComponent).toBeDefined();
    });
});
