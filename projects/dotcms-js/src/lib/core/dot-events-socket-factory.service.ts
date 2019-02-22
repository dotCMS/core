import { Injectable } from '@angular/core';
import { DotcmsConfig } from './dotcms-config.service';
import { LoggerService } from './logger.service';
import { Observable, of } from 'rxjs';
import { CoreWebService } from './core-web.service';
import { DotEventsSocket } from './util/dot-event-socket';
import { map } from 'rxjs/operators';
import { DotEventsSocketURL } from './util/models/dot-event-socket-url';

/**
 * Factory of {@link DotEventsSocket}
 */
@Injectable()
export class DotEventsSocketFactoryService {
    private socket: DotEventsSocket;

    constructor(
        private dotcmsConfig: DotcmsConfig,
        private loggerService: LoggerService,
        private coreWebService: CoreWebService
    ) {}

    /**
     * Return a {@link DotEventsSocket} getting the web sockets configuration parameters from back end,
     * and also ensuring that just exist one instance
     */
    public createSocket(): Observable<DotEventsSocket> {
        this.loggerService.debug('Creating socket object');

        return this.socket ? of(this.socket) : this.createDotEventsSocket();
    }

    public clean(): void {
        this.socket = null;
    }

    private createDotEventsSocket(): Observable<DotEventsSocket> {
        return this.dotcmsConfig.getConfig().pipe(
            map((configParams) => {
                const url: DotEventsSocketURL = new DotEventsSocketURL(
                    configParams.websocketProtocol,
                    configParams.websocketBaseURL,
                    configParams.websocketsSystemEventsEndpoint
                );

                this.socket = new DotEventsSocket(
                    url,
                    configParams,
                    this.loggerService,
                    this.coreWebService
                );

                return this.socket;
            })
        );
    }
}
