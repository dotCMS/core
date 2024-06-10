import { Observable, Subject, of } from 'rxjs';

import { ReflectiveInjector } from '@angular/core';

import { DotcmsEventsService } from './dotcms-events.service';
import { LoggerService } from './logger.service';
import { DotEventTypeWrapper } from './models';
import { StringUtils } from './string-utils.service';
import { DotEventsSocket } from './util/dot-event-socket';
import { DotEventMessage } from './util/models/dot-event-message';

class DotEventsSocketMock {
    _messages: Subject<any> = new Subject();
    _open: Subject<any> = new Subject();
    private connected = false;

    connect(): Observable<any> {
        this.connected = true;

        return of(true);
    }

    open(): Observable<any> {
        return this._open.asObservable();
    }

    messages(): Observable<any> {
        return this._messages.asObservable();
    }

    public sendMessage(message: DotEventMessage) {
        this._messages.next(message);
    }

    isConnected(): boolean {
        return this.connected;
    }

    destroy(): void {}
}

describe('DotcmsEventsService', () => {
    let socket: DotEventsSocketMock;
    let dotcmsEventsService: DotcmsEventsService;

    let injector: ReflectiveInjector;

    beforeEach(() => {
        socket = new DotEventsSocketMock();

        injector = ReflectiveInjector.resolveAndCreate([
            { provide: DotEventsSocket, useValue: socket },
            StringUtils,
            LoggerService,
            DotcmsEventsService
        ]);

        dotcmsEventsService = injector.get(DotcmsEventsService);

        spyOn(socket, 'connect').and.callThrough();
        spyOn(socket, 'destroy').and.callThrough();
    });

    it('should create and connect a new socket', () => {
        dotcmsEventsService.start();

        expect(socket.connect).toHaveBeenCalled();
    });

    it('should reuse socket', () => {
        dotcmsEventsService.start();
        dotcmsEventsService.start();

        expect(socket.connect).toHaveBeenCalledTimes(1);
    });

    it('should trigger open event', (done) => {
        dotcmsEventsService.open().subscribe(() => {
            done();
        });

        socket._open.next(true);
    });

    it('should subscribe to a event', (done) => {
        dotcmsEventsService.start();

        dotcmsEventsService.subscribeTo('test_event').subscribe((dotEventData: any) => {
            expect(dotEventData).toEqual('test payload');
            done();
        });

        socket.sendMessage({
            event: 'test_event',
            payload: {
                data: 'test payload'
            }
        });
    });

    it('should subscribe to several events', () => {
        let count = 0;

        dotcmsEventsService.start();

        dotcmsEventsService
            .subscribeToEvents(['test_event_1', 'test_event_2'])
            .subscribe((dotEventTypeWrapper: DotEventTypeWrapper<any>) => {
                if (dotEventTypeWrapper.name === 'test_event_1') {
                    expect(dotEventTypeWrapper.data).toEqual('test payload_1');
                } else if (dotEventTypeWrapper.name === 'test_event_2') {
                    expect(dotEventTypeWrapper.data).toEqual('test payload_2');
                } else {
                    expect(true).toBe(false);
                }

                count++;
            });

        socket.sendMessage({
            event: 'test_event_1',
            payload: {
                data: 'test payload_1'
            }
        });

        socket.sendMessage({
            event: 'test_event_2',
            payload: {
                data: 'test payload_2'
            }
        });

        expect(count).toBe(2);
    });

    it('should destroy socket', () => {
        dotcmsEventsService.start();
        dotcmsEventsService.destroy();

        expect(socket.destroy).toHaveBeenCalled();
    });

    it('should destroy socket and connect', () => {
        dotcmsEventsService.start();
        dotcmsEventsService.destroy();
        expect(socket.destroy).toHaveBeenCalled();

        dotcmsEventsService.start();
        expect(socket.connect).toHaveBeenCalledTimes(1);
    });

    it('should stop emitting after destroy', () => {
        const subscribeCallback = jasmine.createSpy('spy');

        dotcmsEventsService.start();
        dotcmsEventsService.subscribeTo('test_event').subscribe(subscribeCallback);
        dotcmsEventsService.destroy();

        socket.sendMessage({
            event: 'test_event',
            payload: 'test payload'
        });

        expect(subscribeCallback).not.toHaveBeenCalled();
    });
});
