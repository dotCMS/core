import { Observable, Subject } from 'rxjs';

import { DotEventTypeWrapper } from '@dotcms/dotcms-js';

export class DotcmsEventsServiceMock {
    private observers: Subject<unknown>[] = [];

    subscribeTo(clientEventType: string): Observable<DotEventTypeWrapper<unknown>> {
        if (!this.observers[clientEventType]) {
            this.observers[clientEventType] = new Subject();
        }

        return this.observers[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<DotEventTypeWrapper<unknown>> {
        const subject: Subject<DotEventTypeWrapper<unknown>> = new Subject<
            DotEventTypeWrapper<unknown>
        >();

        clientEventTypes.forEach((eventType) =>
            this.subscribeTo(eventType).subscribe((data) => subject.next(data))
        );

        return subject.asObservable();
    }

    triggerSubscribeTo(clientEventType: string, data: unknown): void {
        this.observers[clientEventType].next(data);
    }

    triggerSubscribeToEvents(clientEventTypes: string[], data: unknown): void {
        clientEventTypes.forEach((eventType) => {
            this.observers[eventType].next({
                eventType: eventType,
                data: data
            });
        });
    }
}
