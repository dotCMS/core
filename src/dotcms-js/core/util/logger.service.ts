import {Inject, Injectable} from '@angular/core';
import {Logger} from 'angular2-logger/core';

/**
 * LoggerService to log.  Allows logger to be changed at runtime
 * To set the logger level in the console run logger.level=logger.Level.DEBUG
 */
@Injectable()
@Inject('logger')
export class LoggerService {

    constructor
    (
        private logger: Logger
    ) {
    }

    info(message: string): void {
        this.logger.info(message);
    }

    error(message: string): void {
        this.logger.error(message);
    }

    warn(message: string): void {
        this.logger.warn(message);
    }

    debug(message: string): void {
        this.logger.debug(message);
    }
}