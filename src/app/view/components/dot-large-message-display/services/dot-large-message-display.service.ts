import { BehaviorSubject, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { DotcmsEventsService, DotEventData } from 'dotcms-js';

export interface DotLargeMessageDisplayParams {
    title: string;
    width?: string;
    height?: string;
    body?: string;
    code?: {
        lang: string;
        content: string;
    };
}

@Injectable({
    providedIn: 'root'
})
export class DotLargeMessageDisplayService {
    private _messages: BehaviorSubject<DotLargeMessageDisplayParams> = new BehaviorSubject(null);

    constructor(dotcmsEventsService: DotcmsEventsService) {
        dotcmsEventsService.subscribeTo('LARGE_MESSAGE').subscribe((messageEvent: DotEventData) => {
            const { code, width, body, title, lang, height } = messageEvent.data;
            this.push({
                title,
                height,
                width,
                body,
                code: { lang, content: code }
            });
        });
    }

    /**
     * Clear service's Observable content
     *
     * @memberof DotLargeMessageDisplayService
     */
    clear(): void {
        this._messages.next(null);
    }

    /**
     * Allow set/publish new large messages
     *
     * @param DotLargeMessageDisplayParams message
     * @memberof DotLargeMessageDisplayService
     */
    push(message: DotLargeMessageDisplayParams): void {
        this._messages.next(message);
    }

    /**
     * Allow subscribe to receive new messages
     *
     * @returns {Observable<DotLargeMessageDisplayParams>}
     * @memberof DotLargeMessageDisplayService
     */
    sub(): Observable<DotLargeMessageDisplayParams> {
        return this._messages.asObservable();
    }

}
