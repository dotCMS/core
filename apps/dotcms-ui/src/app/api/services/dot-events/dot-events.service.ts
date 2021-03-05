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
    private subject: Subject<DotEvent> = new Subject();

    constructor() {}

    /**
     * Method to register a listener of a specif event.
     *
     * @param string eventName
     * @returns Observable<DotEvent>
     */
    listen(eventName: string): Observable<DotEvent> {
        // TODO: need to make this method to support multiple events
        return this.subject.asObservable().pipe(filter((res) => res.name === eventName));
    }

    /**
     * Method to notify subscribers of a specific event.
     *
     * @param DotEvent dotEvent
     */
    notify(name: string, data?: any): void {
        this.subject.next({
            name: name,
            data: data
        });
    }
}
