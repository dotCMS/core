import { Observable, Subject } from 'rxjs';
import { DotEventTypeWrapper } from '@dotcms/dotcms-js';

export class DotcmsEventsServiceMock {
    private observers: Subject<any>[] = [];

    subscribeTo(clientEventType: string): Observable<DotEventTypeWrapper<any>> {
        if (!this.observers[clientEventType]) {
            this.observers[clientEventType] = new Subject();
        }
        return this.observers[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<DotEventTypeWrapper<any>> {
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
            this.observers[eventType].next({
                eventType: eventType,
                data: data
            });
        });
    }
}
