import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LoggerService } from './logger.service';
import { Subject } from 'rxjs';
import { DotEventTypeWrapper } from './models';
import { DotEventsSocket } from './util/dot-event-socket';
import { DotEventMessage } from './util/models/dot-event-message';

@Injectable()
export class DotcmsEventsService {
    private subjects: Subject<any>[] = [];

    constructor(private dotEventsSocket: DotEventsSocket, private loggerService: LoggerService) {
    }

    /**
     * Close the socket
     *
     * @memberof DotcmsEventsService
     */
    destroy(): void {
        this.dotEventsSocket.destroy();
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

            this.dotEventsSocket.connect().subscribe(() => {
                this.dotEventsSocket.messages().subscribe(
                    (data: DotEventMessage) => {
                        if (!this.subjects[data.event]) {
                            this.subjects[data.event] = new Subject();
                        }
                        this.subjects[data.event].next(data.payload);
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
            });
        }
    }

    /**
     * This method will be called by clients that want to receive notifications
     * regarding incoming system events. The events they will receive will be
     * based on the type of event clients register for.
     *
     * @param clientEventType - The type of event clients will get. For example,
     *                          "notification" will allow a client to receive the
     *                          messages in the Notification section.
     * @returns any The system events that a client will receive.
     */
    subscribeTo(clientEventType: string): Observable<any> {
        if (!this.subjects[clientEventType]) {
            this.subjects[clientEventType] = new Subject();
        }

        return this.subjects[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<DotEventTypeWrapper> {
        const subject: Subject<DotEventTypeWrapper> = new Subject<DotEventTypeWrapper>();

        clientEventTypes.forEach((eventType) =>
            this.subscribeTo(eventType).subscribe((data) =>
                subject.next({
                    data: data,
                    eventType: eventType
                })
            )
        );

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
