import { Injectable } from '@angular/core';
import { Headers } from '@angular/http';
import { UserModel } from './shared/user.model';
import { LoggerService } from './logger.service';
import { HttpHeaders } from '@angular/common/http';
import { RequestOptionsArgs } from '@angular/http/src/interfaces';

@Injectable()
export class ApiRoot {
    // Points to {baseUrl}/api/v1
    defaultSiteUrl: string;
    baseUrl = 'http://localhost:8080/';
    siteId = '48190c8c-42c4-46af-8d1a-0cd5db894797';
    authUser: UserModel;
    authToken: string;
    hideFireOn = false;
    hideRulePushOptions = false;

    static createAuthToken(authUser: UserModel): string {
        let token = null;
        if (authUser && authUser.username && authUser.password) {
            token = 'Basic ' + btoa(authUser.username + ':' + authUser.password);
        }
        return token;
    }

    static parseQueryParam(query: string, token: string): string {
        let idx = -1;
        let result = null;
        token = token + '=';
        if (query && query.length) {
            idx = query.indexOf(token);
        }
        if (idx >= 0) {
            let end = query.indexOf('&', idx);
            end = end !== -1 ? end : query.length;
            result = query.substring(idx + token.length, end);
        }
        return result;
    }

    constructor(authUser: UserModel, private loggerService: LoggerService) {
        this.authUser = authUser;
        this.authToken = ApiRoot.createAuthToken(authUser);
        try {
            let query = document.location.search.substring(1);
            if (query === '') {
                if (document.location.hash.indexOf('?') >= 0) {
                    query = document.location.hash.substr(document.location.hash.indexOf('?') + 1);
                }
            }
            const siteId = ApiRoot.parseQueryParam(query, 'realmId');
            if (siteId) {
                this.siteId = siteId;
                this.loggerService.debug('Site Id set to ', this.siteId);
            }
            const hideFireOn = ApiRoot.parseQueryParam(query, 'hideFireOn');
            if (hideFireOn) {
                this.hideFireOn = hideFireOn === 'true' || hideFireOn === '1';
                this.loggerService.debug('hideFireOn set to ', this.hideFireOn);
            }

            const hideRulePushOptions = ApiRoot.parseQueryParam(query, 'hideRulePushOptions');
            if (hideRulePushOptions) {
                this.hideRulePushOptions =
                    hideRulePushOptions === 'true' || hideRulePushOptions === '1';
                this.loggerService.debug('hideRulePushOptions set to ', this.hideRulePushOptions);
            }

            const baseUrl = ApiRoot.parseQueryParam(query, 'baseUrl');
            this.loggerService.debug('Proxy server Base URL set to ', baseUrl);
            this.setBaseUrl(baseUrl); // if null, just uses the base of the current URL
            this.configureUser(query, authUser);
        } catch (e) {
            this.loggerService.error('Could not set baseUrl automatically.', e);
        }
    }

    getDefaultRequestHeaders(): HttpHeaders {
        let headers = new HttpHeaders()
            .set('com.dotmarketing.session_host', this.siteId)
            .set('Accept', '*/*');
        if (this.authToken) {
            headers = headers.set('Authorization', this.authToken);
        }
        return headers;
    }

    getDefaultRequestOptions(): RequestOptionsArgs {
        const headers = new Headers();
        headers.append('com.dotmarketing.session_host', this.siteId);
        if (this.authToken) {
            headers.append('Authorization', this.authToken);
        }
        headers.append('Content-Type', 'application/json');

        return { headers: headers };
    }

    setBaseUrl(url = null): void {
        if (url === null) {
            // set to same as current request
            const loc = document.location;
            this.baseUrl = loc.protocol + '//' + loc.host + '/';
        } else if (url && url.startsWith('http://' || url.startsWith('https://'))) {
            this.baseUrl = url.endsWith('/') ? url : url + '/';
        } else {
            throw new Error("Invalid proxy server base url: '" + url + "'");
        }
        this.defaultSiteUrl = this.baseUrl + 'api/v1/sites/' + this.siteId;
    }

    private configureUser(query: string, user: UserModel): void {
        user.suppressAlerts = ApiRoot.parseQueryParam(query, 'suppressAlerts') === 'true';
    }
}
