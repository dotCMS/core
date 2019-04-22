import { CoreWebService } from './core-web.service';
import { RequestMethod } from '@angular/http';
import { Injectable } from '@angular/core';
import { Observable, Observer } from 'rxjs';
import { pluck } from 'rxjs/operators';
import { LoggerService } from './logger.service';
import { Menu } from './routing.service';

/**
 * Created by josecastro on 7/29/16.
 *
 * Wraps the configuration properties for dotCMS in order to provide an
 * easier way to access the information.
 *
 */
const DOTCMS_WEBSOCKET_RECONNECT_TIME = 'dotcms.websocket.reconnect.time';
const DOTCMS_DISABLE_WEBSOCKET_PROTOCOL = 'dotcms.websocket.disable';
const DOTCMS_PAGINATOR_ROWS = 'dotcms.paginator.rows';
const DOTCMS_PAGINATOR_LINKS = 'dotcms.paginator.links';
const EMAIL_REGEX = 'emailRegex';

export interface ConfigParams {
    colors: object;
    emailRegex: string;
    license: object;
    menu: Menu[];
    paginatorLinks: number;
    paginatorRows: number;
    websocket: WebSocketConfigParams;
}

export interface WebSocketConfigParams {
    disabledWebsockets: string;
    websocketReconnectTime: number;
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
        return Observable.create((obs) => {
            if (this.configParams) {
                obs.next(this.configParams);
            } else {
                this.waiting.push(obs);
            }
        });
    }

    loadConfig(): void {
        this.loggerService.debug('Loading configuration on: ', this.configUrl);

        this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.configUrl
            })
            .pipe(pluck('entity'))
            .subscribe((res: any) => {
                this.loggerService.debug('Configuration Loaded!', res);

                this.configParams = {
                    colors: res.config.colors,
                    emailRegex: res.config[EMAIL_REGEX],
                    license: res.config.license,
                    menu: res.menu,
                    paginatorLinks: res.config[DOTCMS_PAGINATOR_LINKS],
                    paginatorRows: res.config[DOTCMS_PAGINATOR_ROWS],
                    websocket: {
                        websocketReconnectTime: res.config[DOTCMS_WEBSOCKET_RECONNECT_TIME],
                        disabledWebsockets: res.config[DOTCMS_DISABLE_WEBSOCKET_PROTOCOL],
                    }
                };

                this.loggerService.debug('this.configParams', this.configParams);

                this.waiting.forEach((obs) => obs.next(this.configParams));
                this.waiting = null;
                return res;
            });
    }
}
