import { Injectable } from '@angular/core';
import { Protocol, Url } from './util/protocol';
import { DotcmsConfig } from './dotcms-config.service';
import { LoggerService } from './logger.service';
import { EventsSocket } from './util/socket';
import { Observable } from 'rxjs/Rx';
import { CoreWebService } from './core-web.service';

@Injectable()
export class SocketFactory {
    private socket: Protocol;

    constructor(
        private dotcmsConfig: DotcmsConfig,
        private loggerService: LoggerService,
        private coreWebService: CoreWebService
    ) {}

    public createSocket(): Observable<Protocol> {
        this.loggerService.debug('Creating socket object');

        return Observable.create(observer => {
            this.dotcmsConfig.getConfig().subscribe(configParams => {
                const url: Url = new Url(
                    configParams.websocketProtocol,
                    configParams.websocketBaseURL,
                    configParams.websocketsSystemEventsEndpoint
                );

                if (!this.socket) {
                    this.socket = new EventsSocket(
                        url,
                        configParams,
                        this.loggerService,
                        this.coreWebService
                    );
                }

                observer.next(this.socket);
                observer.complete();
            });
        });
    }

    public clean(): void {
        this.socket = null;
    }
}
