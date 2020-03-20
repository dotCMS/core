
import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable()
export class RuleViewService {
    private _message: Subject<string> = new Subject();

    public showErrorMessage(message: string): void {
        this._message.next(message);
    }

    get message(): Observable<string> {
        return this._message.asObservable();
    }
}