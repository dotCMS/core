import {Injectable} from '@angular/core';
import {LoggerService} from '../services/logger.service';

@Injectable()
export class UserModel {
  username: string;
  password: string;
  locale: string;
  suppressAlerts = false;

  constructor(private loggerService: LoggerService) {

    this.locale = process.env.DEFAULT_LOCALE; // default to 'en-US'
    try {
      let url = window.location.search.substring(1);
      this.locale = this.checkQueryForUrl(url);
    } catch (e) {
      this.loggerService.error('Could not set locale from URL.');
    }
  }

  checkQueryForUrl(locationQuery: string): string {
    let locale = this.locale;
    if (locationQuery && locationQuery.length) {
      let q = locationQuery;
      let token = 'locale=';
      let idx = q.indexOf(token);
      if (idx >= 0) {
        let end = q.indexOf('&', idx);
        end = end !== -1 ? end : q.indexOf('#', idx);
        end = end !== -1 ? end : q.length;
        locale = q.substring(idx + token.length, end);
      }
    }
    return locale;
  }

}