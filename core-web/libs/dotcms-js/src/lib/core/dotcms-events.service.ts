import { Observable, Subscription, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { LoggerService } from './logger.service';
import { DotEventTypeWrapper } from './models';
import { DotEventsSocket } from './util/dot-event-socket';
import { DotEventMessage } from './util/models/dot-event-message';

@Injectable()
export class DotcmsEventsService {
    private dotEventsSocket = inject(DotEventsSocket);
    private loggerService = inject(LoggerService);

    private subjects = [];
    private messagesSub: Subscription;

    /**
     * Close the socket
     *
     * @memberof DotcmsEventsService
     */
    destroy(): void {
        this.dotEventsSocket.destroy();
        this.messagesSub.unsubscribe();
    }

    /**
     * Start the socket
     *
     * @memberof DotcmsEventsService
     */
    start(): void {
        this.loggerService.debug('start DotcmsEventsService', this.dotEventsSocket.isConnected());
        if (!this.dotEventsSocket.isConnected()) {
            this.loggerService.debug('Connecting with socket');

            this.messagesSub = this.dotEventsSocket
                .connect()
                .pipe(switchMap(() => this.dotEventsSocket.messages()))
                .subscribe(
                    ({ event, payload }: DotEventMessage) => {
                        if (!this.subjects[event]) {
                            this.subjects[event] = new Subject();
                        }

                        this.subjects[event].next(payload.data);
                    },
                    (e) => {
                        this.loggerService.debug(
                            'Error in the System Events service: ' + e.message
                        );
                    },
                    () => {
                        this.loggerService.debug('Completed');
                    }
                );
        }
    }

    /**
     * This method will be called by clients that want to receive notifications
     * regarding incoming system events. The events they will receive will be
     * based on the type of event clients register for.
     *
     * @memberof DotcmsEventsService
     */
    subscribeTo<T>(clientEventType: string): Observable<T> {
        if (!this.subjects[clientEventType]) {
            this.subjects[clientEventType] = new Subject<T>();
        }

        return this.subjects[clientEventType].asObservable();
    }

    /**
     * Subscribe to multiple events from the DotCMS WebSocket
     *
     * @memberof DotcmsEventsService
     */
    subscribeToEvents<T>(clientEventTypes: string[]): Observable<DotEventTypeWrapper<T>> {
        const subject: Subject<DotEventTypeWrapper<T>> = new Subject<DotEventTypeWrapper<T>>();

        clientEventTypes.forEach((eventType: string) => {
            this.subscribeTo(eventType).subscribe((data: T) => {
                subject.next({
                    data: data,
                    name: eventType
                });
            });
        });

        return subject.asObservable();
    }

    /**
     * Listen  when the socket is opened
     *
     * @returns Observable<boolean>
     * @memberof DotcmsEventsService
     */
    open(): Observable<boolean> {
        return this.dotEventsSocket.open();
    }
}
