import { BehaviorSubject, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { DotcmsEventsService, DotEventData } from 'dotcms-js';

export interface DotDialogMessageParams {
    title: string;
    width?: string;
    height?: string;
    body?: string;
    code?: {
        lang: string;
        content: string;
    };
}

@Injectable()
export class DotDialogMessageService {
    private _messages: BehaviorSubject<DotDialogMessageParams> = new BehaviorSubject(null);

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

    push(message: DotDialogMessageParams): void {
        this._messages.next(message);
    }

    sub(): Observable<DotDialogMessageParams> {
        return this._messages.asObservable();
    }
}
