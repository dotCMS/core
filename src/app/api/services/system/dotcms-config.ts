import {CoreWebService} from '../core-web-service';
import {RequestMethod} from '@angular/http';
import {Injectable} from '@angular/core';
import {Observable, Observer} from 'rxjs/Rx';
import {LoggerService} from '../logger.service';
import {Menu} from '../routing-service';

/**
 * Created by josecastro on 7/29/16.
 *
 * Wraps the configuration properties for dotCMS in order to provide an
 * easier way to access the information.
 *
 */
const DEFAULT_REST_PAGE_COUNT = 'DEFAULT_REST_PAGE_COUNT';
const DOTCMS_WEBSOCKET_RECONNECT_TIME = 'dotcms.websocket.reconnect.time';
const DOTCMS_WEBSOCKET_ENDPOINTS = 'dotcms.websocket.endpoints';
const WEBSOCKET_SYSTEMEVENTS_ENDPOINT = 'websocket.systemevents.endpoint';
const DOTCMS_WEBSOCKET_BASEURL = 'dotcms.websocket.baseurl';
const DOTCMS_WEBSOCKET_PROTOCOL = 'dotcms.websocket.protocol';
const DOTCMS_DISABLE_WEBSOCKET_PROTOCOL = 'dotcms.websocket.disable';
const DOTCMS_PAGINATOR_ROWS = 'dotcms.paginator.rows';
const DOTCMS_PAGINATOR_LINKS = 'dotcms.paginator.links';

export interface ConfigParams {
    disabledWebsockets: string;
    websocketReconnectTime: number;
    websocketEndpoints: string;
    websocketsSystemEventsEndpoint: string;
    websocketBaseURL: string;
    websocketProtocol: string;
    menu: Menu[];
    paginatorRows: number;
    paginatorLinks: number;
}

@Injectable()
export class DotcmsConfig {

    private waiting: Observer<any>[] = [];
    private configParams: ConfigParams;
    private configUrl: string;

    /**
     * Initializes this class with the dotCMS core configuration parameters.
     *
     * @param configParams - The configuration properties for the current instance.
     */
    constructor(private coreWebService: CoreWebService, private loggerService: LoggerService) {
        this.configUrl = 'v1/appconfiguration';
        this.loadConfig();
    }

    getConfig(): Observable<ConfigParams> {
        return Observable.create( obs => {
            if (this.configParams) {
                obs.next(this.configParams);
            } else {
                this.waiting.push(obs);
            }
        });
    }

    loadConfig(): void {

        this.loggerService.debug('Loading configuration on: ', this.configUrl);

        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.configUrl
        }).pluck('entity').subscribe((res: any) => {

            this.loggerService.debug('Configuration Loaded!', res);

            this.configParams = {
                disabledWebsockets: res.config[DOTCMS_DISABLE_WEBSOCKET_PROTOCOL],
                menu: res.menu,
                paginatorLinks: res.config[DOTCMS_PAGINATOR_LINKS],
                paginatorRows: res.config[DOTCMS_PAGINATOR_ROWS],
                websocketBaseURL: res.config[DOTCMS_WEBSOCKET_BASEURL],
                websocketEndpoints: res.config[DOTCMS_WEBSOCKET_ENDPOINTS],
                websocketProtocol: res.config[DOTCMS_WEBSOCKET_PROTOCOL],
                websocketReconnectTime: res.config[DOTCMS_WEBSOCKET_RECONNECT_TIME],
                websocketsSystemEventsEndpoint: res.config[DOTCMS_WEBSOCKET_ENDPOINTS][WEBSOCKET_SYSTEMEVENTS_ENDPOINT]
            };

            this.loggerService.debug('this.configParams', this.configParams);

            this.waiting.forEach(obs => obs.next(this.configParams));
            this.waiting = null;
            return res;
        });
    }
}
