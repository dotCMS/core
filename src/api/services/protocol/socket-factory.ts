import {Injectable} from '@angular/core';
import {Protocol, Url} from './protocol';
import {DotcmsConfig} from '../system/dotcms-config';
import {LoggerService} from '../logger.service';
import {EventsSocket} from './socket';
import {Observable} from 'rxjs/Rx';
import {CoreWebService} from '../core-web-service';

@Injectable()
export class ProtocolFactory {

    constructor(private dotcmsConfig: DotcmsConfig, private loggerService: LoggerService, private coreWebService: CoreWebService) {

    }

    get socket$(): Observable<Protocol> {
        return  this.dotcmsConfig.getConfig().map( configParams => {
            let url: Url = new Url(configParams.websocketProtocol, configParams.websocketBaseURL,
                configParams.websocketsSystemEventsEndpoint);

            return new EventsSocket(url, configParams, this.loggerService, this.coreWebService);
        });
    }
}
