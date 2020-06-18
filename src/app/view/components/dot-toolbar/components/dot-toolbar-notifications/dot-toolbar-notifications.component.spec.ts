import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input, Injectable } from '@angular/core';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';
import { INotification } from '@shared/models/notifications';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotcmsEventsService, LoginService } from 'dotcms-js';
import { NotificationsService } from '@services/notifications-service';
import { Observable, Subject } from 'rxjs';
import { of as observableOf } from 'rxjs';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';

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
class MockIframeOverlayService {

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
    public watchUser(_func: Function): void {}
}

@Injectable()
class MockNotificationsService {
    getLastNotifications(): Observable<any> {

        return observableOf(
            {
                entity: {
                    totalUnreadNotifications: 1,
                    notifications: [
                        {
                            id: '1',
                            title: 'Notification Title',
                            message: 'Notification message',
                        }
                    ],
                    total: 1
                }
            }
        );
    }
}


describe('DotToolbarNotificationsComponent', () => {
    let fixture: ComponentFixture<DotToolbarNotificationsComponent>;
    const messageServiceMock = new MockDotMessageService({
        'notifications_dismissall': 'Dismiss all',
        'notifications_title': 'Notifications',
        'notifications_load_more': 'More'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DotToolbarNotificationsComponent,
                MockDotDropDownComponent,
                MockDotNotificationsListComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock},
                { provide: IframeOverlayService, useClass: MockIframeOverlayService },
                { provide: DotcmsEventsService, useClass: MockDotcmsEventsService },
                { provide: LoginService, useClass: MockLoginService },
                { provide: NotificationsService, useClass: MockNotificationsService },
            ]
        });

        fixture = DOTTestBed.createComponent(DotToolbarNotificationsComponent);
    }));

    it(`should has a badge`, () => {
        fixture.detectChanges();
        const badge: DebugElement = fixture.debugElement.query(By.css('#dot-toolbar-notifications-badge'));
        expect(badge).not.toBeNull();
    });
});
