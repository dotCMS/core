import { Protocol } from './protocol';
import { LoggerService } from '../logger.service';
import { CoreWebService } from '../core-web.service';
import { RequestMethod } from '@angular/http';
import { pluck } from 'rxjs/operators';

export class LongPollingProtocol extends Protocol {
    private isClosed = false;

    constructor(
        private url: string,
        loggerService: LoggerService,
        private coreWebService: CoreWebService
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
        this._close.next();
    }

    private getLastCallback(data): number {
        return data.length > 0 ? data[data.length - 1].creationDate + 1 : undefined;
    }

    private connectLongPooling(lastCallBack?: number): void {
        this.isClosed = false;
        this.loggerService.info('Starting long polling connection');

        this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.url,
                params: lastCallBack ? {lastCallBack: lastCallBack} : {}
            })
            .pipe(pluck('entity'))
            .subscribe(
                (data) => {
                    this.loggerService.debug('new Events', data);
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
}
