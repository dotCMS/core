
import {Injectable} from '@angular/core';
import {Protocol, QueryParams, Url} from './protocol';
import {LoggerService} from '../logger.service';
import {CoreWebService} from '../core-web-service';
import {RequestMethod} from '@angular/http';

interface QueryFuncBuilder {
    (data: any): QueryParams;
}

export class LongPollingProtocol extends Protocol {

    private isClosed = false;

    constructor(private url: Url, loggerService: LoggerService, private coreWebService: CoreWebService,
        private queryBuilder?: QueryFuncBuilder) {
        super(loggerService);

        if (!queryBuilder) {
            this.queryBuilder = data => null;
        }
    }

    start(queryParameters?: QueryParams): void {
        this.isClosed = false;

        this.loggerService.info('Starting long polling connection');

        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.url.getUrlWith(queryParameters),
        }).pluck('entity').subscribe(data => {
            this.loggerService.debug('new Events', data);

            if (data instanceof Array) {
                data.forEach(message => {
                    this._message.next(message);
                });
            } else {
                this._message.next(data);
            }

            if (!this.isClosed) {
                let query: QueryParams = this.queryBuilder(data);
                this.start(query);
            }
        }, () => {
            this.loggerService.info('A error occur connecting through long polling');
            if (!this.isClosed) {
                this.start();
            }
        });
    }

    destroy(): void {
        this.loggerService.info('destroying long polling');
        this.isClosed = true;
    }
}