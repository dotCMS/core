import { Observable, Subject } from 'rxjs';

import { LoggerService } from '../logger.service';

export abstract class Protocol {
    protected _open: Subject<any> = new Subject<any>();
    protected _close: Subject<any> = new Subject<any>();
    protected _message: Subject<any> = new Subject<any>();
    protected _error: Subject<any> = new Subject<any>();

    constructor(protected loggerService: LoggerService) {}

    abstract connect(): void;
    abstract close(): void;

    message$(): Observable<any> {
        return this._message.asObservable();
    }

    open$(): Observable<any> {
        return this._open.asObservable();
    }

    close$(): Observable<any> {
        return this._close.asObservable();
    }

    error$(): Observable<any> {
        return this._error.asObservable();
    }

    reconnect(): void {
        this.close();
        setTimeout(() => {
            this.connect();
        }, 0);
    }

    destroy(): void {
        this._open.complete();
        this._close.complete();
        this._message.complete();
        this._error.complete();
    }
}
