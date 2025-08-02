/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from '@jest/globals';

import { TestBed } from '@angular/core/testing';

import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotMessage, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotcmsEventsServiceMock } from '@dotcms/utils-testing';

import { DotMessageDisplayService } from './dot-message-display.service';

import { DotRouterService } from '../dot-router/dot-router.service';

describe('DotMessageDisplayService', () => {
    const mockDotcmsEventsService: DotcmsEventsServiceMock = new DotcmsEventsServiceMock();

    let dotMessageDisplayService: DotMessageDisplayService;

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

    xit('should emit a message', () => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual({
                ...messageExpected,
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
        });

        mockDotcmsEventsService.triggerSubscribeTo('MESSAGE', messageExpected);
    });

    it('should push a message', () => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual(messageExpected);
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
        it('should show message when currentPortlet is in portletIdList ', () => {
            messageExpected.portletIdList = ['content-types-angular'];

            dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
                expect(message).toEqual({
                    ...messageExpected,
                    severity: DotMessageSeverity.ERROR,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
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
