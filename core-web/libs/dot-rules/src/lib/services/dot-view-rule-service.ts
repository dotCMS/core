import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable()
export class RuleViewService {
    private _message: Subject<DotRuleMessage> = new Subject();

    public showErrorMessage(message: string, allowClose = true, errorKey = ''): void {
        this._message.next({
            message,
            allowClose,
            errorKey
        });
    }

    get message(): Observable<DotRuleMessage> {
        return this._message.asObservable();
    }
}

export interface DotRuleMessage {
    message: string;
    allowClose: boolean;
    errorKey?: string;
}
