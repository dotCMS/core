import { Observable, Subject } from 'rxjs';

export class DotcmsEventsServiceMock {
    private observers: Subject<any>[] = [];

    subscribeTo(clientEventType: string): Observable<any> {
        if (!this.observers[clientEventType]) {
            this.observers[clientEventType] = new Subject();
        }
        return this.observers[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<any> {
        let subject: Subject<any> = new Subject<any>();

        clientEventTypes.forEach(eventType => this.subscribeTo(eventType).subscribe(data => subject.next(data)));

        return subject.asObservable();
    }

    tiggerSubscribeTo(clientEventType: string, data: any): void {
        this.observers[clientEventType].next(data);
    }

    triggerSubscribeToEvents(clientEventTypes: string[], data: any): void {
        clientEventTypes.forEach((eventType) => {
            this.observers[eventType].next(data);
        });
    }
}