import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { HttpCode, LoginService } from '@dotcms/dotcms-js';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

import { DotAlertConfirmService } from '../dot-alert-confirm/dot-alert-confirm.service';
import { DotMessageDisplayService } from '../dot-message-display/dot-message-display.service';
import { DotMessageService } from '../dot-messages/dot-messages.service';
import { DotRouterService } from '../dot-router/dot-router.service';

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
    private dotDialogService = inject(DotAlertConfirmService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotMessageService = inject(DotMessageService);
    private loginService = inject(LoginService);
    private dotRouterService = inject(DotRouterService);

    private readonly errorHandlers?: Record<HttpCode, (response?: HttpErrorResponse) => boolean>;
    private _unobtrusive = false;

    constructor() {
        if (!this.errorHandlers) {
            this.errorHandlers = {
                [HttpCode.NOT_FOUND]: this.handleNotFound.bind(this),
                [HttpCode.UNAUTHORIZED]: this.handleUnathorized.bind(this),
                [HttpCode.FORBIDDEN]: this.handleForbidden.bind(this),
                [HttpCode.SERVER_ERROR]: this.handleServerError.bind(this),
                [HttpCode.BAD_REQUEST]: this.handleBadRequestError.bind(this),
                [HttpCode.NO_CONTENT]: this.handleNotContentError.bind(this)
            };
        }
    }

    /**
     * Handle the http error message and return a true if it did a redirect
     *
     * @param ResponseView err
     * @param boolean unobtrusive
     * @returns Observable<boolean>
     * @memberof DotHttpErrorManagerService
     */
    handle(err: HttpErrorResponse, unobtrusive = false): Observable<DotHttpErrorHandled> {
        this._unobtrusive = unobtrusive;

        const result: DotHttpErrorHandled = {
            redirected: this.callErrorHandler(err),
            status: err.status
        };

        const error = err.error?.errors ? err.error.errors[0] : err.error;

        if (error && this.contentletIsForbidden(this.getErrorMessage(err))) {
            result.status = HttpCode.FORBIDDEN;
        }

        return of(result);
    }

    private callErrorHandler(response: HttpErrorResponse): boolean {
        const code = response.status;

        return code === HttpCode.FORBIDDEN
            ? this.isLicenseError(response)
                ? this.handleLicense()
                : this.handleForbidden()
            : (this.errorHandlers?.[code as HttpCode](response) ?? false);
    }

    private contentletIsForbidden(error: string): boolean {
        return (
            error?.indexOf('does not have permissions READ') > -1 ||
            error?.indexOf('User cannot edit') > -1
        );
    }

    private isLicenseError(response: HttpErrorResponse): boolean {
        return (
            response.headers &&
            response.headers.get('error-key') === 'dotcms.api.error.license.required'
        );
    }

    private handleForbidden(): boolean {
        const header = this.dotMessageService.get('dot.common.http.error.403.header');
        const message = this.dotMessageService.get('dot.common.http.error.403.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private handleLicense(): boolean {
        const header = this.dotMessageService.get('dot.common.http.error.403.license.header');
        const message = this.dotMessageService.get('dot.common.http.error.403.license.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private handleNotFound(): boolean {
        const header = this.dotMessageService.get('dot.common.http.error.404.header');
        const message = this.dotMessageService.get('dot.common.http.error.404.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private handleServerError(response?: HttpErrorResponse): boolean {
        const header = this.dotMessageService.get('dot.common.http.error.500.header');
        const message =
            this.getErrorMessage(response) ||
            this.dotMessageService.get('dot.common.http.error.500.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private handleBadRequestError(response?: HttpErrorResponse): boolean {
        const { error } = response ?? { error: undefined };

        const header = error?.header
            ? this.dotMessageService.get(error.header)
            : this.dotMessageService.get('dot.common.http.error.400.header');

        const message =
            this.getErrorMessage(response) ||
            this.dotMessageService.get('dot.common.http.error.400.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private handleNotContentError(response?: HttpErrorResponse): boolean {
        const header = this.dotMessageService.get('dot.common.http.error.204.header');
        const message =
            this.getErrorMessage(response) ||
            this.dotMessageService.get('dot.common.http.error.204.message');

        this.showErrorMessage(message, header);

        return false;
    }

    private showErrorMessage(message: string, header: string): void {
        if (this._unobtrusive) {
            this.dotMessageDisplayService.push({
                life: 3000,
                message,
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
        } else {
            this.dotDialogService.alert({
                message,
                header
            });
        }
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

    /**
     * Extracts a readable error message from an HttpErrorResponse
     *
     * @param response The HttpErrorResponse to extract the message from
     * @returns A string containing the error message or empty string if no message found
     */
    private getErrorMessage(response?: HttpErrorResponse): string {
        if (!response) {
            return '';
        }

        const { error } = response;
        let errorMessage = '';

        // Handle array of errors
        if (Array.isArray(error) && error.length > 0) {
            errorMessage = this.extractMessageFromErrorObject(error[0]);
        }
        // Handle error object with nested errors array
        else if (error?.errors && Array.isArray(error.errors) && error.errors.length > 0) {
            errorMessage = this.extractMessageFromErrorObject(error.errors[0]);
        }
        // Handle direct error object
        else if (error && typeof error === 'object') {
            errorMessage = this.extractMessageFromErrorObject(error);
        }
        // Handle string error
        else if (error && typeof error === 'string') {
            errorMessage = error;
        }

        // Try to get localized message if it's a message key
        const localizedMessage = this.dotMessageService.get(errorMessage);

        return localizedMessage !== errorMessage ? localizedMessage : errorMessage;
    }

    /**
     * Extracts message from an error object and trims it if it contains a colon
     *
     * @param errorObj The error object to extract message from
     * @returns The extracted message or empty string
     */
    private extractMessageFromErrorObject(errorObj: unknown): string {
        if (!errorObj) {
            return '';
        }

        // Handle string directly
        if (typeof errorObj === 'string') {
            return this.formatErrorMessage(errorObj);
        }

        // Handle error object
        if (typeof errorObj === 'object' && errorObj !== null) {
            const errorRecord = errorObj as Record<string, unknown>;

            // Try to extract message from common error properties in priority order
            const message =
                this.getStringProperty(errorRecord, 'message') ||
                this.getStringProperty(errorRecord, 'error') ||
                this.getStringProperty(errorRecord, 'detail') ||
                this.getStringProperty(errorRecord, 'description') ||
                '';

            return this.formatErrorMessage(message);
        }

        return '';
    }

    /**
     * Safely extracts a string property from an object
     *
     * @param obj The object to extract from
     * @param prop The property name to extract
     * @returns The string value or empty string
     */
    private getStringProperty(obj: Record<string, unknown>, prop: string): string {
        const value = obj[prop];

        return typeof value === 'string' ? value : '';
    }

    /**
     * Formats an error message by trimming at first colon if present
     *
     * @param message The message to format
     * @returns The formatted message
     */
    private formatErrorMessage(message: string): string {
        return message.includes(':') ? message.substring(0, message.indexOf(':')) : message;
    }
}
