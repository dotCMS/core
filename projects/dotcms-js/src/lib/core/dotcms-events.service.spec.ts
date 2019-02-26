
import { Injectable, ReflectiveInjector } from '@angular/core';
import { Observable, of, Subject } from 'rxjs';
import { DotcmsEventsService } from './dotcms-events.service';
import { DotEventsSocketFactoryService } from './dot-events-socket-factory.service';
import { StringUtils } from './string-utils.service';
import { LoggerService } from './logger.service';
import { DotEventTypeWrapper } from './models';
import { DotEventMessage } from './util/models/dot-event-message';

class DotEventsSocketMock {
    _messages: Subject<any> = new Subject();

    connect(): void {}

    messages(): Observable<any> {
        return this._messages.asObservable();
    }

    public sendMessage(message: DotEventMessage) {
        this._messages.next(message);
    }
}

@Injectable()
class DotEventsSocketFactoryServiceMock  {
    socket = new DotEventsSocketMock();

    public createSocket(): Observable<any> {
        return of(this.socket);
    }
}


describe('DotcmsEventsService', () => {

    let socketFactory: DotEventsSocketFactoryServiceMock;
    let dotcmsEventsService: DotcmsEventsService;

    let injector: ReflectiveInjector;

    beforeEach(() => {
        socketFactory = new DotEventsSocketFactoryServiceMock();

        injector = ReflectiveInjector.resolveAndCreate([
            { provide: DotEventsSocketFactoryService, useValue: socketFactory },
            StringUtils,
            LoggerService,
            DotcmsEventsService
        ]);

        dotcmsEventsService = injector.get(DotcmsEventsService);
    });

    it('should create and connect a new socket', () => {
        spyOn(socketFactory.socket, 'connect');

        dotcmsEventsService.start();

        expect(socketFactory.socket.connect).toHaveBeenCalled();
    });

    it('should reuse socket', () => {
        const dotEventsSocketFactoryService = injector.get(DotEventsSocketFactoryService);
        spyOn(dotEventsSocketFactoryService, 'createSocket').and.callThrough();

        dotcmsEventsService.start();
        dotcmsEventsService.start();

        expect(dotEventsSocketFactoryService.createSocket).toHaveBeenCalledTimes(1);
    });

    it('should subscribe to a event', (done) => {
        dotcmsEventsService.start();

        dotcmsEventsService.subscribeTo('test_event').subscribe((dotEventData: any) => {
            expect(dotEventData).toEqual('test payload');
            done();
        });

        socketFactory.socket.sendMessage({
            event: 'test_event',
            payload: 'test payload'
        });
    });

    it('should subscribe to several events', () => {
        let count = 0;

        dotcmsEventsService.start();

        dotcmsEventsService.subscribeToEvents(['test_event_1', 'test_event_2'])
            .subscribe((dotEventTypeWrapper: DotEventTypeWrapper) => {
                if (dotEventTypeWrapper.eventType === 'test_event_1') {
                    expect(dotEventTypeWrapper.data).toEqual('test payload_1');
                } else if (dotEventTypeWrapper.eventType === 'test_event_2') {
                    expect(dotEventTypeWrapper.data).toEqual('test payload_2');
                } else {
                    expect(true).toBe(false);
                }

                count++;
            });

        socketFactory.socket.sendMessage({
            event: 'test_event_1',
            payload: 'test payload_1'
        });

        socketFactory.socket.sendMessage({
            event: 'test_event_2',
            payload: 'test payload_2'
        });

        expect(count).toBe(2);
    });
});
