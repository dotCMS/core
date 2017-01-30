import {Injectable} from '@angular/core';
import {Logger} from 'angular2-logger/core';
import {CONSTANT} from '../../view/app.constant.dev';

/**
 * LoggerService to log.  Allows logger to be changed at runtime
 * To set the logger level in the console run logger.level=logger.Level.DEBUG
 */
@Injectable()
export class LoggerService {

    constructor(private logger: Logger) {

        console.log('Setting the logger');
        if (CONSTANT.ENV !== 'PROD') {

            console.log('Developer mode logger on');
            logger.level = logger.Level.LOG;
        }

    }

    info(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.info(message, optionalParams);
        } else {
            this.logger.info(message);
        }
    }

    error(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.error(message, optionalParams);
        } else {
            this.logger.error(message);
        }
    }

    warn(message?: any, ...optionalParams: any[]): void {
        if (optionalParams && optionalParams.length > 0) {
            this.logger.warn(message, optionalParams);
        } else {
            this.logger.warn(message);
        }
    }

    debug(message?: any, ...optionalParams: any[]): void {

        if (optionalParams && optionalParams.length > 0) {
            this.logger.debug(message, optionalParams);
        } else {
            this.logger.debug(message);
        }
    }

}