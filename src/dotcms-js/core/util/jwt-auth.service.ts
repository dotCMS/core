import {Inject, Injectable} from '@angular/core';
import {NotificationService} from './notification.service';
import {LoggerService} from './logger.service';
import {Http, Headers, Response, RequestMethod, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs';
import {SettingsStorageService} from './settings-storage.service';

import 'rxjs/add/operator/map';

/**
 * Service for managing JWT Auth Token from dotCMS Site/Host
 */
@Injectable()
@Inject('http')
@Inject('notificationService')
@Inject('log')
@Inject('settingsStorageService')
export class JWTAuthService {

    constructor
    (
        private http: Http,
        private notificationService: NotificationService,
        private log: LoggerService,
        private settingsStorageService: SettingsStorageService
    ) {}

    /**
     * Will POST to the dotCMS to retrieve a dotCMS Auth Token
     * @param siteURL Site/Host of dotCMS
     * @param username
     * @param password
     * @returns {Observable<R>} String return for the token
     */
    getJWT(siteURL: string, username: string, password: string): Observable<string> {
        let data = {
            expirationDays: 30,
            password: password,
            user: username,
        };
        return this.doPostAuth(siteURL, data)
            .map((res: Response) => {
                if (res.status < 200 || res.status >= 300) {
                    this.handleError(res);
                    throw new Error('This request has failed ' + res.status);
                }
                return this.extractJWT(res);
            })
            .catch(error => this.handleError(error));
    }

    /**
     * Will login and save the Auth Token to local storage
     * @param siteURL
     * @param username
     * @param password
     * @returns {Observable<R>}
     */
    login(siteURL: string, username: string, password: string): Observable<string> {
        return this.getJWT(siteURL, username, password)
            .map(token => {
                this.settingsStorageService.storeSettings(siteURL, token);
                return token;
            });
    }

    private doPostAuth(siteUrl: string, data: any): Observable<Response> {
        let opts: RequestOptions = new RequestOptions();
        opts.method = RequestMethod.Post;
        opts.headers = new Headers({'Content-Type': 'application/json'});
        return this.http.post(siteUrl + '/api/v1/authentication/api-token', JSON.stringify(data), opts);
    }

    private extractJWT(res: Response): string {
        let token: string;
        let obj = JSON.parse(res.text());
        let results: string = obj.entity.token;
        return results;
    }

    private handleError(error: any): Observable<string> {
        let errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            this.log.error(errMsg);
            this.notificationService.displayErrorMessage('There was an error; please try again : ' + errMsg);
            return Observable.throw(errMsg);
        }
    }

}
