import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotEventTypeWrapper } from './models';

/**
 * @deprecated Use DotEventsSocket from @dotcms/data-access directly.
 *
 * This is a pure Subject-based event bus with no WebSocket logic.
 * Messages are fed into it by withWebSocket() (global-store) via feedMessage(),
 * keeping dotcms-js free of any data-access imports and avoiding circular deps.
 *
 * start() and destroy() are intentional no-ops — DotEventsSocket owns the connection.
 */
@Injectable({ providedIn: 'root' })
export class DotcmsEventsService {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    #subjects: Record<string, Subject<any>> = {};

    /** Called by withWebSocket() on each incoming message to fan out to subscribers. */
    feedMessage(event: string, data: unknown): void {
        if (this.#subjects[event]) {
            this.#subjects[event].next(data);
        }
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    start(): void {}

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    destroy(): void {}

    subscribeTo<T>(clientEventType: string): Observable<T> {
        if (!this.#subjects[clientEventType]) {
            this.#subjects[clientEventType] = new Subject<T>();
        }

        return this.#subjects[clientEventType].asObservable();
    }

    subscribeToEvents<T>(clientEventTypes: string[]): Observable<DotEventTypeWrapper<T>> {
        const subject = new Subject<DotEventTypeWrapper<T>>();

        clientEventTypes.forEach((eventType) => {
            this.subscribeTo<T>(eventType).subscribe((data) => {
                subject.next({ data, name: eventType });
            });
        });

        return subject.asObservable();
    }
}
