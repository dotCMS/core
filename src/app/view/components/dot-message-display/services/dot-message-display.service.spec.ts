import { DotMessageDisplayService } from './dot-message-display.service';
import { DotMessage, DotMessageSeverity, DotMessageType } from '../model';
import { DotcmsEventsService } from 'dotcms-js';
import { DotcmsEventsServiceMock } from 'src/app/test/dotcms-events-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { TestBed } from '@angular/core/testing';

describe('DotMessageDisplayService', () => {
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
        TestBed.configureTestingModule({
            providers: [
                DotMessageDisplayService,
                { provide: DotcmsEventsService, useValue: mockDotcmsEventsService },
                {
                    provide: DotRouterService,
                    useValue: {
                        currentPortlet: {
                            id: 'content-types-angular',
                            url: ''
                        }
                    }
                }
            ]
        });

        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
    });

    xit('should emit a message', (done) => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual({
                ...messageExpected,
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
            done();
        });

        mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', messageExpected);
    });

    it('should push a message', (done) => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual(messageExpected);
            done();
        });

        dotMessageDisplayService.push(messageExpected);
    });

    it('should unsubscribe', () => {
        let wasCalled = false;

        dotMessageDisplayService.messages().subscribe(() => {
            wasCalled = true;
        });

        dotMessageDisplayService.unsubscribe();

        mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', messageExpected);

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

            mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', messageExpected);
        });

        it('should not show message when currentPortlet is not in portletIdList ', () => {
            messageExpected.portletIdList = ['not-content-types-angular'];

            let wasCalled = false;

            dotMessageDisplayService.messages().subscribe(() => {
                wasCalled = true;
            });

            mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', messageExpected);

            expect(wasCalled).toBe(false);
        });
    });
});
