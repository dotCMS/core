import {Injectable} from '@angular/core';
import {Protocol, Url} from './protocol';
import {DotcmsConfig} from '../system/dotcms-config';
import {LoggerService} from '../logger.service';
import {EventsSocket} from './socket';
import {Observable, Subject} from 'rxjs/Rx';
import {CoreWebService} from '../core-web-service';

@Injectable()
export class SocketFactory {

    private _socket: Subject<Protocol> = new Subject<Protocol>();

    constructor(private dotcmsConfig: DotcmsConfig, private loggerService: LoggerService,
                private coreWebService: CoreWebService) {
    }

    get socket$(): Observable<Protocol> {
        return this._socket.asObservable();
    }

    public createSocket(): void {

        this.dotcmsConfig.getConfig().subscribe( configParams => {
            let url: Url = new Url(configParams.websocketProtocol, configParams.websocketBaseURL,
                configParams.websocketsSystemEventsEndpoint);

            this._socket.next(new EventsSocket(url, configParams, this.loggerService, this.coreWebService));
        });
    }
}
