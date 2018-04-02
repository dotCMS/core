import { DotRouterService } from '../dot-router/dot-router.service';
import { DotMessageService } from '../dot-messages-service';
import { Injectable } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import { ResponseView, LoginService, HttpCode } from 'dotcms-js/dotcms-js';

import { DotDialogService } from '../dot-dialog';

export interface DotHttpErrorHandled {
    redirected: boolean;
    forbidden?: boolean;
}

/**
 * Handle the UI for http errors messages
 *
 * @export
 * @class DotHttpErrorManagerService
 */
@Injectable()
export class DotHttpErrorManagerService {
    constructor(
        private dotDialogService: DotDialogService,
        private dotMessageService: DotMessageService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService
    ) {
        this.dotMessageService
            .getMessages([
                'dot.common.http.error.403.header',
                'dot.common.http.error.403.message',
                'dot.common.http.error.404.header',
                'dot.common.http.error.404.message',
                'dot.common.http.error.500.header',
                'dot.common.http.error.500.message'
            ])
            .subscribe();
    }

    /**
     * Handle the http error message and return a true if it did a redirect
     *
     * @param {ResponseView} err
     * @returns {Observable<boolean>}
     * @memberof DotHttpErrorManagerService
     */
    handle(err: ResponseView): Observable<DotHttpErrorHandled> {
        const result: DotHttpErrorHandled = {
            redirected: this.callErrorHandler(err.response.status)
        };

        if (err['bodyJsonObject'].error) {
            result.forbidden = this.contentletIsForbidden(err['bodyJsonObject'].error);
        }

        return Observable.of(result);
    }

    private callErrorHandler(code: HttpCode): boolean {
        const errors = {};

        errors[HttpCode.NOT_FOUND] = this.handleNotFound.bind(this);
        errors[HttpCode.UNAUTHORIZED] = this.handleUnathorized.bind(this);
        errors[HttpCode.FORBIDDEN] = this.handleForbidden.bind(this);
        errors[HttpCode.SERVER_ERROR] = this.handleServerError.bind(this);

        return errors[code]();
    }

    private contentletIsForbidden(error: string): boolean {
        return (
            error.indexOf('does not have permissions READ') > -1 ||
            error.indexOf('User cannot edit') > -1
        );
    }

    private handleForbidden(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.403.message'),
            header: this.dotMessageService.get('dot.common.http.error.403.header')
        });
        return false;
    }

    private handleNotFound(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.404.message'),
            header: this.dotMessageService.get('dot.common.http.error.404.header')
        });
        return false;
    }

    private handleServerError(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.500.message'),
            header: this.dotMessageService.get('dot.common.http.error.500.header')
        });
        return false;
    }

    private handleUnathorized(): boolean {
        if (this.loginService.auth.user) {
            this.handleForbidden();
        } else {
            this.dotRouterService.goToLogin();
            return true;
        }
        return false;
    }
}
