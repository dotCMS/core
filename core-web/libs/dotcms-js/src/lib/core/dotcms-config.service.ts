import { BehaviorSubject, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { filter, map, shareReplay, switchMap, take, tap } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { LoggerService } from './logger.service';
import { Menu } from './routing.service';

/**
 * Created by josecastro on 7/29/16.
 *
 * Wraps the configuration properties for dotCMS in order to provide an
 * easier way to access the information.
 *
 * @deprecated Use DotSystemConfigService from @dotcms/data-access instead.
 * This service will be removed in a future version.
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

export interface SystemTimezone {
    id: string;
    label: string;
    offset: string;
}

export interface ConfigParams {
    colors: DotUiColors;
    emailRegex: string;
    languageBundleVersion?: string;
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
    systemTimezone: SystemTimezone;
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

interface DotAppConfigResponse {
    config: {
        colors: DotUiColors;
        [EMAIL_REGEX]: string;
        languageBundleVersion?: string;
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
        [DOTCMS_PAGINATOR_LINKS]: number;
        [DOTCMS_PAGINATOR_ROWS]: number;
        releaseInfo?: {
            buildDate: string;
            version: string;
        };
        websocket: {
            [DOTCMS_WEBSOCKET_RECONNECT_TIME]: number;
            [DOTCMS_DISABLE_WEBSOCKET_PROTOCOL]: boolean;
        };
        systemTimezone: SystemTimezone;
        timezones: DotTimeZone[];
    };
    menu: Menu[];
}

/**
 * @deprecated Use DotSystemConfigService from @dotcms/data-access instead.
 * This service will be removed in a future version.
 *
 * @example
 * ```typescript
 * // Old way (deprecated)
 * private dotcmsConfigService = inject(DotcmsConfigService);
 *
 * // New way (recommended)
 * private systemConfigService = inject(DotSystemConfigService);
 * ```
 */
@Injectable({
    providedIn: 'root'
})
export class DotcmsConfigService {
    private http = inject(HttpClient);
    private loggerService = inject(LoggerService);

    private configParamsSubject: BehaviorSubject<ConfigParams> = new BehaviorSubject(null);
    private configRequest$: Observable<ConfigParams> | undefined;
    private configUrl: string;

    /**
     * Initializes this class with the dotCMS core configuration parameters.
     *
     * @param configParams - The configuration properties for the current instance.
     */
    constructor() {
        this.configUrl = '/api/v1/appconfiguration';
        this.loadConfig();
    }

    getConfig(): Observable<ConfigParams> {
        return this.configParamsSubject.pipe(
            switchMap((config) => (config ? of(config) : this.requestConfig()))
        );
    }

    /** Observes successful configuration loads without starting or retrying a request. */
    getConfigUpdates(): Observable<ConfigParams> {
        return this.configParamsSubject.pipe(filter((config): config is ConfigParams => !!config));
    }

    loadConfig(): void {
        // Preserve eager loading without leaving constructor/refresh errors unhandled.
        this.requestConfig().subscribe({
            error: (error) => this.loggerService.error('Failed to load configuration', error)
        });
    }

    private requestConfig(): Observable<ConfigParams> {
        if (this.configRequest$) {
            return this.configRequest$;
        }

        this.loggerService.debug('Loading configuration on: ', this.configUrl);

        this.configRequest$ = this.http
            .get<DotCMSResponse<DotAppConfigResponse>>(this.configUrl)
            .pipe(
                map((response) => response.entity),
                map((res: DotAppConfigResponse) => {
                    this.loggerService.debug('Configuration Loaded!', res);

                    const configParams: ConfigParams = {
                        colors: res.config.colors,
                        emailRegex: res.config[EMAIL_REGEX],
                        languageBundleVersion: res.config.languageBundleVersion,
                        license: res.config.license,
                        logos: res.config.logos,
                        menu: res.menu,
                        paginatorLinks: res.config[DOTCMS_PAGINATOR_LINKS],
                        paginatorRows: res.config[DOTCMS_PAGINATOR_ROWS],
                        releaseInfo: {
                            buildDate: res.config.releaseInfo?.buildDate ?? '',
                            version: res.config.releaseInfo?.version ?? ''
                        },
                        websocket: {
                            websocketReconnectTime:
                                res.config.websocket[DOTCMS_WEBSOCKET_RECONNECT_TIME],
                            disabledWebsockets:
                                res.config.websocket[DOTCMS_DISABLE_WEBSOCKET_PROTOCOL]
                        },
                        systemTimezone: res.config.systemTimezone
                    };

                    return configParams;
                }),
                tap({
                    next: (configParams) => {
                        this.configParamsSubject.next(configParams);
                        this.loggerService.debug('this.configParams', configParams);
                    },
                    // Clear before notifying subscribers so even an immediate retry gets a new request.
                    error: () => (this.configRequest$ = undefined),
                    complete: () => (this.configRequest$ = undefined)
                }),
                shareReplay({ bufferSize: 1, refCount: false })
            );

        return this.configRequest$;
    }

    /**
     * Return a list of timezones.
     * @returns Observable<DotTimeZone[]>
     * @memberof DotcmsConfigService
     */
    getTimeZones(): Observable<DotTimeZone[]> {
        return this.http.get<DotCMSResponse<DotAppConfigResponse>>(this.configUrl).pipe(
            map((response) => response.entity.config.timezones),
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
        return this.http.get<DotCMSResponse<DotAppConfigResponse>>(this.configUrl).pipe(
            map((response) => response.entity.config.systemTimezone),
            take(1)
        );
    }
}
