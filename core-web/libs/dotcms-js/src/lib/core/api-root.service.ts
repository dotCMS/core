import { Injectable, inject } from '@angular/core';

import { LoggerService } from './logger.service';
import { UserModel } from './shared/user.model';

@Injectable()
export class ApiRoot {
    private loggerService = inject(LoggerService);

    siteId = '48190c8c-42c4-46af-8d1a-0cd5db894797';
    authUser: UserModel;
    hideFireOn = false;
    hideRulePushOptions = false;

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

    constructor() {
        const authUser = inject(UserModel);

        this.authUser = authUser;

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

            this.configureUser(query, authUser);
        } catch (e) {
            this.loggerService.error('Could not set baseUrl automatically.', e);
        }
    }

    private configureUser(query: string, user: UserModel): void {
        user.suppressAlerts = ApiRoot.parseQueryParam(query, 'suppressAlerts') === 'true';
    }
}
