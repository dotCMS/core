import {Observable} from 'rxjs/Rx';
import {LoggerService} from '../logger.service';
import {Subject} from 'rxjs/Subject';

export class Url {
    constructor(private protocol: string, private baseUrl: string, private endPoint: string)  {

    }

    get url(): string {
        return `${this.protocol}://${this.baseUrl}${this.endPoint}`;
    }

    public getHttpUrl(): Url {
        return new Url(this.protocol === 'ws' ? 'http' : 'https', this.baseUrl, this.endPoint);
    }

    public getUrlWith(queryParameters: QueryParams): string {
        let buffer = '';

        // tslint:disable-next-line:forin
        for (let propt in queryParameters) {
            if (buffer.length > 0) {
                buffer += '&';
            }

            buffer += propt + '=' + encodeURIComponent(queryParameters[propt]);
        }

        return this.url + (queryParameters ? '?' + buffer : '');
    }
}

export abstract class Protocol {

    protected _open: Subject<any> = new Subject<any>();
    protected _close: Subject<any> = new Subject<any>();
    protected _message: Subject<any> = new Subject<any>();
    protected _error: Subject<any> = new Subject<any>();

    constructor(protected loggerService: LoggerService, private protocolConfig?: ProtocolConfig ) {
        if (!protocolConfig) {
            this.protocolConfig = {
                initialTimeout: 500,
                maxTimeout: 300000,
            };
        }
    }

    abstract start(queryParameters?: QueryParams): void;
    abstract destroy(): void;

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

    protected reconnect(): void {
        this.destroy();
        setTimeout( this.start(), 0);
    }

    // Exponential Backoff Formula by Prof. Douglas Thain
    // http://dthain.blogspot.co.uk/2009/02/exponential-backoff-in-distributed.html
    protected getBackoffDelay(attempt): number {
        if (this.protocolConfig && !this.protocolConfig.timeWaitToReconnect) {
            let R = Math.random() + 1;
            let T = this.protocolConfig.initialTimeout;
            let F = 2;
            let N = attempt;
            let M = this.protocolConfig.maxTimeout;

            return Math.floor(Math.min(R * T * Math.pow(F, N), M));
        }else {
            return this.protocolConfig.timeWaitToReconnect;
        }
    }
}

export interface ProtocolConfig {
    initialTimeout?: number;
    maxTimeout?: number;
    timeWaitToReconnect?: number;
}

export interface QueryParams {
    [key: string]: any;
}