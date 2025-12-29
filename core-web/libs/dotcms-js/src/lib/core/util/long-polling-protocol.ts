import { HttpClient, HttpParams } from '@angular/common/http';

import { map, take } from 'rxjs/operators';

import { Protocol } from './protocol';

import { LoggerService } from '../logger.service';

interface LongPollingResponse<T> {
    entity: T;
}

export class LongPollingProtocol extends Protocol {
    private isClosed = false;
    private isAlreadyOpen = false;
    private lastCallback: number;

    constructor(
        private url: string,
        loggerService: LoggerService,
        private http: HttpClient
    ) {
        super(loggerService);
    }

    /**
     * Connect to a Long Polling connection
     */
    connect(): void {
        this.connectLongPooling();
    }

    /**
     * Close the connection
     */
    close(): void {
        this.loggerService.info('destroying long polling');
        this.isClosed = true;
        this.isAlreadyOpen = false;
        this._close.next();
    }

    private getLastCallback(data): number {
        this.lastCallback =
            data.length > 0 ? data[data.length - 1].creationDate + 1 : this.lastCallback;

        return this.lastCallback;
    }

    private connectLongPooling(lastCallBack?: number): void {
        this.isClosed = false;
        this.loggerService.info('Starting long polling connection');

        let params = new HttpParams();
        if (lastCallBack) {
            params = params.set('lastCallBack', lastCallBack.toString());
        }

        this.http
            .get<LongPollingResponse<unknown>>(this.url, { params })
            .pipe(
                map((response) => response.entity),
                take(1)
            )
            .subscribe(
                (data) => {
                    this.loggerService.debug('new Events', data);
                    this.triggerOpen();

                    if (data instanceof Array) {
                        data.forEach((message) => {
                            this._message.next(message);
                        });
                    } else {
                        this._message.next(data);
                    }

                    if (!this.isClosed) {
                        this.connectLongPooling(this.getLastCallback(data));
                    }
                },
                (e) => {
                    this.loggerService.info('A error occur connecting through long polling');
                    this._error.next(e);
                }
            );
    }

    private triggerOpen(): void {
        if (!this.isAlreadyOpen) {
            this._open.next(true);
            this.isAlreadyOpen = true;
        }
    }
}
