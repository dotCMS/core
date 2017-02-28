import {Injectable} from '@angular/core';
import {Protocol, Url} from './protocol';
import {DotcmsConfig} from '../system/dotcms-config';
import {LoggerService} from '../logger.service';
import {EventsSocket} from './socket';
import {Observable, Subject} from 'rxjs/Rx';
import {CoreWebService} from '../core-web-service';

@Injectable()
export class SocketFactory {

    private socket: Protocol;

    constructor(private dotcmsConfig: DotcmsConfig, private loggerService: LoggerService,
                private coreWebService: CoreWebService) {
    }

    public createSocket(): Observable<Protocol> {

        this.loggerService.debug('Creating socket object');

        return Observable.create(observer => {
            this.dotcmsConfig.getConfig().subscribe( configParams => {
                let url: Url = new Url(configParams.websocketProtocol, configParams.websocketBaseURL,
                    configParams.websocketsSystemEventsEndpoint);

                this.loggerService.debug('is socket object created?', !this.socket);

                if (!this.socket) {
                    this.socket = new EventsSocket(url, configParams, this.loggerService, this.coreWebService);
                }

                observer.next(this.socket);
                observer.complete();
            });
        });
    }
}
