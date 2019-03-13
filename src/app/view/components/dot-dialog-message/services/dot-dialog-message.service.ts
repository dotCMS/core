import { BehaviorSubject, Observable } from 'rxjs';
import { Injectable } from '@angular/core';

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

    constructor() {}

    push(message: DotDialogMessageParams): void {
        this._messages.next(message);
    }

    sub(): Observable<DotDialogMessageParams> {
        return this._messages.asObservable();
    }
}
