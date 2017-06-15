
import { Observable, Subject } from 'rxjs';

export class DotcmsEventsServiceMock {
    private observers: Subject<any>[] = [];

    subscribeTo(clientEventType: string): Observable<any> {
        if (!this.observers[clientEventType]) {
            this.observers[clientEventType] = new Subject();
        }
        return this.observers[clientEventType].asObservable();
    }

    tiggerSubscribeTo(clientEventType: string, data: any): void {
        this.observers[clientEventType].next(data);
    }
}