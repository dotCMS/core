/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf, Subject } from 'rxjs';

import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotMessageService } from '@dotcms/data-access';
import { DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
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

@Component({
    selector: 'dot-icon-button',
    template: ''
})
class MockDotIconButtonComponent {}

@Injectable()
class MockIframeOverlayService {}

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
    const messageServiceMock = new MockDotMessageService({
        notifications_dismissall: 'Dismiss all',
        notifications_title: 'Notifications',
        notifications_load_more: 'More'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotToolbarNotificationsComponent,
                MockDotDropDownComponent,
                MockDotNotificationsListComponent,
                MockDotIconButtonComponent
            ],
            imports: [DotPipesModule, DotMessagePipe, ButtonModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: IframeOverlayService, useClass: MockIframeOverlayService },
                { provide: DotcmsEventsService, useClass: MockDotcmsEventsService },
                { provide: LoginService, useClass: MockLoginService },
                { provide: NotificationsService, useClass: MockNotificationsService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotToolbarNotificationsComponent);
    }));

    it(`should has a badge`, () => {
        fixture.detectChanges();
        const badge: DebugElement = fixture.debugElement.query(
            By.css('#dot-toolbar-notifications-badge')
        );
        expect(badge).not.toBeNull();
    });
});
