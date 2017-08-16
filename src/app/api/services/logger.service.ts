import { Injectable } from '@angular/core';
import { Logger } from 'angular2-logger/core';
import { Config } from '../util/config';
import { StringUtils } from '../util/string.utils';
import { environment } from '../../../environments/environment';
import { HttpRequestUtils } from '../util/httpRequestUtils';
const DEV_MODE_PARAM = 'devMode';

/**
 * LoggerService to log.  Allows logger to be changed at runtime
 * To set the logger level in the console run logger.level=logger.Level.DEBUG
 */
@Injectable()
export class LoggerService {
    private showLogs = true;
    private httpRequestUtils: HttpRequestUtils;

    constructor(
        private config: Config,
        private logger: Logger,
        private stringUtils: StringUtils,
    ) {
        logger.info('Setting the logger...');
        this.httpRequestUtils = new HttpRequestUtils();
        this.showLogs = this.shouldShowLogs();

        if (this.showLogs) {
            logger.info('Developer mode logger on');
            logger.level = logger.Level.LOG;
        }
    }

    info(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.info(this.wrapMessage(message), optionalParams);
        } else {
            this.logger.info(this.wrapMessage(message));
        }
    }

    error(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.error(this.wrapMessage(message), optionalParams);
        } else {
            this.logger.error(this.wrapMessage(message));
        }
    }

    warn(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.warn(this.wrapMessage(message), optionalParams);
        } else {
            this.logger.warn(this.wrapMessage(message));
        }
    }

    debug(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.debug(this.wrapMessage(message), optionalParams);
        } else {
            this.logger.debug(this.wrapMessage(message));
        }
    }

    /**
     * Determine if angular should show lo
     * @returns {boolean}
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
