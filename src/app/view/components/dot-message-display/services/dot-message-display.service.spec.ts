import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { DotMessageDisplayService } from './dot-message-display.service';
import { DotMessage } from '../model/dot-message.model';
import { DotMessageSeverity } from '../model/dot-message-severity.model';
import { DotMessageType } from '../model/dot-message-type.model';
import { DotcmsEventsService } from 'dotcms-js';
import { DotcmsEventsServiceMock } from 'src/app/test/dotcms-events-service.mock';
import { Router } from '@angular/router';


describe('DotMessageDisplayService', () => {

    const mockRouter = {
        routerState: {
            snapshot: {
                url: '/content-types-angular'
            }
        }
    };

    const mockDotcmsEventsService: DotcmsEventsServiceMock = new DotcmsEventsServiceMock();

    let dotMessageDisplayService;

    const messageExpected: any = {
        life: 3000,
        message: 'Hello World',
        portletIdList: [],
        severity: 'ERROR',
        type: 'SIMPLE_MESSAGE'
    };

    beforeEach(() => {
        const injector = DOTTestBed.resolveAndCreate([
            { provide: DotcmsEventsService, useValue: mockDotcmsEventsService },
            { provide: Router, useValue: mockRouter },
            DotMessageDisplayService
        ]);

        dotMessageDisplayService = injector.get(DotMessageDisplayService);
    });

    it('should emit a message', (done) => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual({
                ...messageExpected,
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
            done();
        });

        mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', {
            data: messageExpected,
            type: 'SIMPLE_MESSAGE'
        });
    });

    it('should unsubscribe', () => {
        let wasCalled = false;

        dotMessageDisplayService.messages().subscribe(() => {
            wasCalled = true;
        });

        dotMessageDisplayService.unsubscribe();

        mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', {
            data: messageExpected,
            type: 'SIMPLE_MESSAGE'
        });

        expect(wasCalled).toBe(false);
    });

    describe('with portletIdList', () => {
        it('should show message when currentPortlet is in portletIdList ', (done) => {
            messageExpected.portletIdList = ['content-types-angular'];

            dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
                expect(message).toEqual({
                    ...messageExpected,
                    severity: DotMessageSeverity.ERROR,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
                done();
            });

            mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', {
                data: messageExpected,
                type: 'SIMPLE_MESSAGE'
            });
        });

        it('should not show message when currentPortlet is not in portletIdList ', () => {
            messageExpected.portletIdList = ['not-content-types-angular'];

            let wasCalled = false;

            dotMessageDisplayService.messages().subscribe(() => {
                wasCalled = true;
            });

            mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', {
                data: messageExpected,
                type: 'SIMPLE_MESSAGE'
            });

            expect(wasCalled).toBe(false);
        });
    });
});
