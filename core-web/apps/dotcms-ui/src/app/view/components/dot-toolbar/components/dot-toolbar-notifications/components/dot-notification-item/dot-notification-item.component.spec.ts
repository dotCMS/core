import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocketURL,
    LoggerService,
    StringUtils,
    DotEventsSocket
} from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNotificationItemComponent } from './dot-notification-item.component';

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

describe('DotNotificationItemComponent', () => {
    let spectator: Spectator<DotNotificationItemComponent>;
    let component: DotNotificationItemComponent;

    const messageServiceMock = new MockDotMessageService({
        notifications_dismiss: 'Dismiss'
    });

    const createComponent = createComponentFactory({
        component: DotNotificationItemComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            DotcmsEventsService,
            DotcmsConfigService,
            LoggerService,
            StringUtils,
            DotEventsSocket
        ],
        detectChanges: false
    });

    const mockNotification = {
        id: '1',
        title: 'Test Notification',
        message: 'This is a test notification',
        level: 'INFO',
        timeSent: '2024-01-01T00:00:00Z',
        actions: [
            {
                action: 'https://www.google.com',
                actionType: 'LINK',
                text: 'Test Action'
            }
        ],
        notificationData: {
            actions: [
                {
                    action: 'https://www.google.com',
                    actionType: 'LINK',
                    text: 'Test Action'
                }
            ]
        }
    };

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: mockNotification
            } as unknown
        });
        component = spectator.component;
    });

    it('should create component successfully', () => {
        spectator.detectChanges();
        expect(component).toBeTruthy();
    });

    it('should initialize with default values', () => {
        spectator.detectChanges();
        expect(component.$data()).toBe(mockNotification);
    });
});
