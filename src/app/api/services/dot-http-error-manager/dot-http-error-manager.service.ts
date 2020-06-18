import {Observable, of} from 'rxjs';
import { DotRouterService } from '../dot-router/dot-router.service';
import { DotMessageService } from '../dot-message/dot-messages.service';
import { Injectable } from '@angular/core';

import { ResponseView, LoginService, HttpCode } from 'dotcms-js';

import { DotAlertConfirmService } from '../dot-alert-confirm';
import { Response } from '@angular/http';

export interface DotHttpErrorHandled {
    redirected: boolean;
    status: HttpCode;
}

/**
 * Handle the UI for http errors messages
 *
 * @export
 * @class DotHttpErrorManagerService
 */
@Injectable()
export class DotHttpErrorManagerService {
    private readonly errorHandlers;

    constructor(
        private dotDialogService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService
    ) {
        if (!this.errorHandlers) {
            this.errorHandlers = {};
            this.errorHandlers[HttpCode.NOT_FOUND] = this.handleNotFound.bind(this);
            this.errorHandlers[HttpCode.UNAUTHORIZED] = this.handleUnathorized.bind(this);
            this.errorHandlers[HttpCode.FORBIDDEN] = this.handleForbidden.bind(this);
            this.errorHandlers[HttpCode.SERVER_ERROR] = this.handleServerError.bind(this);
            this.errorHandlers[HttpCode.BAD_REQUEST] = this.handleBadRequestError.bind(this);
            this.errorHandlers[HttpCode.NO_CONTENT] = this.handleNotContentError.bind(this);
        }
    }

    /**
     * Handle the http error message and return a true if it did a redirect
     *
     * @param ResponseView err
     * @returns Observable<boolean>
     * @memberof DotHttpErrorManagerService
     */
    handle(err: ResponseView): Observable<DotHttpErrorHandled> {
        const result: DotHttpErrorHandled = {
            redirected: this.callErrorHandler(err.response),
            status: err.status
        };

        if (
            err['bodyJsonObject'].error &&
            this.contentletIsForbidden(err['bodyJsonObject'].error)
        ) {
            result.status = HttpCode.FORBIDDEN;
        }
        return of(result);
    }

    private callErrorHandler(response: Response): boolean {
        const code = response.status;

        return code === HttpCode.FORBIDDEN
            ? this.isLicenseError(response)
                ? this.handleLicense()
                : this.handleForbidden()
            : this.errorHandlers[code](response);
    }

    private contentletIsForbidden(error: string): boolean {
        return (
            error.indexOf('does not have permissions READ') > -1 ||
            error.indexOf('User cannot edit') > -1
        );
    }

    private isLicenseError(response: Response): boolean {
        return (
            response.headers &&
            response.headers.get('error-key') === 'dotcms.api.error.license.required'
        );
    }

    private handleForbidden(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.403.message'),
            header: this.dotMessageService.get('dot.common.http.error.403.header')
        });
        return false;
    }

    private handleLicense(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.403.license.message'),
            header: this.dotMessageService.get('dot.common.http.error.403.license.header')
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

    private handleBadRequestError(response: Response): boolean {
        this.dotDialogService.alert({
            message:
                this.getErrorMessage(response) ||
                response.json()['message'] ||
                this.dotMessageService.get('dot.common.http.error.400.message'),
            header: this.dotMessageService.get('dot.common.http.error.400.header')
        });
        return false;
    }

    private handleNotContentError(): boolean {
        this.dotDialogService.alert({
            message: this.dotMessageService.get('dot.common.http.error.204.message'),
            header: this.dotMessageService.get('dot.common.http.error.204.header')
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

    private getErrorMessage(response: Response): string {
        return response.json()['errors'] ? response.json()['errors'][0].message : null;
    }
}
