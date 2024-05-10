/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf, of, Subject } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Injectable, Input, signal } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { AnnouncementsStore } from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotMessageService } from '@dotcms/data-access';
import { DotcmsEventsService, LoginService, SiteService, SiteServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { INotification } from '@shared/models/notifications';

import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

@Component({
    selector: 'dot-dropdown-component',
    template: ''
})
class MockDotDropDownComponent {
    @Input()
    disabled = false;
}

@Component({
    selector: 'dot-notifications-list',
    template: ''
})
class MockDotNotificationsListComponent {
    @Input()
    notifications: INotification;
}

@Injectable()
class MockDotcmsEventsService {
    private events: Subject<any> = new Subject();

    subscribeTo(clientEventType: string): Observable<any> {
        if (clientEventType) {
            return this.events.asObservable();
        } else {
            return null;
        }
    }
}

@Injectable()
class MockLoginService {
    public watchUser(_func: (params?: unknown) => void): void {}
}

@Injectable()
class MockNotificationsService {
    getLastNotifications(): Observable<any> {
        return observableOf({
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
        });
    }
}

describe('DotToolbarNotificationsComponent', () => {
    let fixture: ComponentFixture<DotToolbarNotificationsComponent>;
    let iframeOverlayService: IframeOverlayService;
    const messageServiceMock = new MockDotMessageService({
        notifications_dismissall: 'Dismiss all',
        notifications_title: 'Notifications',
        notifications_load_more: 'More',
        notifications_no_notifications_title: 'No More Notifications Here',
        notifications_no_notifications: 'There are no notifications to show right now.'
    });
    const siteServiceMock = new SiteServiceMock();

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotToolbarNotificationsComponent,
                MockDotDropDownComponent,
                MockDotNotificationsListComponent
            ],
            imports: [DotSafeHtmlPipe, DotMessagePipe, ButtonModule, HttpClientTestingModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: IframeOverlayService, useClass: IframeOverlayService },
                { provide: DotcmsEventsService, useClass: MockDotcmsEventsService },
                { provide: LoginService, useClass: MockLoginService },
                { provide: NotificationsService, useClass: MockNotificationsService },
                {
                    provide: AnnouncementsStore,
                    useClass: AnnouncementsStore
                },
                {
                    provide: SiteService,
                    useValue: siteServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotToolbarNotificationsComponent);
        iframeOverlayService = fixture.debugElement.injector.get(IframeOverlayService);
    }));

    it(`should has a badge`, () => {
        fixture.componentInstance.showUnreadAnnouncement = signal(true);
        fixture.detectChanges();
        const badge: DebugElement = fixture.debugElement.query(
            By.css('#dot-toolbar-notifications-badge')
        );

        expect(badge).not.toBeNull();
    });

    it('should display the dropdown even if there are no notifications', () => {
        spyOn(TestBed.inject(NotificationsService), 'getLastNotifications').and.returnValue(
            of<any>({
                entity: {
                    totalUnreadNotifications: 0,
                    notifications: [],
                    total: 0
                }
            })
        );

        iframeOverlayService.show();
        fixture.detectChanges();

        const notificationsComponent = fixture.debugElement.query(
            By.css('[data-testId="dot-toolbar-notifications"]')
        );
        expect(notificationsComponent).toBeDefined();
    });
});
