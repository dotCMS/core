import {Injectable} from '@angular/core';
import {Logger} from 'angular2-logger/core';
import {Config} from '../util/config';
import {StringUtils} from '../util/string.utils';


/**
 * LoggerService to log.  Allows logger to be changed at runtime
 * To set the logger level in the console run logger.level=logger.Level.DEBUG
 */
@Injectable()
export class LoggerService {

    private isProduction: boolean = true;

    constructor(private logger: Logger, private config: Config, private stringUtils : StringUtils) {

        console.log('Setting the logger...');

        this.isProduction = this.config.isProduction();

        if (!this.isProduction) {

            console.log('Developer mode logger on');
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

    private wrapMessage (message?: any): string {
        // on prod, we do not attach the caller.
        return this.isProduction ? message :
                                  this.getCaller() + '>> ' + message;
    }
    private getCaller (): string {

        let caller = 'unknown';
        try { throw new Error(); } catch (e) { caller = this.cleanCaller(this.stringUtils.getLine(e.stack, 4)); }
        return caller;
    }

    private cleanCaller (caller: string): string {

        return (caller) ? caller.trim().substr(3) : 'unknown';
    }

}