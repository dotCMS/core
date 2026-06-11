/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from '@jest/globals';
import { Subject } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotMessage, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

import { DotMessageDisplayService } from './dot-message-display.service';

import { DotRouterService } from '../dot-router/dot-router.service';
import { DotEventsSocket } from '../dot-websocket/dot-events-socket.service';

describe('DotMessageDisplayService', () => {
    const messageSubject = new Subject<unknown>();
    const mockDotEventsSocket = {
        on: jest.fn().mockReturnValue(messageSubject.asObservable())
    };

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
                { provide: DotEventsSocket, useValue: mockDotEventsSocket },
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

    it('should emit a message', () => {
        dotMessageDisplayService.messages().subscribe((message: DotMessage) => {
            expect(message).toEqual({
                ...messageExpected,
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
        });

        messageSubject.next(messageExpected);
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

        messageSubject.next(messageExpected);

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

            messageSubject.next(messageExpected);
        });

        it('should not show message when currentPortlet is not in portletIdList ', () => {
            messageExpected.portletIdList = ['not-content-types-angular'];

            let wasCalled = false;

            dotMessageDisplayService.messages().subscribe(() => {
                wasCalled = true;
            });

            messageSubject.next(messageExpected);

            expect(wasCalled).toBe(false);
        });
    });
});
