import { Observable, Subject } from 'rxjs';
import { DotEventData } from 'dotcms-js/lib/core/event/model/dot-event-data';
import { DotEventTypeWrapper } from 'dotcms-js/lib/core/event/model/dot-event-type-wrapper';

export class DotcmsEventsServiceMock {
    private observers: Subject<any>[] = [];

    subscribeTo(clientEventType: string): Observable<DotEventData> {
        if (!this.observers[clientEventType]) {
            this.observers[clientEventType] = new Subject();
        }
        return this.observers[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<DotEventTypeWrapper> {
        const subject: Subject<any> = new Subject<any>();

        clientEventTypes.forEach((eventType) =>
            this.subscribeTo(eventType).subscribe((data) => subject.next(data))
        );

        return subject.asObservable();
    }

    triggerSubscribeTo(clientEventType: string, data: any): void {
        this.observers[clientEventType].next(data);
    }

    triggerSubscribeToEvents(clientEventTypes: string[], data: any): void {
        clientEventTypes.forEach((eventType) => {
            this.observers[eventType].next(data);
        });
    }
}

