import { filter } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotEvent } from '@models/dot-event/dot-event';

/**
 * Provide a Global service to Subscribe to custom events and notify subscribers when those events occur.
 * @DotEventsService
 */
@Injectable()
export class DotEventsService {
    private subject: Subject<DotEvent<unknown>> = new Subject();

    /**
     * Method to register a listener of a specif event.
     *
     * @param string eventName
     * @returns Observable<DotEvent<T>>
     */
    listen<T>(eventName: string): Observable<DotEvent<T>> {
        // TODO: need to make this method to support multiple events
        return this.subject
            .asObservable()
            .pipe(filter((res: DotEvent<T>) => res.name === eventName));
    }

    /**
     * Method to notify subscribers of a specific event.
     *
     * @param string name
     * @param <T> data
     */
    notify<T>(name: string, data?: T): void {
        this.subject.next({
            name: name,
            data: data
        });
    }
}
