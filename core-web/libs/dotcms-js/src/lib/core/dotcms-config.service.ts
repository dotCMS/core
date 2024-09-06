import { Observable, BehaviorSubject } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck, filter, map, take } from 'rxjs/operators';

import { CoreWebService } from './core-web.service';
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

export interface DotUiColors {
    primary: string;
    secondary: string;
    background: string;
}

export interface ConfigParams {
    colors: DotUiColors;
    emailRegex: string;
    license: {
        displayServerId: string;
        isCommunity: boolean;
        level: number;
        levelName: string;
    };
    logos: {
        loginScreen: string;
        navBar: string;
    };
    menu: Menu[];
    paginatorLinks: number;
    paginatorRows: number;
    releaseInfo: {
        buildDate: string;
        version: string;
    };
    websocket: WebSocketConfigParams;
}

export interface WebSocketConfigParams {
    disabledWebsockets: boolean;
    websocketReconnectTime: number;
}

export interface DotTimeZone {
    id: string;
    label: string;
    offset: string;
}

@Injectable()
export class DotcmsConfigService {
    private configParamsSubject: BehaviorSubject<ConfigParams> = new BehaviorSubject(null);
    private configUrl: string;

    /**
     * Initializes this class with the dotCMS core configuration parameters.
     *
     * @param configParams - The configuration properties for the current instance.
     */
    constructor(
        private coreWebService: CoreWebService,
        private loggerService: LoggerService
    ) {
        this.configUrl = 'v1/appconfiguration';
        this.loadConfig();
    }

    getConfig(): Observable<ConfigParams> {
        return this.configParamsSubject
            .asObservable()
            .pipe(filter((config: ConfigParams) => !!config));
    }

    loadConfig(): void {
        this.loggerService.debug('Loading configuration on: ', this.configUrl);

        this.coreWebService
            .requestView({
                url: this.configUrl
            })
            .pipe(pluck('entity'))
            .subscribe((res: any) => {
                this.loggerService.debug('Configuration Loaded!', res);

                const configParams: ConfigParams = {
                    colors: res.config.colors,
                    emailRegex: res.config[EMAIL_REGEX],
                    license: res.config.license,
                    logos: res.config.logos,
                    menu: res.menu,
                    paginatorLinks: res.config[DOTCMS_PAGINATOR_LINKS],
                    paginatorRows: res.config[DOTCMS_PAGINATOR_ROWS],
                    releaseInfo: {
                        buildDate: res.config.releaseInfo?.buildDate,
                        version: res.config.releaseInfo?.version
                    },
                    websocket: {
                        websocketReconnectTime:
                            res.config.websocket[DOTCMS_WEBSOCKET_RECONNECT_TIME],
                        disabledWebsockets: res.config.websocket[DOTCMS_DISABLE_WEBSOCKET_PROTOCOL]
                    }
                };

                this.configParamsSubject.next(configParams);

                this.loggerService.debug('this.configParams', configParams);

                return res;
            });
    }

    /**
     * Return a list of timezones.
     * @returns Observable<DotTimeZone[]>
     * @memberof DotcmsConfigService
     */
    getTimeZones(): Observable<DotTimeZone[]> {
        return this.coreWebService
            .requestView({
                url: this.configUrl
            })
            .pipe(
                pluck('entity', 'config', 'timezones'),
                map((timezones: DotTimeZone[]) => {
                    return timezones.sort((a: DotTimeZone, b: DotTimeZone) => {
                        if (a.label > b.label) {
                            return 1;
                        }

                        if (a.label < b.label) {
                            return -1;
                        }

                        // a must be equal to b
                        return 0;
                    });
                })
            );
    }

    /**
     * Return the system Timezone.
     * @returns Observable<DotTimeZone[]>
     * @memberof DotcmsConfigService
     */
    getSystemTimeZone(): Observable<DotTimeZone> {
        return this.coreWebService
            .requestView({
                url: this.configUrl
            })
            .pipe(pluck('entity', 'config', 'systemTimezone'), take(1));
    }
}
