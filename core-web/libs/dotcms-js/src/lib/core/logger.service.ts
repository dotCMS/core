import { Injectable, inject } from '@angular/core';

import { StringUtils } from './string-utils.service';
import { HttpRequestUtils } from './util/http-request-utils';

import { environment } from '../environments/environment';
const DEV_MODE_PARAM = 'devMode';

/**
 * LoggerService to log.  Allows logger to be changed at runtime
 * To set the logger level in the console run logger.level=logger.Level.DEBUG
 */
@Injectable()
export class LoggerService {
    private stringUtils = inject(StringUtils);

    private showLogs = true;
    private httpRequestUtils: HttpRequestUtils;

    constructor() {
        this.httpRequestUtils = new HttpRequestUtils();
        this.showLogs = this.shouldShowLogs();

        if (this.showLogs) {
            console.info('Setting the logger --> Developer mode logger on');
        }
    }

    info(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            console.info(this.wrapMessage(message), optionalParams);
        } else {
            console.info(this.wrapMessage(message));
        }
    }

    error(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            console.error(this.wrapMessage(message), optionalParams);
        } else {
            console.error(this.wrapMessage(message));
        }
    }

    warn(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            console.warn(this.wrapMessage(message), optionalParams);
        } else {
            console.warn(this.wrapMessage(message));
        }
    }

    debug(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            console.debug(this.wrapMessage(message), optionalParams);
        } else {
            console.debug(this.wrapMessage(message));
        }
    }

    /**
     * Determine if angular should show lo
     * @returns boolean
     */
    shouldShowLogs(): boolean {
        const devMode: string = this.httpRequestUtils.getQueryStringParam(DEV_MODE_PARAM);

        return !environment.production || devMode === 'on';
    } // isProduction.

    private wrapMessage(message?: any): string {
        // on prod, we do not attach the caller.
        return this.showLogs ? message : this.getCaller() + '>> ' + message;
    }
    private getCaller(): string {
        let caller = 'unknown';
        try {
            throw new Error();
        } catch (e) {
            caller = this.cleanCaller(this.stringUtils.getLine(e.stack, 4));
        }

        return caller;
    }

    private cleanCaller(caller: string): string {
        return caller ? caller.trim().substr(3) : 'unknown';
    }
}
